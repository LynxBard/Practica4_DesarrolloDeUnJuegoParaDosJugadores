package com.example.practica4_juegopara2jugadores.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.practica4_juegopara2jugadores.data.GameStatistics
import com.example.practica4_juegopara2jugadores.data.StatisticsRepository
import com.example.practica4_juegopara2jugadores.model.Player
import com.example.practica4_juegopara2jugadores.ui.BoardBlue
import com.example.practica4_juegopara2jugadores.ui.RedPlayer
import com.example.practica4_juegopara2jugadores.ui.YellowPlayer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    repository: StatisticsRepository,
    onBack: () -> Unit
) {
    val statistics by repository.statisticsFlow.collectAsState(initial = GameStatistics())
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Estadísticas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Tu historial de juego",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
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
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = "Reiniciar estadísticas",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Resumen general
            GeneralStatsCard(statistics)

            // Gráfico de victorias
            WinsChartCard(statistics)

            // Estadísticas por modo de juego
            GameModeStatsCard(statistics)

            // Estadísticas contra IA
            if (statistics.singlePlayerGamesPlayed > 0) {
                AIStatsCard(statistics)
            }

            // Rachas
            StreakCard(statistics)

            // Tiempo promedio
            TimeStatsCard(statistics)
        }
    }

    // Diálogo de confirmación de reinicio
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
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
                    "Reiniciar Estadísticas",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("¿Estás seguro de que deseas reiniciar todas las estadísticas? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.resetAllStatistics()
                            snackbarHostState.showSnackbar(
                                "Estadísticas reiniciadas",
                                duration = SnackbarDuration.Short
                            )
                        }
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252)
                    )
                ) {
                    Text("Reiniciar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun GeneralStatsCard(stats: GameStatistics) {
    val totalGames = stats.localGamesPlayed + stats.singlePlayerGamesPlayed + stats.bluetoothGamesPlayed

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = BoardBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Resumen General",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBox(
                    icon = Icons.Default.Gamepad,
                    value = totalGames.toString(),
                    label = "Partidas",
                    color = BoardBlue
                )

                StatBox(
                    icon = Icons.Default.EmojiEvents,
                    value = (stats.redWinsTotal + stats.yellowWinsTotal).toString(),
                    label = "Victorias",
                    color = Color(0xFFFFC107)
                )

                StatBox(
                    icon = Icons.Default.Balance,
                    value = stats.drawsTotal.toString(),
                    label = "Empates",
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun WinsChartCard(stats: GameStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = null,
                    tint = BoardBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Victorias por Jugador",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gráfico de barras
            WinsBarChart(
                redWins = stats.redWinsTotal,
                yellowWins = stats.yellowWinsTotal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Leyenda
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("Rojo", stats.redWinsTotal, RedPlayer)
                LegendItem("Amarillo", stats.yellowWinsTotal, YellowPlayer)
            }
        }
    }
}

@Composable
private fun WinsBarChart(
    redWins: Int,
    yellowWins: Int,
    modifier: Modifier = Modifier
) {
    val maxWins = maxOf(redWins, yellowWins, 1)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (redWins == 0 && yellowWins == 0) {
            Text(
                "No hay datos disponibles",
                color = Color.Gray,
                fontSize = 14.sp
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = size.width * 0.3f
                val spacing = size.width * 0.1f
                val maxBarHeight = size.height * 0.8f

                // Barra Roja
                val redBarHeight = (redWins.toFloat() / maxWins) * maxBarHeight
                drawRoundRect(
                    color = RedPlayer,
                    topLeft = Offset(spacing, size.height - redBarHeight),
                    size = Size(barWidth, redBarHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                )

                // Barra Amarilla
                val yellowBarHeight = (yellowWins.toFloat() / maxWins) * maxBarHeight
                drawRoundRect(
                    color = YellowPlayer,
                    topLeft = Offset(size.width - spacing - barWidth, size.height - yellowBarHeight),
                    size = Size(barWidth, yellowBarHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                )

                // Líneas de grid
                for (i in 0..5) {
                    val y = size.height - (i * maxBarHeight / 5)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    value: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, CircleShape)
        )
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = value.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GameModeStatsCard(stats: GameStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Category,
                    contentDescription = null,
                    tint = BoardBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Partidas por Modo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ModeStatRow("👥 Local", stats.localGamesPlayed, Color(0xFF4CAF50))
            Spacer(modifier = Modifier.height(8.dp))
            ModeStatRow("🤖 vs IA", stats.singlePlayerGamesPlayed, Color(0xFF2196F3))
            Spacer(modifier = Modifier.height(8.dp))
            ModeStatRow("📱 Bluetooth", stats.bluetoothGamesPlayed, Color(0xFFFF9800))
        }
    }
}

@Composable
private fun ModeStatRow(
    label: String,
    value: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun AIStatsCard(stats: GameStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    tint = BoardBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Contra la IA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AILevelStat("Fácil", stats.easyAIWins, stats.easyAILosses, Color(0xFF4CAF50))
            Spacer(modifier = Modifier.height(8.dp))
            AILevelStat("Medio", stats.mediumAIWins, stats.mediumAILosses, Color(0xFFFF9800))
            Spacer(modifier = Modifier.height(8.dp))
            AILevelStat("Difícil", stats.hardAIWins, stats.hardAILosses, Color(0xFFFF5252))
        }
    }
}

@Composable
private fun AILevelStat(
    level: String,
    wins: Int,
    losses: Int,
    color: Color
) {
    val total = wins + losses
    val winRate = if (total > 0) (wins.toFloat() / total * 100).toInt() else 0

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(level, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "$wins-$losses ($winRate%)",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (total > 0) wins.toFloat() / total else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
        )
    }
}

@Composable
private fun StreakCard(stats: GameStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (stats.currentStreak > 0) {
                Color(0xFFFFC107).copy(alpha = 0.1f)
            } else {
                Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Rachas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StreakBox(
                    label = "Actual",
                    value = stats.currentStreak,
                    winner = stats.lastWinner,
                    isActive = stats.currentStreak > 0
                )

                StreakBox(
                    label = "Mejor",
                    value = stats.bestStreak,
                    winner = null,
                    isActive = false
                )
            }
        }
    }
}

@Composable
private fun StreakBox(
    label: String,
    value: Int,
    winner: String?,
    isActive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = if (isActive) Color(0xFFFF9800).copy(alpha = 0.2f) else Color(0xFFF5F5F5),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color(0xFFFF9800) else Color.Gray
                )
                if (isActive && winner != null) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = if (winner == "RED") RedPlayer else YellowPlayer,
                                shape = CircleShape
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun TimeStatsCard(stats: GameStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = BoardBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Tiempo de Juego",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimeBox(
                    label = "Promedio",
                    seconds = stats.averageGameTime,
                    color = BoardBlue
                )

                TimeBox(
                    label = "Total",
                    seconds = stats.totalGameTime,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun TimeBox(
    label: String,
    seconds: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatTime(seconds),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun StatBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

private fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}