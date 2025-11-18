package com.example.practica4_juegopara2jugadores.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * Estados de conexión Bluetooth
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Scanning : ConnectionState()
    data class Connecting(val deviceName: String) : ConnectionState()
    data class Connected(val deviceName: String, val isHost: Boolean) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * Mensajes del juego que se envían por Bluetooth
 */
@Serializable
sealed class GameMessage {
    @Serializable
    data class Move(val column: Int, val player: String) : GameMessage()

    @Serializable
    data class ResetGame(val requestedBy: String) : GameMessage()

    @Serializable
    data class Chat(val sender: String, val message: String) : GameMessage()

    @Serializable
    data class GameStateSync(
        val currentPlayer: String,
        val redWins: Int,
        val yellowWins: Int
    ) : GameMessage()

    @Serializable
    object Disconnect : GameMessage()
}

/**
 * Resultado de operaciones Bluetooth
 */
sealed class BluetoothResult<out T> {
    data class Success<T>(val data: T) : BluetoothResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : BluetoothResult<Nothing>()
}

/**
 * Servicio de conectividad Bluetooth para el juego Connect Four
 */
class BluetoothGameService(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothGameService"
        private const val SERVICE_NAME = "ConnectFourGame"
        // UUID estándar para SPP (Serial Port Profile)
        private val SERVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val BUFFER_SIZE = 1024
        private const val CONNECTION_TIMEOUT = 10000L // 10 segundos
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // StateFlows para el estado
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _receivedMessages = MutableSharedFlow<GameMessage>(replay = 0)
    val receivedMessages: SharedFlow<GameMessage> = _receivedMessages.asSharedFlow()

    // Sockets y streams
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    // Jobs para manejo de coroutines
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scanningJob: Job? = null
    private var serverJob: Job? = null
    private var receiveJob: Job? = null
    private var connectionTimeoutJob: Job? = null

    // BroadcastReceiver para descubrimiento de dispositivos
    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        if (!_discoveredDevices.value.contains(it)) {
                            _discoveredDevices.value = _discoveredDevices.value + it
                            Log.d(TAG, "Device discovered: ${it.name ?: "Unknown"} - ${it.address}")
                        }
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d(TAG, "Discovery started")
                    _connectionState.value = ConnectionState.Scanning
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "Discovery finished. Found ${_discoveredDevices.value.size} devices")
                    if (_connectionState.value is ConnectionState.Scanning) {
                        _connectionState.value = ConnectionState.Disconnected
                    }
                }
            }
        }
    }

    private var isReceiverRegistered = false

    init {
        // Verificar que Bluetooth esté disponible
        if (bluetoothAdapter == null) {
            _connectionState.value = ConnectionState.Error("Bluetooth no disponible en este dispositivo")
        }
    }

    /**
     * Verifica si el dispositivo tiene Bluetooth habilitado
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Verifica permisos de Bluetooth según la versión de Android
     */
    fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_ADMIN
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Obtiene la lista de permisos requeridos según la versión de Android
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    /**
     * Obtiene dispositivos ya emparejados
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): BluetoothResult<List<BluetoothDevice>> {
        if (!hasRequiredPermissions()) {
            return BluetoothResult.Error("Permisos de Bluetooth no otorgados")
        }

        return try {
            val pairedDevices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
            BluetoothResult.Success(pairedDevices)
        } catch (e: SecurityException) {
            BluetoothResult.Error("Error de permisos: ${e.message}", e)
        } catch (e: Exception) {
            BluetoothResult.Error("Error al obtener dispositivos emparejados: ${e.message}", e)
        }
    }

    /**
     * Inicia el escaneo de dispositivos Bluetooth
     */
    @SuppressLint("MissingPermission")
    suspend fun startScanning(): BluetoothResult<Unit> = withContext(Dispatchers.IO) {
        if (!hasRequiredPermissions()) {
            return@withContext BluetoothResult.Error("Permisos de Bluetooth no otorgados")
        }

        if (!isBluetoothEnabled()) {
            return@withContext BluetoothResult.Error("Bluetooth no está habilitado")
        }

        try {
            // Limpiar lista de dispositivos descubiertos
            _discoveredDevices.value = emptyList()

            // Registrar el receiver si no está registrado
            if (!isReceiverRegistered) {
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
                context.registerReceiver(discoveryReceiver, filter)
                isReceiverRegistered = true
            }

            // Cancelar escaneo anterior si existe
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }

            // Iniciar nuevo escaneo
            val started = bluetoothAdapter?.startDiscovery() ?: false
            if (started) {
                Log.d(TAG, "Scanning started successfully")
                BluetoothResult.Success(Unit)
            } else {
                BluetoothResult.Error("No se pudo iniciar el escaneo")
            }
        } catch (e: SecurityException) {
            BluetoothResult.Error("Error de permisos: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scan", e)
            BluetoothResult.Error("Error al iniciar escaneo: ${e.message}", e)
        }
    }

    /**
     * Detiene el escaneo de dispositivos
     */
    @SuppressLint("MissingPermission")
    suspend fun stopScanning(): BluetoothResult<Unit> = withContext(Dispatchers.IO) {
        if (!hasRequiredPermissions()) {
            return@withContext BluetoothResult.Error("Permisos de Bluetooth no otorgados")
        }

        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }

            if (isReceiverRegistered) {
                try {
                    context.unregisterReceiver(discoveryReceiver)
                    isReceiverRegistered = false
                } catch (e: IllegalArgumentException) {
                    // Receiver ya estaba desregistrado
                }
            }

            if (_connectionState.value is ConnectionState.Scanning) {
                _connectionState.value = ConnectionState.Disconnected
            }

            Log.d(TAG, "Scanning stopped")
            BluetoothResult.Success(Unit)
        } catch (e: SecurityException) {
            BluetoothResult.Error("Error de permisos: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan", e)
            BluetoothResult.Error("Error al detener escaneo: ${e.message}", e)
        }
    }

    /**
     * Conecta a un dispositivo remoto como cliente
     * MEJORADO: Usa reflexión como fallback
     */
    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(device: BluetoothDevice): BluetoothResult<Unit> = withContext(Dispatchers.IO) {
        if (!hasRequiredPermissions()) {
            return@withContext BluetoothResult.Error("Permisos de Bluetooth no otorgados")
        }

        try {
            // Detener escaneo si está activo
            stopScanning()

            _connectionState.value = ConnectionState.Connecting(device.name ?: "Dispositivo desconocido")

            // Cerrar conexión anterior si existe
            closeConnection()

            Log.d(TAG, "Connecting to device: ${device.name} - ${device.address}")

            // Intentar conectar con método estándar primero
            var socket: BluetoothSocket? = null
            var connectionSuccessful = false

            // MÉTODO 1: Conexión estándar
            try {
                socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)

                // Timeout de conexión
                connectionTimeoutJob = serviceScope.launch {
                    delay(CONNECTION_TIMEOUT)
                    if (!connectionSuccessful) {
                        Log.w(TAG, "Connection timeout, cancelling")
                        socket?.close()
                    }
                }

                socket.connect()
                connectionSuccessful = true
                connectionTimeoutJob?.cancel()

                Log.d(TAG, "Standard connection successful")
            } catch (e: IOException) {
                Log.w(TAG, "Standard connection failed, trying fallback method", e)
                socket?.close()
                socket = null
                connectionSuccessful = false
                connectionTimeoutJob?.cancel()

                // MÉTODO 2: Conexión con reflexión (fallback para algunos dispositivos)
                try {
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    socket = method.invoke(device, 1) as BluetoothSocket

                    connectionTimeoutJob = serviceScope.launch {
                        delay(CONNECTION_TIMEOUT)
                        if (!connectionSuccessful) {
                            socket?.close()
                        }
                    }

                    socket.connect()
                    connectionSuccessful = true
                    connectionTimeoutJob?.cancel()

                    Log.d(TAG, "Fallback connection successful")
                } catch (e2: Exception) {
                    Log.e(TAG, "Fallback connection also failed", e2)
                    socket?.close()
                    throw IOException("No se pudo conectar con ningún método", e2)
                }
            }

            if (socket == null || !connectionSuccessful) {
                throw IOException("No se pudo establecer conexión")
            }

            clientSocket = socket
            inputStream = clientSocket?.inputStream
            outputStream = clientSocket?.outputStream

            _connectionState.value = ConnectionState.Connected(
                deviceName = device.name ?: "Dispositivo desconocido",
                isHost = false
            )

            // Iniciar recepción de mensajes
            startReceivingMessages()

            Log.d(TAG, "Connected successfully as client")
            BluetoothResult.Success(Unit)

        } catch (e: IOException) {
            Log.e(TAG, "Connection failed", e)
            val errorMsg = when {
                e.message?.contains("timeout") == true -> "Tiempo de espera agotado. Asegúrate de que el otro dispositivo esté esperando conexiones."
                e.message?.contains("closed") == true -> "El dispositivo remoto rechazó la conexión. Asegúrate de que el servidor esté activo."
                else -> "Error de conexión: ${e.message}"
            }
            _connectionState.value = ConnectionState.Error(errorMsg)
            closeConnection()
            BluetoothResult.Error(errorMsg, e)
        } catch (e: SecurityException) {
            _connectionState.value = ConnectionState.Error("Permisos denegados")
            BluetoothResult.Error("Error de permisos: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during connection", e)
            _connectionState.value = ConnectionState.Error("Error inesperado")
            closeConnection()
            BluetoothResult.Error("Error inesperado: ${e.message}", e)
        }
    }

    /**
     * Inicia el servidor para escuchar conexiones entrantes
     */
    @SuppressLint("MissingPermission")
    suspend fun startServer(): BluetoothResult<Unit> = withContext(Dispatchers.IO) {
        if (!hasRequiredPermissions()) {
            return@withContext BluetoothResult.Error("Permisos de Bluetooth no otorgados")
        }

        if (!isBluetoothEnabled()) {
            return@withContext BluetoothResult.Error("Bluetooth no está habilitado")
        }

        try {
            // Cerrar servidor anterior si existe
            closeConnection()

            Log.d(TAG, "Starting server...")

            // Crear server socket
            serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                SERVICE_NAME,
                SERVICE_UUID
            )

            _connectionState.value = ConnectionState.Connecting("Esperando conexión...")

            // Iniciar job para aceptar conexiones
            serverJob = serviceScope.launch {
                try {
                    Log.d(TAG, "Server waiting for connection...")

                    // accept() es bloqueante hasta que se conecte un cliente
                    clientSocket = serverSocket?.accept()

                    Log.d(TAG, "Client connected!")

                    // Configurar streams
                    inputStream = clientSocket?.inputStream
                    outputStream = clientSocket?.outputStream

                    val deviceName = clientSocket?.remoteDevice?.name ?: "Dispositivo desconocido"
                    _connectionState.value = ConnectionState.Connected(
                        deviceName = deviceName,
                        isHost = true
                    )

                    // Cerrar server socket ya que solo aceptamos una conexión
                    serverSocket?.close()
                    serverSocket = null

                    // Iniciar recepción de mensajes
                    startReceivingMessages()

                } catch (e: IOException) {
                    if (isActive) {
                        Log.e(TAG, "Server accept failed", e)
                        _connectionState.value = ConnectionState.Error("Error al aceptar conexión")
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Unexpected server error", e)
                        _connectionState.value = ConnectionState.Error("Error del servidor")
                    }
                }
            }

            BluetoothResult.Success(Unit)

        } catch (e: IOException) {
            Log.e(TAG, "Failed to start server", e)
            _connectionState.value = ConnectionState.Error("Error al iniciar servidor")
            BluetoothResult.Error("Error al iniciar servidor: ${e.message}", e)
        } catch (e: SecurityException) {
            _connectionState.value = ConnectionState.Error("Permisos denegados")
            BluetoothResult.Error("Error de permisos: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error starting server", e)
            _connectionState.value = ConnectionState.Error("Error inesperado")
            BluetoothResult.Error("Error inesperado: ${e.message}", e)
        }
    }

    /**
     * Envía un mensaje a través de la conexión Bluetooth
     */
    suspend fun sendMessage(message: GameMessage): BluetoothResult<Unit> = withContext(Dispatchers.IO) {
        if (_connectionState.value !is ConnectionState.Connected) {
            return@withContext BluetoothResult.Error("No hay conexión activa")
        }

        try {
            val jsonMessage = json.encodeToString(message)
            val messageBytes = jsonMessage.toByteArray(Charsets.UTF_8)

            // Enviar el tamaño del mensaje primero (4 bytes)
            val sizeBytes = ByteArray(4)
            sizeBytes[0] = (messageBytes.size shr 24).toByte()
            sizeBytes[1] = (messageBytes.size shr 16).toByte()
            sizeBytes[2] = (messageBytes.size shr 8).toByte()
            sizeBytes[3] = messageBytes.size.toByte()

            outputStream?.write(sizeBytes)
            outputStream?.write(messageBytes)
            outputStream?.flush()

            Log.d(TAG, "Message sent: ${message::class.simpleName}")
            BluetoothResult.Success(Unit)

        } catch (e: IOException) {
            Log.e(TAG, "Failed to send message", e)
            _connectionState.value = ConnectionState.Error("Error al enviar mensaje")
            disconnect()
            BluetoothResult.Error("Error al enviar mensaje: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error sending message", e)
            BluetoothResult.Error("Error inesperado: ${e.message}", e)
        }
    }

    /**
     * Inicia el proceso de recepción de mensajes
     */
    private fun startReceivingMessages() {
        receiveJob = serviceScope.launch {
            try {
                val buffer = ByteArray(BUFFER_SIZE)
                val sizeBuffer = ByteArray(4)

                while (isActive && clientSocket?.isConnected == true) {
                    try {
                        // Leer el tamaño del mensaje (4 bytes)
                        var bytesRead = inputStream?.read(sizeBuffer) ?: -1
                        if (bytesRead != 4) break

                        val messageSize = ((sizeBuffer[0].toInt() and 0xFF) shl 24) or
                                ((sizeBuffer[1].toInt() and 0xFF) shl 16) or
                                ((sizeBuffer[2].toInt() and 0xFF) shl 8) or
                                (sizeBuffer[3].toInt() and 0xFF)

                        if (messageSize <= 0 || messageSize > BUFFER_SIZE * 10) {
                            Log.w(TAG, "Invalid message size: $messageSize")
                            break
                        }

                        // Leer el mensaje completo
                        val messageBytes = ByteArray(messageSize)
                        var totalBytesRead = 0

                        while (totalBytesRead < messageSize) {
                            bytesRead = inputStream?.read(
                                messageBytes,
                                totalBytesRead,
                                messageSize - totalBytesRead
                            ) ?: -1

                            if (bytesRead == -1) break
                            totalBytesRead += bytesRead
                        }

                        if (totalBytesRead != messageSize) break

                        // Deserializar y emitir mensaje
                        val jsonMessage = String(messageBytes, Charsets.UTF_8)
                        val message = json.decodeFromString<GameMessage>(jsonMessage)

                        Log.d(TAG, "Message received: ${message::class.simpleName}")
                        _receivedMessages.emit(message)

                        // Si es un mensaje de desconexión, cerrar conexión
                        if (message is GameMessage.Disconnect) {
                            disconnect()
                            break
                        }

                    } catch (e: IOException) {
                        if (isActive) {
                            Log.e(TAG, "Error reading message", e)
                            break
                        }
                    }
                }

                // Si salimos del loop, desconectar
                if (isActive) {
                    _connectionState.value = ConnectionState.Error("Conexión perdida")
                    disconnect()
                }

            } catch (e: Exception) {
                if (isActive) {
                    Log.e(TAG, "Error in receive loop", e)
                    _connectionState.value = ConnectionState.Error("Error de recepción")
                    disconnect()
                }
            }
        }
    }

    /**
     * Desconecta la conexión actual
     */
    fun disconnect() {
        serviceScope.launch {
            try {
                // Enviar mensaje de desconexión si hay conexión activa
                if (_connectionState.value is ConnectionState.Connected) {
                    try {
                        sendMessage(GameMessage.Disconnect)
                        delay(100) // Dar tiempo para que se envíe
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending disconnect message", e)
                    }
                }

                closeConnection()
                _connectionState.value = ConnectionState.Disconnected
                Log.d(TAG, "Disconnected")

            } catch (e: Exception) {
                Log.e(TAG, "Error during disconnect", e)
            }
        }
    }

    /**
     * Cierra los sockets y streams
     */
    private fun closeConnection() {
        try {
            connectionTimeoutJob?.cancel()
            receiveJob?.cancel()
            serverJob?.cancel()

            inputStream?.close()
            outputStream?.close()
            clientSocket?.close()
            serverSocket?.close()

            inputStream = null
            outputStream = null
            clientSocket = null
            serverSocket = null

        } catch (e: IOException) {
            Log.e(TAG, "Error closing connection", e)
        }
    }

    /**
     * Limpia recursos al destruir el servicio
     */
    fun cleanup() {
        serviceScope.launch {
            stopScanning()
            disconnect()
            serviceScope.cancel()
        }
    }
}