package com.example.practica4_juegopara2jugadores.ui.screens

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.practica4_juegopara2jugadores.data.bluetooth.BluetoothGameService
import com.example.practica4_juegopara2jugadores.data.bluetooth.BluetoothResult
import com.example.practica4_juegopara2jugadores.data.bluetooth.ConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothSetupScreen(
    bluetoothService: BluetoothGameService,
    onConnectionEstablished: (isHost: Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados
    var selectedTab by remember { mutableStateOf(0) }
    var hostName by remember { mutableStateOf("") }
    var isDeviceDiscoverable by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    // Observar estados del servicio
    val connectionState by bluetoothService.connectionState.collectAsState()
    val discoveredDevices by bluetoothService.discoveredDevices.collectAsState()

    // Launcher para solicitar permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            scope.launch {
                snackbarHostState.showSnackbar("Permisos otorgados")
            }
        } else {
            showPermissionRationale = true
        }
    }

    // Launcher para hacer el dispositivo descubrible
    val discoverableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isDeviceDiscoverable = result.resultCode == Activity.RESULT_OK
        if (isDeviceDiscoverable) {
            // Iniciar servidor
            scope.launch {
                when (val serverResult = bluetoothService.startServer()) {
                    is BluetoothResult.Success -> {
                        snackbarHostState.showSnackbar("Esperando conexión...")
                    }
                    is BluetoothResult.Error -> {
                        snackbarHostState.showSnackbar(
                            "Error al iniciar servidor: ${serverResult.message}"
                        )
                    }
                }
            }
        }
    }

    // Launcher para habilitar Bluetooth
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Bluetooth habilitado, verificar permisos
            if (!bluetoothService.hasRequiredPermissions()) {
                permissionLauncher.launch(bluetoothService.getRequiredPermissions())
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Bluetooth es necesario para jugar en red")
            }
        }
    }

    // Verificar Bluetooth al inicio
    LaunchedEffect(Unit) {
        // PRIMERO verificar permisos
        if (!bluetoothService.hasRequiredPermissions()) {
            permissionLauncher.launch(bluetoothService.getRequiredPermissions())
        }
        // LUEGO verificar si Bluetooth está habilitado
        else if (!bluetoothService.isBluetoothEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }

    // Navegar automáticamente cuando se establece conexión
    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Connected) {
            delay(500)
            onConnectionEstablished((connectionState as ConnectionState.Connected).isHost)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Configurar Bluetooth", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        bluetoothService.disconnect()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // TabRow
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Crear Partida",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    },
                    icon = {
                        Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Unirse",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    },
                    icon = {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                )
            }

            // Contenido según tab seleccionado
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() + slideInHorizontally() togetherWith
                            fadeOut() + slideOutHorizontally()
                },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    0 -> CreateGameTab(
                        bluetoothService = bluetoothService,
                        hostName = hostName,
                        onHostNameChange = { hostName = it },
                        connectionState = connectionState,
                        onMakeDiscoverable = {
                            if (!bluetoothService.hasRequiredPermissions()) {
                                permissionLauncher.launch(bluetoothService.getRequiredPermissions())
                            } else {
                                val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                                }
                                discoverableLauncher.launch(discoverableIntent)
                            }
                        },
                        snackbarHostState = snackbarHostState,
                        onRequestPermissions = {
                            permissionLauncher.launch(bluetoothService.getRequiredPermissions())
                        }
                    )
                    1 -> JoinGameTab(
                        bluetoothService = bluetoothService,
                        discoveredDevices = discoveredDevices,
                        connectionState = connectionState,
                        onRequestPermissions = {
                            permissionLauncher.launch(bluetoothService.getRequiredPermissions())
                        },
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }

        // Diálogo de rationale de permisos
        if (showPermissionRationale) {
            PermissionRationaleDialog(
                onDismiss = { showPermissionRationale = false },
                onRequestPermissions = {
                    showPermissionRationale = false
                    permissionLauncher.launch(bluetoothService.getRequiredPermissions())
                }
            )
        }
    }
}

