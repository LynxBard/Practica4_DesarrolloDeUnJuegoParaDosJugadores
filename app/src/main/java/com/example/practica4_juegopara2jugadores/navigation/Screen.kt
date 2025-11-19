package com.example.practica4_juegopara2jugadores.navigation

import com.example.practica4_juegopara2jugadores.domain.ai.Difficulty
import com.example.practica4_juegopara2jugadores.model.GameState

sealed class Screen {
    object MainMenu : Screen()
    object GameModeSelection : Screen()
    object LocalGame : Screen()
    object SinglePlayerGame : Screen()
    data class SinglePlayerGameWithConfig(
        val difficulty: Difficulty,
        val playerGoesFirst: Boolean
    ) : Screen()

    // Pantallas Bluetooth
    object BluetoothSetup : Screen()
    data class BluetoothGame(val isHost: Boolean) : Screen()

    // Partidas guardadas
    object SaveLoadMenu : Screen()
    data class LoadedGame(val gameState: GameState) : Screen()

    // NUEVO: Pantalla de estadísticas
    object Statistics : Screen()
}