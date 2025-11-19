package com.example.practica4_juegopara2jugadores.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.practica4_juegopara2jugadores.model.Move
import com.example.practica4_juegopara2jugadores.model.Player
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveHistoryScreen(
    moves: List<Move>,
    elapsedTimeSeconds: Int,
    onBack: () -> Unit,
    onUndoMove: (() -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll al último movimiento
    LaunchedEffect(moves.size) {
        if (moves.isNotEmpty()) {
            listState.animateScrollToItem(moves.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Historial de Movimientos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "${moves.size} movimientos",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (onUndoMove != null && moves.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onUndoMove,
                    containerColor = Color(0xFFFF9800),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Refresh, "Deshacer")
                }
            }
        }
    ) { paddingValues ->
        if (moves.isEmpty()) {
            EmptyHistoryState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Resumen de estadísticas
                HistoryStatistics(
                    moves = moves,
                    elapsedTimeSeconds = elapsedTimeSeconds,
                    modifier = Modifier.padding(16.dp)
                )

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                // Lista de movimientos
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = moves,
                        key = { it.timestamp }
                    ) { move ->
                        MoveItem(
                            moveNumber = moves.indexOf(move) + 1,
                            move = move,
                            isLatest = move == moves.lastOrNull()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryStatistics(
    moves: List<Move>,
    elapsedTimeSeconds: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.Timer,
                label = "Tiempo",
                value = formatTime(elapsedTimeSeconds),
                color = MaterialTheme.colorScheme.primary
            )

            VerticalDivider(modifier = Modifier.height(40.dp))

            StatItem(
                icon = Icons.Default.ArrowForward,
                label = "Movimientos",
                value = moves.size.toString(),
                color = Color(0xFF4CAF50)
            )

            VerticalDivider(modifier = Modifier.height(40.dp))

            val avgTime = if (moves.isNotEmpty()) elapsedTimeSeconds / moves.size else 0
            StatItem(
                icon = Icons.Default.Speed,
                label = "Promedio",
                value = "${avgTime}s",
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun MoveItem(
    moveNumber: Int,
    move: Move,
    isLatest: Boolean
) {
    val playerColor = if (move.player == Player.RED) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
    val scale by animateFloatAsState(
        targetValue = if (isLatest) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isLatest) {
                playerColor.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLatest) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Número del movimiento
            Surface(
                modifier = Modifier.size(40.dp),
                color = playerColor.copy(alpha = 0.2f),
                shape = CircleShape
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$moveNumber",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = playerColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información del movimiento
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(playerColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (move.player == Player.RED) "Rojo" else "Amarillo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ViewColumn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Columna ${move.column + 1}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Height,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Fila ${6 - move.row}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Tiempo del movimiento
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatTimestamp(move.timestamp),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                }
            }

            if (isLatest) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.NewReleases,
                    contentDescription = "Último",
                    tint = playerColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.Gray
            )
            Text(
                text = "No hay movimientos",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Text(
                text = "Los movimientos aparecerán aquí\na medida que juegues",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

private fun formatTimestamp(timestamp: Long): String {
    val seconds = (timestamp / 1000) % 60
    val minutes = (timestamp / 1000 / 60) % 60
    return String.format("%02d:%02d", minutes, seconds)
}