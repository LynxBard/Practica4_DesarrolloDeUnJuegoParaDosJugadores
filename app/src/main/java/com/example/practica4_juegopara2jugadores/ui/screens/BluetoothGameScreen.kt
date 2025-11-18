package com.example.practica4_juegopara2jugadores.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.practica4_juegopara2jugadores.data.bluetooth.BluetoothGameService
import com.example.practica4_juegopara2jugadores.data.bluetooth.ConnectionState
import com.example.practica4_juegopara2jugadores.data.bluetooth.GameMessage
import com.example.practica4_juegopara2jugadores.model.Cell
import com.example.practica4_juegopara2jugadores.model.GameMode
import com.example.practica4_juegopara2jugadores.model.GameState
import com.example.practica4_juegopara2jugadores.model.Player
import com.example.practica4_juegopara2jugadores.ui.*
import com.example.practica4_juegopara2jugadores.viewmodel.BluetoothGameViewModel
import kotlinx.coroutines.launch
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothGameScreen(
    bluetoothService: BluetoothGameService,
    isHost: Boolean,
    onBack: () -> Unit
) {
    val viewModel: BluetoothGameViewModel = viewModel(
        factory = BluetoothGameViewModel.Factory(bluetoothService, isHost)
    )

    val gameState by viewModel.gameState.collectAsState()
    val connectionState by bluetoothService.connectionState.collectAsState()
    val isMyTurn by viewModel.isMyTurn.collectAsState()
    val myPlayer by viewModel.myPlayer.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDisconnectDialog by remember { mutableStateOf(false) }

    // Observar errores de conexión
    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Error) {
            snackbarHostState.showSnackbar(
                message = "Error de conexión: ${(connectionState as ConnectionState.Error).message}",
                duration = SnackbarDuration.Long
            )
        } else if (connectionState is ConnectionState.Disconnected) {
            snackbarHostState.showSnackbar(
                message = "Conexión perdida",
                duration = SnackbarDuration.Short
            )
            onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Connect Four",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        if (connectionState is ConnectionState.Connected) {
                            BluetoothConnectionIndicator(
                                deviceName = (connectionState as ConnectionState.Connected).deviceName,
                                isConnected = true
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showDisconnectDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDisconnectDialog = true }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Desconectar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BoardBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        // Hacer scrollable para pantallas pequeñas
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Marcador e indicador de turno
            BluetoothScoreBoard(
                gameState = gameState,
                myPlayer = myPlayer,
                isMyTurn = isMyTurn
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tablero de juego (responsivo)
            BluetoothGameBoard(
                gameState = gameState,
                isMyTurn = isMyTurn,
                onColumnClick = { column ->
                    viewModel.makeMove(column)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de control
            BluetoothControlButtons(
                onResetGame = {
                    scope.launch {
                        viewModel.requestResetGame()
                    }
                },
                onDisconnect = { showDisconnectDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Diálogo de victoria/empate
            if (gameState.isGameOver()) {
                BluetoothWinnerDialog(
                    gameState = gameState,
                    myPlayer = myPlayer,
                    onNewGame = {
                        scope.launch {
                            viewModel.requestResetGame()
                        }
                    }
                )
            }
        }
    }

    // Diálogo de desconexión
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Desconectar",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("¿Estás seguro de que deseas desconectar? Se perderá la partida actual.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        bluetoothService.disconnect()
                        showDisconnectDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252)
                    )
                ) {
                    Text("Desconectar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun BluetoothConnectionIndicator(
    deviceName: String,
    isConnected: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "connection")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF5252),
                    shape = CircleShape
                )
                .graphicsLayer { this.alpha = if (isConnected) alpha else 1f }
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = if (isConnected) "Conectado" else "Desconectado",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun BluetoothScoreBoard(
    gameState: GameState,
    myPlayer: Player,
    isMyTurn: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BluetoothPlayerScore(
            player = myPlayer,
            score = if (myPlayer == Player.RED) gameState.redWins else gameState.yellowWins,
            isCurrentPlayer = isMyTurn && !gameState.isGameOver(),
            label = "Tú",
            isMe = true
        )

        BluetoothPlayerScore(
            player = myPlayer.other(),
            score = if (myPlayer.other() == Player.RED) gameState.redWins else gameState.yellowWins,
            isCurrentPlayer = !isMyTurn && !gameState.isGameOver(),
            label = "Oponente",
            isMe = false
        )
    }
}

