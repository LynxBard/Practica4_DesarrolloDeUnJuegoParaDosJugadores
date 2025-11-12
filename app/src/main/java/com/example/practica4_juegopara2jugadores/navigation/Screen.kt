package com.example.practica4_juegopara2jugadores.navigation

sealed class Screen {
    object MainMenu : Screen()
    object GameModeSelection : Screen()
    object LocalGame : Screen()
    object SinglePlayerGame : Screen()
    object BluetoothSetup : Screen()
    object BluetoothGame : Screen()
    object SaveLoadMenu : Screen()
}
