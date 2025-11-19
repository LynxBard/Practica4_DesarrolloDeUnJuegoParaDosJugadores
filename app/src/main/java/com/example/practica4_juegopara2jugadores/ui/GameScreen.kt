package com.example.practica4_juegopara2jugadores.ui

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
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.practica4_juegopara2jugadores.data.GameSaveRepository
import com.example.practica4_juegopara2jugadores.data.StatisticsRepository
import com.example.practica4_juegopara2jugadores.model.Cell
import com.example.practica4_juegopara2jugadores.model.GameMode
import com.example.practica4_juegopara2jugadores.model.GameState
import com.example.practica4_juegopara2jugadores.model.Player
import com.example.practica4_juegopara2jugadores.ui.screens.MoveHistoryScreen
import com.example.practica4_juegopara2jugadores.ui.screens.SaveGameScreen
import com.example.practica4_juegopara2jugadores.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import kotlin.math.min

// Color del tablero - usa el primary del tema
val BoardBlue
    @Composable
    get() = MaterialTheme.colorScheme.primary

// Color del fondo de la app
val BackgroundColor
    @Composable
    get() = MaterialTheme.colorScheme.background

// Color para las celdas vacías
val EmptyCell
    @Composable
    get() = MaterialTheme.colorScheme.surface

// Colores de los jugadores - usan tertiary y secondary del tema
val RedPlayer
    @Composable
    get() = MaterialTheme.colorScheme.tertiary

val YellowPlayer
    @Composable
    get() = MaterialTheme.colorScheme.secondary

/**
 * Versiones estáticas para uso en objetos (fallback)
 * Solo se usan en casos donde no hay contexto de Composable
 */
object StaticColors {
    val RedPlayerStatic = Color(0xFFE53935)
    val YellowPlayerStatic = Color(0xFFFDD835)
    val BoardBlueStatic = Color(0xFF1976D2)
    val BackgroundColorStatic = Color(0xFFF5F5F5)
    val EmptyCellStatic = Color(0xFFFFFFFF)
}

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

    // Estado para mostrar diálogos
    var showSaveDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Repositorio de estadísticas
    val statsRepository = remember { StatisticsRepository(context) }

    // Variable para rastrear si ya se actualizaron las estadísticas
    var statsUpdated by remember { mutableStateOf(false) }

    // Actualizar estadísticas cuando el juego termina (solo una vez)
    LaunchedEffect(gameState.isGameOver()) {
        if (gameState.isGameOver() && !statsUpdated && gameState.moveHistory.isNotEmpty()) {
            statsUpdated = true

            // Determinar dificultad si es contra IA (puedes guardarlo en el ViewModel)
            val aiDifficulty = if (gameState.gameMode == GameMode.SINGLE_PLAYER) {
                "MEDIUM" // Valor por defecto, idealmente obtenlo del ViewModel
            } else null

            statsRepository.updateAfterGame(
                gameMode = gameState.gameMode,
                winner = gameState.winner,
                isDraw = gameState.isDraw,
                gameTimeSeconds = gameState.elapsedTimeSeconds,
                aiDifficulty = aiDifficulty
            )
        }

        // Resetear cuando comience un nuevo juego
        if (!gameState.isGameOver()) {
            statsUpdated = false
        }
    }

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
                        // Mostrar temporizador
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = viewModel.formatElapsedTime(gameState.elapsedTimeSeconds),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        // Botón para ver historial con badge
                        BadgedBox(
                            badge = {
                                if (gameState.moveHistory.isNotEmpty()) {
                                    Badge(
                                        containerColor = YellowPlayer
                                    ) {
                                        Text(
                                            gameState.moveHistory.size.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            IconButton(
                                onClick = { showHistoryDialog = true }
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = "Historial",
                                    tint = Color.White
                                )
                            }
                        }

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
        // Hacer la columna scrollable para pantallas pequeñas
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
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

            Spacer(modifier = Modifier.height(8.dp))

            // Indicador de turno y puntuación
            ScoreBoard(
                gameState = gameState,
                isAIThinking = isAIThinking && showAIIndicator
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tablero de juego (ahora responsivo)
            GameBoard(
                gameState = gameState,
                onColumnClick = { column -> viewModel.makeMove(column) },
                isInteractionEnabled = !isAIThinking
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de control
            ControlButtons(
                onResetGame = { viewModel.resetGame() },
                onResetAll = { viewModel.resetAll() },
                onSaveGame = { showSaveDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

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

    // Diálogo de historial de movimientos
    if (showHistoryDialog) {
        Dialog(
            onDismissRequest = { showHistoryDialog = false },
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
                MoveHistoryScreen(
                    moves = gameState.moveHistory,
                    elapsedTimeSeconds = gameState.elapsedTimeSeconds,
                    onBack = { showHistoryDialog = false },
                    onUndoMove = null // Puedes implementar deshacer si lo deseas
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
            .padding(horizontal = 8.dp),
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
            containerColor = if (isCurrentPlayer) playerColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
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
                if (isAIThinking) {
                    Text(
                        text = "🤔",
                        fontSize = 18.sp,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = rotation
                        }
                    )
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
                Text(
                    text = if (isAIThinking) "Pensando..." else "Tu turno",
                    fontSize = 11.sp,
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
    // Obtener dimensiones de pantalla
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp

    // Calcular tamaño de celda basado en ancho de pantalla
    val availableWidth = screenWidthDp - 36.dp - 18.dp
    val cellSize = min((availableWidth / GameState.COLUMNS).value, 52f).dp

    Card(
        modifier = Modifier
            .wrapContentSize()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = BoardBlue)
    ) {
        Column(
            modifier = Modifier.padding(7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (row in 0 until GameState.ROWS) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (col in 0 until GameState.COLUMNS) {
                        CellView(
                            cell = gameState.board[row][col],
                            isWinningCell = gameState.winningCells.contains(Pair(row, col)),
                            isLastMove = gameState.lastMove == Pair(row, col),
                            cellSize = cellSize,
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
    cellSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isWinningCell) 1.15f else 1f,
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
            .size(cellSize)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
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
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onResetGame,
                colors = ButtonDefaults.buttonColors(containerColor = BoardBlue),
                modifier = Modifier.weight(1f)
            ) {
                Text("Nueva Partida", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onResetAll,
                modifier = Modifier.weight(1f)
            ) {
                Text("Reiniciar Todo", fontSize = 12.sp)
            }
        }

        OutlinedButton(
            onClick = onSaveGame,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Guardar Partida", fontSize = 12.sp)
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