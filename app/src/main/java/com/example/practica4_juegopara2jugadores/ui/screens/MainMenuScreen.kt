package com.example.practica4_juegopara2jugadores.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.practica4_juegopara2jugadores.navigation.Screen
import com.example.practica4_juegopara2jugadores.ui.BoardBlue
import com.example.practica4_juegopara2jugadores.ui.RedPlayer
import com.example.practica4_juegopara2jugadores.ui.YellowPlayer
import kotlinx.coroutines.delay
import kotlin.system.exitProcess

@Composable
fun MainMenuScreen(
    onNavigate: (Screen) -> Unit
) {
    // Animación del título
    var titleVisible by remember { mutableStateOf(false) }
    var buttonsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        titleVisible = true
        delay(300)
        buttonsVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BoardBlue.copy(alpha = 0.1f),
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
            verticalArrangement = Arrangement.Center
        ) {
            // Título animado
            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(animationSpec = tween(800)) +
                        slideInVertically(
                            initialOffsetY = { -100 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Connect Four",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = BoardBlue,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Decoración de círculos de colores
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(
                                        color = if (index % 2 == 0) RedPlayer else YellowPlayer,
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Botones del menú
            AnimatedVisibility(
                visible = buttonsVisible,
                enter = fadeIn(animationSpec = tween(600)) +
                        expandVertically(animationSpec = tween(600))
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    MenuButton(
                        text = "🎮 Jugar",
                        onClick = { onNavigate(Screen.GameModeSelection) },
                        isPrimary = true
                    )

                    MenuButton(
                        text = "💾 Cargar Partida",
                        onClick = { onNavigate(Screen.SaveLoadMenu) },
                        isPrimary = false
                    )

                    MenuButton(
                        text = "📊 Estadísticas",
                        onClick = { onNavigate(Screen.Statistics) },
                        isPrimary = false
                    )

                    MenuButton(
                        text = "❌ Salir",
                        onClick = { exitProcess(0) },
                        isPrimary = false,
                        isDestructive = true
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuButton(
    text: String,
    onClick: () -> Unit,
    isPrimary: Boolean,
    isDestructive: Boolean = false
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
            .height(70.dp)
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDestructive -> Color(0xFFFF5252).copy(alpha = 0.1f)
                isPrimary -> BoardBlue
                else -> Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPrimary) 8.dp else 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isDestructive -> Color(0xFFD32F2F)
                    isPrimary -> Color.White
                    else -> BoardBlue
                }
            )
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}