@Composable
private fun BluetoothPlayerScore(
    player: Player,
    score: Int,
    isCurrentPlayer: Boolean,
    label: String,
    isMe: Boolean
) {
    val playerColor = if (player == Player.RED) RedPlayer else YellowPlayer
    val scale by animateFloatAsState(
        targetValue = if (isCurrentPlayer) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .width(150.dp)
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlayer) playerColor.copy(alpha = 0.2f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentPlayer) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(playerColor),
                contentAlignment = Alignment.Center
            ) {
                if (isMe) {
                    Text(text = "👤", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Victorias: $score",
                fontSize = 12.sp,
                color = Color.Gray
            )

            if (isCurrentPlayer) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isMe) "Tu turno" else "Esperando...",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = playerColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BluetoothGameBoard(
    gameState: GameState,
    isMyTurn: Boolean,
    onColumnClick: (Int) -> Unit
) {
    // Indicador de turno del oponente
    AnimatedVisibility(
        visible = !isMyTurn && !gameState.isGameOver(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = YellowPlayer.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = YellowPlayer
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Esperando movimiento...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )
            }
        }
    }

    // Obtener dimensiones de pantalla
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp

    // Calcular tamaño de celda
    val availableWidth = screenWidthDp - 32.dp - 16.dp
    val cellSize = min((availableWidth / GameState.COLUMNS).value, 52f).dp

    // Tablero
    Card(
        modifier = Modifier
            .wrapContentSize()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = BoardBlue)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (row in 0 until GameState.ROWS) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (col in 0 until GameState.COLUMNS) {
                        BluetoothCellView(
                            cell = gameState.board[row][col],
                            isWinningCell = gameState.winningCells.contains(Pair(row, col)),
                            isLastMove = gameState.lastMove == Pair(row, col),
                            isInteractive = isMyTurn && !gameState.isGameOver(),
                            cellSize = cellSize,
                            onClick = {
                                if (isMyTurn && !gameState.isGameOver()) {
                                    onColumnClick(col)
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
private fun BluetoothCellView(
    cell: Cell,
    isWinningCell: Boolean,
    isLastMove: Boolean,
    isInteractive: Boolean,
    cellSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = when {
            isWinningCell -> 1.15f
            isLastMove -> 1.08f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cell_scale"
    )

    val cellColor = when (cell) {
        is Cell.Empty -> EmptyCell
        is Cell.Occupied -> when (cell.player) {
            Player.RED -> RedPlayer
            Player.YELLOW -> YellowPlayer
        }
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .clickable(enabled = isInteractive, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .scale(scale)
                .clip(CircleShape)
                .background(cellColor)
        )

        if (isLastMove && cell is Cell.Occupied) {
            Box(
                modifier = Modifier
                    .size(cellSize * 0.3f)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

@Composable
private fun BluetoothControlButtons(
    onResetGame: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onResetGame,
            colors = ButtonDefaults.buttonColors(containerColor = BoardBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Nueva Partida", fontSize = 12.sp)
        }

        OutlinedButton(
            onClick = onDisconnect,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFFF5252)
            )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Desconectar", fontSize = 12.sp)
        }
    }
}

@Composable
private fun BluetoothWinnerDialog(
    gameState: GameState,
    myPlayer: Player,
    onNewGame: () -> Unit
) {
    val didIWin = gameState.winner == myPlayer

    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = when {
                    gameState.isDraw -> "¡Empate!"
                    didIWin -> "¡Ganaste!"
                    else -> "Has Perdido"
                },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when {
                        gameState.isDraw -> "🤝"
                        didIWin -> "🎉"
                        else -> "😔"
                    },
                    fontSize = 64.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!gameState.isDraw) {
                    val winnerColor = if (gameState.winner == Player.RED) RedPlayer else YellowPlayer
                    val winnerText = if (didIWin) "¡Felicidades!" else "El oponente ganó"

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(winnerColor)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = winnerText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "El tablero está lleno",
                        fontSize = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onNewGame,
                colors = ButtonDefaults.buttonColors(containerColor = BoardBlue)
            ) {
                Text("Nueva Partida")
            }
        }
    )
}