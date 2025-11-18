package com.example.practica4_juegopara2jugadores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.practica4_juegopara2jugadores.data.bluetooth.BluetoothGameService
import com.example.practica4_juegopara2jugadores.data.bluetooth.BluetoothResult
import com.example.practica4_juegopara2jugadores.data.bluetooth.GameMessage
import com.example.practica4_juegopara2jugadores.domain.GameLogic
import com.example.practica4_juegopara2jugadores.model.GameMode
import com.example.practica4_juegopara2jugadores.model.GameState
import com.example.practica4_juegopara2jugadores.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar el juego por Bluetooth
 * Sincroniza movimientos entre dos dispositivos
 */
class BluetoothGameViewModel(
    private val bluetoothService: BluetoothGameService,
    private val isHost: Boolean
) : ViewModel() {

    // Estado del juego
    private val _gameState = MutableStateFlow(
        GameState(gameMode = GameMode.BLUETOOTH_MULTIPLAYER)
    )
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Jugador de este dispositivo
    private val _myPlayer = MutableStateFlow(if (isHost) Player.RED else Player.YELLOW)
    val myPlayer: StateFlow<Player> = _myPlayer.asStateFlow()

    // Control de turno
    private val _isMyTurn = MutableStateFlow(isHost) // El host (RED) siempre empieza
    val isMyTurn: StateFlow<Boolean> = _isMyTurn.asStateFlow()

    init {
        // Observar mensajes recibidos
        observeReceivedMessages()
    }

    /**
     * Observa los mensajes recibidos del otro dispositivo
     */
    private fun observeReceivedMessages() {
        viewModelScope.launch {
            bluetoothService.receivedMessages.collect { message ->
                when (message) {
                    is GameMessage.Move -> {
                        handleRemoteMove(message)
                    }
                    is GameMessage.ResetGame -> {
                        handleRemoteReset()
                    }
                    is GameMessage.GameStateSync -> {
                        handleStateSync(message)
                    }
                    is GameMessage.Disconnect -> {
                        // El servicio ya maneja la desconexión
                    }
                    else -> {
                        // Otros tipos de mensaje (Chat, etc.) se pueden manejar aquí
                    }
                }
            }
        }
    }

    /**
     * Realiza un movimiento en el tablero
     */
    fun makeMove(column: Int) {
        // Verificar que sea nuestro turno
        if (!_isMyTurn.value) {
            return
        }

        val currentState = _gameState.value

        // Verificar que el juego no haya terminado
        if (currentState.isGameOver()) {
            return
        }

        // Intentar hacer el movimiento
        val newState = GameLogic.makeMove(currentState, column)

        if (newState != null) {
            // Actualizar el estado local
            _gameState.value = newState

            // Enviar el movimiento al otro dispositivo
            viewModelScope.launch {
                val message = GameMessage.Move(
                    column = column,
                    player = _myPlayer.value.name
                )

                when (val result = bluetoothService.sendMessage(message)) {
                    is BluetoothResult.Success -> {
                        // Movimiento enviado exitosamente
                        // Cambiar de turno solo si el juego no ha terminado
                        if (!newState.isGameOver()) {
                            _isMyTurn.value = false
                        }
                    }
                    is BluetoothResult.Error -> {
                        // Error al enviar, revertir el movimiento
                        _gameState.value = currentState
                        // Aquí podrías mostrar un mensaje de error al usuario
                    }
                }
            }
        }
    }

    /**
     * Maneja un movimiento recibido del oponente
     */
    private fun handleRemoteMove(message: GameMessage.Move) {
        val currentState = _gameState.value

        // Verificar que no sea nuestro turno
        if (_isMyTurn.value) {
            return
        }

        // Verificar que el juego no haya terminado
        if (currentState.isGameOver()) {
            return
        }

        // Realizar el movimiento del oponente
        val newState = GameLogic.makeMove(currentState, message.column)

        if (newState != null) {
            _gameState.value = newState

            // Cambiar de turno solo si el juego no ha terminado
            if (!newState.isGameOver()) {
                _isMyTurn.value = true
            }
        }
    }

    /**
     * Solicita reiniciar el juego
     */
    fun requestResetGame() {
        viewModelScope.launch {
            // Enviar solicitud de reset
            val message = GameMessage.ResetGame(
                requestedBy = _myPlayer.value.name
            )

            when (bluetoothService.sendMessage(message)) {
                is BluetoothResult.Success -> {
                    // Reset exitoso, actualizar estado local
                    resetGameLocal()
                }
                is BluetoothResult.Error -> {
                    // Error al enviar reset
                }
            }
        }
    }

    /**
     * Maneja la solicitud de reset recibida del oponente
     */
    private fun handleRemoteReset() {
        resetGameLocal()
    }

    /**
     * Reinicia el juego localmente
     */
    private fun resetGameLocal() {
        val currentState = _gameState.value
        val newState = GameLogic.resetGame(currentState).copy(
            gameMode = GameMode.BLUETOOTH_MULTIPLAYER
        )
        _gameState.value = newState

        // El host siempre empieza
        _isMyTurn.value = isHost
    }

    /**
     * Sincroniza el estado del juego
     */
    private fun handleStateSync(message: GameMessage.GameStateSync) {
        // Actualizar victorias sincronizadas
        _gameState.value = _gameState.value.copy(
            redWins = message.redWins,
            yellowWins = message.yellowWins
        )
    }

    /**
     * Envía una sincronización del estado del juego
     */
    fun syncGameState() {
        viewModelScope.launch {
            val currentState = _gameState.value
            val message = GameMessage.GameStateSync(
                currentPlayer = currentState.currentPlayer.name,
                redWins = currentState.redWins,
                yellowWins = currentState.yellowWins
            )

            bluetoothService.sendMessage(message)
        }
    }

    /**
     * Factory para crear el ViewModel con parámetros
     */
    class Factory(
        private val bluetoothService: BluetoothGameService,
        private val isHost: Boolean
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BluetoothGameViewModel::class.java)) {
                return BluetoothGameViewModel(bluetoothService, isHost) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    override fun onCleared() {
        super.onCleared()
        // El servicio se limpia desde la Activity/Screen principal
    }
}