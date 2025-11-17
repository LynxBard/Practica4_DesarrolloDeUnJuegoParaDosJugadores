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
    object BluetoothSetup : Screen()
    object BluetoothGame : Screen()
    object SaveLoadMenu : Screen()
    data class LoadedGame(val gameState: GameState) : Screen()
}