@Composable
private fun CreateGameTab(
    bluetoothService: BluetoothGameService,
    hostName: String,
    onHostNameChange: (String) -> Unit,
    connectionState: ConnectionState,
    onMakeDiscoverable: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onRequestPermissions: () -> Unit // NUEVO parámetro
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        Color.White
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icono decorativo
            AnimatedVisibility(
                visible = connectionState !is ConnectionState.Connected,
                enter = fadeIn() + expandVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "📱",
                        fontSize = 56.sp
                    )
                    Text(
                        text = "Crear Nueva Partida",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Campo de nombre del host
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Tu Nombre",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = hostName,
                        onValueChange = onHostNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ingresa tu nombre", fontSize = 14.sp) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }

            // Estado de conexión
            when (connectionState) {
                is ConnectionState.Disconnected -> {
                    // Instrucciones paso a paso (NUEVO)
                    InstructionsCard(
                        title = "📋 Instrucciones",
                        steps = listOf(
                            "1️⃣ Ingresa tu nombre arriba",
                            "2️⃣ Presiona 'Hacer Visible'",
                            "3️⃣ Espera en esta pantalla",
                            "4️⃣ El otro jugador debe buscarte y conectarse"
                        ),
                        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )

                    // Botón para hacer visible
                    Button(
                        onClick = {
                            if (!bluetoothService.hasRequiredPermissions()) {
                                onRequestPermissions()
                            } else {
                                // PRIMERO iniciar el servidor
                                scope.launch {
                                    when (val result = bluetoothService.startServer()) {
                                        is BluetoothResult.Success -> {
                                            snackbarHostState.showSnackbar("Servidor iniciado. Haciendo dispositivo visible...")
                                            // LUEGO hacer el dispositivo visible
                                            onMakeDiscoverable()
                                        }
                                        is BluetoothResult.Error -> {
                                            snackbarHostState.showSnackbar(
                                                "Error al iniciar servidor: ${result.message}"
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = hostName.isNotBlank() && connectionState !is ConnectionState.Connecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Hacer Visible",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Tu dispositivo será visible durante 5 minutos",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                is ConnectionState.Connecting -> {
                    WaitingForConnectionCard()
                }

                is ConnectionState.Connected -> {
                    ConnectionEstablishedCard(
                        deviceName = connectionState.deviceName
                    )
                }

                is ConnectionState.Error -> {
                    ErrorCard(message = connectionState.message)
                }

                else -> {}
            }

            Spacer(modifier = Modifier.weight(1f))

            // Información adicional
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "⚠️ NO SALGAS de esta pantalla hasta que el otro jugador se conecte",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun JoinGameTab(
    bluetoothService: BluetoothGameService,
    discoveredDevices: List<BluetoothDevice>,
    connectionState: ConnectionState,
    onRequestPermissions: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        Color.White
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Instrucciones importantes (NUEVO)
            InstructionsCard(
                title = "⚠️ Antes de buscar",
                steps = listOf(
                    "✅ El otro jugador debe estar en 'Crear Partida'",
                    "✅ Debe haber presionado 'Hacer Visible'",
                    "✅ Debe estar esperando en esa pantalla",
                    "❌ Si no está esperando, la conexión FALLARÁ"
                ),
                backgroundColor = Color(0xFFFFECB3)
            )

            // Botón de búsqueda
            Button(
                onClick = {
                    scope.launch {
                        if (!bluetoothService.hasRequiredPermissions()) {
                            onRequestPermissions()
                            return@launch
                        }

                        isScanning = true
                        when (val result = bluetoothService.startScanning()) {
                            is BluetoothResult.Success -> {
                                snackbarHostState.showSnackbar("Buscando dispositivos...")
                            }
                            is BluetoothResult.Error -> {
                                snackbarHostState.showSnackbar(result.message)
                                isScanning = false
                            }
                        }

                        delay(12000)
                        bluetoothService.stopScanning()
                        isScanning = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = connectionState !is ConnectionState.Connected && !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buscando...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buscar Dispositivos", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Estado de conexión
            when (connectionState) {
                is ConnectionState.Connecting -> {
                    ConnectingCard(deviceName = connectionState.deviceName)
                }
                is ConnectionState.Connected -> {
                    ConnectionEstablishedCard(deviceName = connectionState.deviceName)
                }
                is ConnectionState.Error -> {
                    ErrorCard(message = connectionState.message)
                }
                else -> {}
            }

            // Lista de dispositivos
            if (connectionState !is ConnectionState.Connected) {
                DevicesList(
                    devices = discoveredDevices,
                    bluetoothService = bluetoothService,
                    isScanning = isScanning,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// NUEVO: Card de instrucciones
@Composable
private fun InstructionsCard(
    title: String,
    steps: List<String>,
    backgroundColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )

            steps.forEach { step ->
                Text(
                    text = step,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun DevicesList(
    devices: List<BluetoothDevice>,
    bluetoothService: BluetoothGameService,
    isScanning: Boolean,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Dispositivos Encontrados",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Divider()

            if (devices.isEmpty() && !isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = Color.Gray
                        )
                        Text(
                            "No se encontraron dispositivos",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        Text(
                            "Presiona 'Buscar Dispositivos' para comenzar",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(devices, key = { it.address }) { device ->
                        DeviceItem(
                            device = device,
                            onClick = {
                                scope.launch {
                                    bluetoothService.stopScanning()
                                    when (val result = bluetoothService.connectToDevice(device)) {
                                        is BluetoothResult.Success -> {
                                            snackbarHostState.showSnackbar("Conectando...")
                                        }
                                        is BluetoothResult.Error -> {
                                            snackbarHostState.showSnackbar(
                                                "Error: ${result.message}"
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(
    device: BluetoothDevice,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = device.name ?: "Dispositivo desconocido",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = device.address,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

@Composable
private fun WaitingForConnectionCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "waiting")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(44.dp)
                    .scale(alpha),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )

            Text(
                text = "⏳ Esperando conexión...",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Otro jugador debe buscarte y conectarse.\n\n⚠️ NO salgas de esta pantalla",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun ConnectingCard(deviceName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Conectando...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = deviceName,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun ConnectionEstablishedCard(deviceName: String) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "¡Conectado!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "Con $deviceName",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF5252).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF5252)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Error",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFFF5252)
                )
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun PermissionRationaleDialog(
    onDismiss: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Permisos de Bluetooth",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Esta aplicación necesita permisos de Bluetooth para:\n\n" +
                        "• Buscar dispositivos cercanos\n" +
                        "• Conectarse con otros jugadores\n" +
                        "• Enviar y recibir movimientos del juego\n\n" +
                        "Sin estos permisos, no podrás jugar en modo multijugador Bluetooth."
            )
        },
        confirmButton = {
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Otorgar Permisos")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}