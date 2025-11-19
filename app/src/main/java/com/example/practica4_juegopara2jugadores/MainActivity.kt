package com.example.practica4_juegopara2jugadores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.practica4_juegopara2jugadores.data.StatisticsRepository
import com.example.practica4_juegopara2jugadores.data.ThemePreferencesRepository
import com.example.practica4_juegopara2jugadores.data.bluetooth.BluetoothGameService
import com.example.practica4_juegopara2jugadores.domain.ai.ConnectFourAI
import com.example.practica4_juegopara2jugadores.model.GameMode
import com.example.practica4_juegopara2jugadores.model.Player
import com.example.practica4_juegopara2jugadores.navigation.Screen
import com.example.practica4_juegopara2jugadores.ui.GameScreen
import com.example.practica4_juegopara2jugadores.ui.screens.*
import com.example.practica4_juegopara2jugadores.ui.theme.ConnectFourTheme
import com.example.practica4_juegopara2jugadores.ui.theme.ThemeConfig
import com.example.practica4_juegopara2jugadores.viewmodel.GameViewModel
import com.example.practica4_juegopara2jugadores.viewmodel.NavigationViewModel

/**
 * Activity principal de la aplicación
 * Utiliza Jetpack Compose para la UI con sistema de navegación y temas personalizables
 */
class MainActivity : ComponentActivity() {

    // Inicializar como lazy para evitar problemas de timing
    private val bluetoothService by lazy { BluetoothGameService(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val themeRepository = remember { ThemePreferencesRepository(context) }
            val themeConfig by themeRepository.themeConfigFlow.collectAsState(initial = ThemeConfig())

            ConnectFourTheme(
                themeType = themeConfig.themeType,
                colorMode = themeConfig.colorMode
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConnectFourApp(
                        bluetoothService = bluetoothService,
                        currentThemeConfig = themeConfig
                    )
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
    currentThemeConfig: ThemeConfig,
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

        // Pantalla de configuración Bluetooth
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
            // Pantalla de carga de partidas
            val repository = remember { GameSaveRepository(context) }

            LoadGameScreen(
                repository = repository,
                onBack = { navigationViewModel.navigateBack() },
                onLoadGame = { gameState ->
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

        // Pantalla de estadísticas
        is Screen.Statistics -> {
            val statsRepository = remember { StatisticsRepository(context) }

            StatisticsScreen(
                repository = statsRepository,
                onBack = { navigationViewModel.navigateBack() }
            )
        }

        // NUEVO: Pantalla de configuración de temas
        is Screen.Settings -> {
            SettingsScreen(
                currentThemeConfig = currentThemeConfig,
                onThemeConfigChange = { /* Los cambios se aplican automáticamente */ },
                onBack = { navigationViewModel.navigateBack() }
            )
        }
    }
}