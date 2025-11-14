package com.example.practica4_juegopara2jugadores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.practica4_juegopara2jugadores.domain.ai.ConnectFourAI
import com.example.practica4_juegopara2jugadores.domain.ai.Difficulty
import com.example.practica4_juegopara2jugadores.model.GameMode
import com.example.practica4_juegopara2jugadores.model.Player
import com.example.practica4_juegopara2jugadores.navigation.Screen
import com.example.practica4_juegopara2jugadores.ui.GameScreen
import com.example.practica4_juegopara2jugadores.ui.screens.AIConfigScreen
import com.example.practica4_juegopara2jugadores.ui.screens.GameModeSelectionScreen
import com.example.practica4_juegopara2jugadores.ui.screens.MainMenuScreen
import com.example.practica4_juegopara2jugadores.ui.theme.ConnectFourTheme
import com.example.practica4_juegopara2jugadores.viewmodel.GameViewModel
import com.example.practica4_juegopara2jugadores.viewmodel.NavigationViewModel

/**
 * Activity principal de la aplicación
 * Utiliza Jetpack Compose para la UI con sistema de navegación
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ConnectFourTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConnectFourApp()
                }
            }
        }
    }
}

@Composable
fun ConnectFourApp(
    navigationViewModel: NavigationViewModel = viewModel()
) {
    val currentScreen by navigationViewModel.currentScreen.collectAsState()

    when (currentScreen) {
        is Screen.MainMenu -> {
            MainMenuScreen(
                onNavigate = { screen -> navigationViewModel.navigateTo(screen) }
            )
        }

        is Screen.GameModeSelection -> {
            GameModeSelectionScreen(
                onNavigate = { screen -> navigationViewModel.navigateTo(screen) },
                onBack = { navigationViewModel.navigateBack() }
            )
        }

        is Screen.LocalGame -> {
            val gameViewModel: GameViewModel = viewModel()
            gameViewModel.setGameMode(GameMode.LOCAL_MULTIPLAYER)

            GameScreen(
                viewModel = gameViewModel,
                onBack = { navigationViewModel.navigateBack() }
            )
        }

        is Screen.SinglePlayerGame -> {
            // Mostrar pantalla de configuración de IA
            AIConfigScreen(
                onStartGame = { difficulty, playerGoesFirst ->
                    // Crear el ViewModel con IA configurada
                    val ai = ConnectFourAI(difficulty, Player.YELLOW)
                    navigationViewModel.navigateToSinglePlayerWithConfig(difficulty, playerGoesFirst)
                },
                onBack = { navigationViewModel.navigateBack() }
            )
        }

        is Screen.SinglePlayerGameWithConfig -> {
            val config = (currentScreen as Screen.SinglePlayerGameWithConfig)

            // Usar remember para mantener la IA y el ViewModel durante toda la sesión
            val ai = remember(config.difficulty) {
                ConnectFourAI(config.difficulty, Player.YELLOW)
            }

            val gameViewModel: GameViewModel = remember(config.difficulty, config.playerGoesFirst) {
                GameViewModel(ai, config.playerGoesFirst).apply {
                    setGameMode(GameMode.SINGLE_PLAYER)
                }
            }

            GameScreen(
                viewModel = gameViewModel,
                onBack = { navigationViewModel.navigateBack() },
                showAIIndicator = true
            )
        }

        is Screen.BluetoothSetup -> {
            PlaceholderScreen(
                title = "Configuración Bluetooth",
                message = "Próximamente: Juego por Bluetooth",
                onBack = { navigationViewModel.navigateBack() }
            )
        }

        is Screen.BluetoothGame -> {
            PlaceholderScreen(
                title = "Juego Bluetooth",
                message = "Partida en progreso...",
                onBack = { navigationViewModel.navigateBack() }
            )
        }

        is Screen.SaveLoadMenu -> {
            PlaceholderScreen(
                title = "Cargar Partida",
                message = "Próximamente: Sistema de guardado",
                onBack = { navigationViewModel.navigateBack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(
    title: String,
    message: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onBack) {
                    Text("Volver")
                }
            }
        }
    }
}