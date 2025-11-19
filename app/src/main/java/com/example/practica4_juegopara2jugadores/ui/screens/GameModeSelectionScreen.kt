package com.example.practica4_juegopara2jugadores.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.practica4_juegopara2jugadores.navigation.Screen
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameModeSelectionScreen(
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    var cardsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        cardsVisible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Seleccionar Modo de Juego",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
            ) {
                AnimatedVisibility(
                    visible = cardsVisible,
                    enter = fadeIn(animationSpec = tween(400)) +
                            slideInVertically(
                                initialOffsetY = { it / 4 },
                                animationSpec = tween(400)
                            )
                ) {
                    GameModeCard(
                        icon = "🤖",
                        title = "1 Jugador vs IA",
                        description = "Juega contra la inteligencia artificial",
                        onClick = { onNavigate(Screen.SinglePlayerGame) },
                        isPrimary = false
                    )
                }

                AnimatedVisibility(
                    visible = cardsVisible,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 100)) +
                            slideInVertically(
                                initialOffsetY = { it / 4 },
                                animationSpec = tween(400, delayMillis = 100)
                            )
                ) {
                    GameModeCard(
                        icon = "👥",
                        title = "2 Jugadores Local",
                        description = "Juega en el mismo dispositivo con un amigo",
                        onClick = { onNavigate(Screen.LocalGame) },
                        isPrimary = true
                    )
                }

                AnimatedVisibility(
                    visible = cardsVisible,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 200)) +
                            slideInVertically(
                                initialOffsetY = { it / 4 },
                                animationSpec = tween(400, delayMillis = 200)
                            )
                ) {
                    GameModeCard(
                        icon = "📱",
                        title = "2 Jugadores Bluetooth",
                        description = "Conecta con otro dispositivo por Bluetooth",
                        onClick = { onNavigate(Screen.BluetoothSetup) },
                        isPrimary = false
                    )
                }
            }
        }
    }
}

@Composable
private fun GameModeCard(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit,
    isPrimary: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        )
    )

    Card(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPrimary) 12.dp else 6.dp
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono grande
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = if (isPrimary) {
                            Color.White.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        },
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 40.sp
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Texto
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPrimary) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = if (isPrimary) {
                        Color.White.copy(alpha = 0.9f)
                    } else {
                        Color.Gray
                    },
                    textAlign = TextAlign.Start,
                    lineHeight = 18.sp
                )
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}