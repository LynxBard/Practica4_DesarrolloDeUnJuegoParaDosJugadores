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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.practica4_juegopara2jugadores.navigation.Screen
import com.example.practica4_juegopara2jugadores.ui.GameScreen
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
    navigationViewModel: NavigationViewModel = viewModel(),
    gameViewModel: GameViewModel = viewModel()
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
            GameScreen(
                viewModel = gameViewModel
            )
        }

        is Screen.SinglePlayerGame -> {
            // TODO: Implementar pantalla de juego vs IA
            PlaceholderScreen(
                title = "Modo 1 Jugador",
                message = "Próximamente: Juego contra IA",
                onBack = { navigationViewModel.navigateBack() }
            )
        }

        is Screen.BluetoothSetup -> {
            // TODO: Implementar configuración Bluetooth
            PlaceholderScreen(
                title = "Configuración Bluetooth",
                message = "Próximamente: Juego por Bluetooth",
                onBack = { navigationViewModel.navigateBack() }
            )
        }

        is Screen.BluetoothGame -> {
            // TODO: Implementar juego Bluetooth
            PlaceholderScreen(
                title = "Juego Bluetooth",
                message = "Partida en progreso...",
                onBack = { navigationViewModel.navigateBack() }
            )
        }

        is Screen.SaveLoadMenu -> {
            // TODO: Implementar menú de guardar/cargar
            PlaceholderScreen(
                title = "Cargar Partida",
                message = "Próximamente: Sistema de guardado",
                onBack = { navigationViewModel.navigateBack() }
            )
        }
    }
}

// En C:/Users/Public/Documents/moviles/Practica4_JuegoPara2Jugadores/app/src/main/java/com/example/practica4_juegopara2jugadores/MainActivity.kt

@OptIn(ExperimentalMaterial3Api::class) // <-- Añade esta línea
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