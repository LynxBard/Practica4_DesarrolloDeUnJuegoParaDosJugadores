package com.example.practica4_juegopara2jugadores.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.practica4_juegopara2jugadores.model.Cell
import com.example.practica4_juegopara2jugadores.model.GameState
import com.example.practica4_juegopara2jugadores.model.Player
import com.example.practica4_juegopara2jugadores.viewmodel.GameViewModel

// Colores del juego
val RedPlayer = Color(0xFFE53935)
val YellowPlayer = Color(0xFFFDD835)
val BoardBlue = Color(0xFF1976D2)
val BackgroundColor = Color(0xFFF5F5F5)
val EmptyCell = Color(0xFFFFFFFF)

@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel()
) {
    val gameState by viewModel.gameState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header con título
        Text(
            text = "Connect Four",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = BoardBlue,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Indicador de turno y puntuación
        ScoreBoard(gameState)

        Spacer(modifier = Modifier.height(24.dp))

        // Tablero de juego
        GameBoard(
            gameState = gameState,
            onColumnClick = { column -> viewModel.makeMove(column) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botones de control
        ControlButtons(
            onResetGame = { viewModel.resetGame() },
            onResetAll = { viewModel.resetAll() }
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

@Composable
fun ScoreBoard(gameState: GameState) {
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
            isCurrentPlayer = gameState.currentPlayer == Player.RED && !gameState.isGameOver()
        )

        // Jugador Amarillo
        PlayerScore(
            player = Player.YELLOW,
            score = gameState.yellowWins,
            isCurrentPlayer = gameState.currentPlayer == Player.YELLOW && !gameState.isGameOver()
        )
    }
}

@Composable
fun PlayerScore(
    player: Player,
    score: Int,
    isCurrentPlayer: Boolean
) {
    val playerColor = if (player == Player.RED) RedPlayer else YellowPlayer
    val scale by animateFloatAsState(
        targetValue = if (isCurrentPlayer) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
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
                    .background(playerColor)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (player == Player.RED) "Rojo" else "Amarillo",
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
                    text = "Tu turno",
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
    onColumnClick: (Int) -> Unit
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
                        Cell(
                            cell = gameState.board[row][col],
                            isWinningCell = gameState.winningCells.contains(Pair(row, col)),
                            isLastMove = gameState.lastMove == Pair(row, col),
                            onClick = {
                                if (!gameState.isGameOver()) {
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
fun Cell(
    cell: Cell,
    isWinningCell: Boolean,
    isLastMove: Boolean,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(cell) {
        if (cell is Cell.Occupied) {
            visible = false
            kotlinx.coroutines.delay(50)
            visible = true
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isWinningCell) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .padding(4.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible || cell is Cell.Empty,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        when (cell) {
                            is Cell.Empty -> EmptyCell
                            is Cell.Occupied -> if (cell.player == Player.RED) RedPlayer else YellowPlayer
                        }
                    )
                    .then(
                        if (isLastMove && cell is Cell.Occupied) {
                            Modifier.shadow(4.dp, CircleShape)
                        } else Modifier
                    )
            )
        }
    }
}

@Composable
fun ControlButtons(
    onResetGame: () -> Unit,
    onResetAll: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onResetGame,
            colors = ButtonDefaults.buttonColors(containerColor = BoardBlue)
        ) {
            Text("Nueva Partida")
        }

        OutlinedButton(
            onClick = onResetAll
        ) {
            Text("Reiniciar Todo")
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
                    val winnerName = if (gameState.winner == Player.RED) "Rojo" else "Amarillo"

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(winnerColor)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "¡Gana el jugador $winnerName!",
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