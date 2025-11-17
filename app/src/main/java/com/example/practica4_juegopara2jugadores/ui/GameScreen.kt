package com.example.practica4_juegopara2jugadores.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.practica4_juegopara2jugadores.data.GameSaveRepository
import com.example.practica4_juegopara2jugadores.model.Cell
import com.example.practica4_juegopara2jugadores.model.GameMode
import com.example.practica4_juegopara2jugadores.model.GameState
import com.example.practica4_juegopara2jugadores.model.Player
import com.example.practica4_juegopara2jugadores.ui.screens.SaveGameScreen
import com.example.practica4_juegopara2jugadores.viewmodel.GameViewModel
import kotlinx.coroutines.launch

// Colores del juego
val RedPlayer = Color(0xFFE53935)
val YellowPlayer = Color(0xFFFDD835)
val BoardBlue = Color(0xFF1976D2)
val BackgroundColor = Color(0xFFF5F5F5)
val EmptyCell = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel(),
    onBack: (() -> Unit)? = null,
    showAIIndicator: Boolean = false
) {
    val gameState by viewModel.gameState.collectAsState()
    val isAIThinking by viewModel.isAIThinking.collectAsState()
    val context = LocalContext.current

    // Estado para mostrar el diálogo de guardar
    var showSaveDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = {
                        Text(
                            "Connect Four",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    },
                    actions = {
                        // Botón para guardar partida
                        IconButton(
                            onClick = { showSaveDialog = true }
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = "Guardar Partida",
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header con título (solo si no hay TopAppBar)
            if (onBack == null) {
                Text(
                    text = "Connect Four",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = BoardBlue,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            // Indicador de turno y puntuación
            ScoreBoard(
                gameState = gameState,
                isAIThinking = isAIThinking && showAIIndicator
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tablero de juego
            GameBoard(
                gameState = gameState,
                onColumnClick = { column -> viewModel.makeMove(column) },
                isInteractionEnabled = !isAIThinking
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botones de control
            ControlButtons(
                onResetGame = { viewModel.resetGame() },
                onResetAll = { viewModel.resetAll() },
                onSaveGame = { showSaveDialog = true }
            )

            // Diálogo de victoria/empate
            if (gameState.isGameOver()) {
                WinnerDialog(
                    gameState = gameState,
                    onNewGame = { viewModel.resetGame() }
                )
            }
        }
    }

    // Diálogo para guardar partida
    if (showSaveDialog) {
        Dialog(
            onDismissRequest = { showSaveDialog = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                SaveGameScreen(
                    gameState = gameState,
                    repository = GameSaveRepository(context),
                    onDismiss = { showSaveDialog = false },
                    onSaveSuccess = { path ->
                        showSaveDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Partida guardada exitosamente",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ScoreBoard(
    gameState: GameState,
    isAIThinking: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Jugador Rojo
        PlayerScore(
            player = Player.RED,
            score = gameState.redWins,
            isCurrentPlayer = gameState.currentPlayer == Player.RED && !gameState.isGameOver(),
            label = if (gameState.gameMode == GameMode.SINGLE_PLAYER) "Tú" else "Rojo",
            isAIThinking = false
        )

        // Jugador Amarillo
        PlayerScore(
            player = Player.YELLOW,
            score = gameState.yellowWins,
            isCurrentPlayer = gameState.currentPlayer == Player.YELLOW && !gameState.isGameOver(),
            label = if (gameState.gameMode == GameMode.SINGLE_PLAYER) "IA" else "Amarillo",
            isAIThinking = isAIThinking
        )
    }
}

@Composable
fun PlayerScore(
    player: Player,
    score: Int,
    isCurrentPlayer: Boolean,
    label: String,
    isAIThinking: Boolean = false
) {
    val playerColor = if (player == Player.RED) RedPlayer else YellowPlayer
    val scale by animateFloatAsState(
        targetValue = if (isCurrentPlayer) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // Animación de rotación para "pensando"
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
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
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(playerColor),
                contentAlignment = Alignment.Center
            ) {
                if (isAIThinking) {
                    Text(
                        text = "🤔",
                        fontSize = 20.sp,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = rotation
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Victorias: $score",
                fontSize = 14.sp,
                color = Color.Gray
            )

            if (isCurrentPlayer) {
                Text(
                    text = if (isAIThinking) "Pensando..." else "Tu turno",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = playerColor
                )
            }
        }
    }
}

@Composable
fun GameBoard(
    gameState: GameState,
    onColumnClick: (Int) -> Unit,
    isInteractionEnabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = BoardBlue)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            for (row in 0 until GameState.ROWS) {
                Row {
                    for (col in 0 until GameState.COLUMNS) {
                        CellView(
                            cell = gameState.board[row][col],
                            isWinningCell = gameState.winningCells.contains(Pair(row, col)),
                            isLastMove = gameState.lastMove == Pair(row, col),
                            onClick = {
                                if (isInteractionEnabled && !gameState.isGameOver()) {
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
fun CellView(
    cell: Cell,
    isWinningCell: Boolean,
    isLastMove: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isWinningCell) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "winning_scale"
    )

    // Determinar el color de la celda
    val cellColor = when (cell) {
        is Cell.Empty -> EmptyCell
        is Cell.Occupied -> when (cell.player) {
            Player.RED -> RedPlayer
            Player.YELLOW -> YellowPlayer
        }
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .padding(4.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isWinningCell) Modifier.scale(scale) else Modifier
                )
                .clip(CircleShape)
                .background(cellColor)
        )
    }
}

@Composable
fun ControlButtons(
    onResetGame: () -> Unit,
    onResetAll: () -> Unit,
    onSaveGame: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onResetGame,
                colors = ButtonDefaults.buttonColors(containerColor = BoardBlue),
                modifier = Modifier.weight(1f)
            ) {
                Text("Nueva Partida")
            }

            OutlinedButton(
                onClick = onResetAll,
                modifier = Modifier.weight(1f)
            ) {
                Text("Reiniciar Todo")
            }
        }

        OutlinedButton(
            onClick = onSaveGame,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Guardar Partida")
        }
    }
}

@Composable
fun WinnerDialog(
    gameState: GameState,
    onNewGame: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = if (gameState.isDraw) "¡Empate!" else "¡Tenemos un Ganador!",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!gameState.isDraw) {
                    val winnerColor = if (gameState.winner == Player.RED) RedPlayer else YellowPlayer
                    val winnerName = when {
                        gameState.gameMode == GameMode.SINGLE_PLAYER && gameState.winner == Player.RED -> "¡Has ganado!"
                        gameState.gameMode == GameMode.SINGLE_PLAYER && gameState.winner == Player.YELLOW -> "La IA ganó"
                        gameState.winner == Player.RED -> "Rojo"
                        else -> "Amarillo"
                    }

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(winnerColor)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (gameState.gameMode == GameMode.SINGLE_PLAYER) {
                            winnerName
                        } else {
                            "¡Gana el jugador $winnerName!"
                        },
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