package com.example.practica4_juegopara2jugadores.viewmodel

import androidx.lifecycle.ViewModel
import com.example.practica4_juegopara2jugadores.domain.GameLogic
import com.example.practica4_juegopara2jugadores.model.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel que maneja el estado del juego Connect Four
 * Sigue el patrón MVVM y maneja la configuración de rotación automáticamente
 */
class GameViewModel : ViewModel() {

    // Estado privado mutable
    private val _gameState = MutableStateFlow(GameState())

    // Estado público inmutable
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    /**
     * Intenta hacer un movimiento en la columna especificada
     * @param column Número de columna (0-6)
     */
    fun makeMove(column: Int) {
        val currentState = _gameState.value
        val newState = GameLogic.makeMove(currentState, column)

        if (newState != null) {
            _gameState.value = newState
        }
    }

    /**
     * Reinicia el juego manteniendo el conteo de victorias
     */
    fun resetGame() {
        _gameState.value = GameLogic.resetGame(_gameState.value)
    }

    /**
     * Reinicia completamente el juego incluyendo el conteo de victorias
     */
    fun resetAll() {
        _gameState.value = GameLogic.resetAll()
    }

    /**
     * Obtiene el estado actual del juego
     */
    fun getCurrentState(): GameState = _gameState.value
}