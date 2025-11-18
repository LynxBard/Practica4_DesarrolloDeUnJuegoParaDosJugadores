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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.practica4_juegopara2jugadores.data.GameSaveRepository
import com.example.practica4_juegopara2jugadores.data.bluetooth.BluetoothGameService // NUEVO
import com.example.practica4_juegopara2jugadores.domain.ai.ConnectFourAI
import com.example.practica4_juegopara2jugadores.model.GameMode
import com.example.practica4_juegopara2jugadores.model.Player
import com.example.practica4_juegopara2jugadores.navigation.Screen
import com.example.practica4_juegopara2jugadores.ui.GameScreen
import com.example.practica4_juegopara2jugadores.ui.screens.AIConfigScreen
import com.example.practica4_juegopara2jugadores.ui.screens.GameModeSelectionScreen
import com.example.practica4_juegopara2jugadores.ui.screens.LoadGameScreen
import com.example.practica4_juegopara2jugadores.ui.screens.MainMenuScreen
import com.example.practica4_juegopara2jugadores.ui.screens.BluetoothSetupScreen
import com.example.practica4_juegopara2jugadores.ui.screens.BluetoothGameScreen
import com.example.practica4_juegopara2jugadores.ui.theme.ConnectFourTheme
import com.example.practica4_juegopara2jugadores.viewmodel.GameViewModel
import com.example.practica4_juegopara2jugadores.viewmodel.NavigationViewModel

/**
 * Activity principal de la aplicación
 * Utiliza Jetpack Compose para la UI con sistema de navegación
 */
class MainActivity : ComponentActivity() {

    private lateinit var bluetoothService: BluetoothGameService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ConnectFourTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConnectFourApp(bluetoothService)
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.cleanup()
    }
}

@Composable
fun ConnectFourApp(
    bluetoothService: BluetoothGameService,
    navigationViewModel: NavigationViewModel = viewModel()
) {
    val currentScreen by navigationViewModel.currentScreen.collectAsState()
    val context = LocalContext.current

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

        // NUEVO: Pantalla de configuración Bluetooth
        is Screen.BluetoothSetup -> {
            BluetoothSetupScreen(
                bluetoothService = bluetoothService,
                onConnectionEstablished = { isHost ->
                    navigationViewModel.navigateTo(Screen.BluetoothGame(isHost))
                },
                onBack = { navigationViewModel.navigateBack() }
            )
        }

        is Screen.BluetoothGame -> {
            val isHost = (currentScreen as Screen.BluetoothGame).isHost

            BluetoothGameScreen(
                bluetoothService = bluetoothService,
                isHost = isHost,
                onBack = {
                    bluetoothService.disconnect()
                    navigationViewModel.navigateBack()
                }
            )
        }

        is Screen.SaveLoadMenu -> {
            // Nueva pantalla de carga de partidas
            val repository = remember { GameSaveRepository(context) }

            LoadGameScreen(
                repository = repository,
                onBack = { navigationViewModel.navigateBack() },
                onLoadGame = { gameState ->
                    // Cargar el juego según su modo
                    navigationViewModel.navigateToLoadedGame(gameState)
                },
                onNewGame = {
                    navigationViewModel.navigateTo(Screen.GameModeSelection)
                }
            )
        }

        is Screen.LoadedGame -> {
            val loadedState = (currentScreen as Screen.LoadedGame).gameState

            // Determinar si necesita IA según el modo de juego
            val gameViewModel: GameViewModel = if (loadedState.gameMode == GameMode.SINGLE_PLAYER) {
                remember {
                    val ai = ConnectFourAI(
                        difficulty = com.example.practica4_juegopara2jugadores.domain.ai.Difficulty.MEDIUM,
                        aiPlayer = Player.YELLOW
                    )
                    GameViewModel(ai, playerGoesFirst = true).apply {
                        // Cargar el estado guardado
                        loadGameState(loadedState)
                    }
                }
            } else {
                remember {
                    GameViewModel().apply {
                        loadGameState(loadedState)
                    }
                }
            }

            GameScreen(
                viewModel = gameViewModel,
                onBack = { navigationViewModel.navigateBack() },
                showAIIndicator = loadedState.gameMode == GameMode.SINGLE_PLAYER
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