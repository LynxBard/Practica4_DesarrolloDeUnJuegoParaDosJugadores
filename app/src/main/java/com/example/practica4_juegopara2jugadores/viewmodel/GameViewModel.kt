package com.example.practica4_juegopara2jugadores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.practica4_juegopara2jugadores.domain.ai.ConnectFourAI
import com.example.practica4_juegopara2jugadores.domain.GameLogic
import com.example.practica4_juegopara2jugadores.model.GameMode
import com.example.practica4_juegopara2jugadores.model.GameState
import com.example.practica4_juegopara2jugadores.model.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel que maneja el estado del juego Connect Four
 * Sigue el patrón MVVM y maneja la configuración de rotación automáticamente
 */
class GameViewModel(
    private val ai: ConnectFourAI? = null,
    private val playerGoesFirst: Boolean = true
) : ViewModel() {

    // Estado privado mutable
    private val _gameState = MutableStateFlow(
        // 🔧 FIX: Si la IA juega primero, el turno inicial debe ser YELLOW
        GameState(
            currentPlayer = if (ai != null && !playerGoesFirst) Player.YELLOW else Player.RED
        )
    )

    // Estado público inmutable
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Control de si la IA está pensando
    private val _isAIThinking = MutableStateFlow(false)
    val isAIThinking: StateFlow<Boolean> = _isAIThinking.asStateFlow()

    init {
        // Configurar el modo de juego según si hay IA
        if (ai != null) {
            _gameState.value = _gameState.value.copy(gameMode = GameMode.SINGLE_PLAYER)
        }

        // Si la IA juega primero, hacer el primer movimiento
        if (ai != null && !playerGoesFirst) {
            makeAIMove()
        }
    }

    /**
     * Intenta hacer un movimiento en la columna especificada
     * @param column Número de columna (0-6)
     */
    fun makeMove(column: Int) {
        val currentState = _gameState.value

        // En modo single player, solo permitir movimientos del jugador humano (RED)
        if (currentState.gameMode == GameMode.SINGLE_PLAYER) {
            if (currentState.currentPlayer != Player.RED) {
                return // No permitir clicks durante turno de IA
            }

            if (_isAIThinking.value) {
                return // No permitir clicks mientras IA piensa
            }
        }

        val newState = GameLogic.makeMove(currentState, column)

        if (newState != null) {
            _gameState.value = newState

            // Si es modo single player y no ha terminado el juego, hacer movimiento de IA
            if (newState.gameMode == GameMode.SINGLE_PLAYER &&
                !newState.isGameOver() &&
                newState.currentPlayer == Player.YELLOW &&
                ai != null) {
                makeAIMove()
            }
        }
    }

    /**
     * Hace que la IA realice su movimiento
     */
    private fun makeAIMove() {
        if (ai == null) return

        viewModelScope.launch {
            _isAIThinking.value = true

            // Delay aleatorio para simular "pensamiento"
            val thinkingTime = Random.nextLong(500, 1000)
            delay(thinkingTime)

            val currentState = _gameState.value

            // Obtener el mejor movimiento de la IA
            val bestColumn = ai.getBestMove(currentState)

            // Realizar el movimiento
            val newState = GameLogic.makeMove(currentState, bestColumn)
            if (newState != null) {
                _gameState.value = newState
            }

            _isAIThinking.value = false
        }
    }

    /**
     * Reinicia el juego manteniendo el conteo de victorias
     */
    fun resetGame() {
        val currentState = _gameState.value
        val newState = GameLogic.resetGame(currentState).copy(
            gameMode = currentState.gameMode,
            // 🔧 FIX: Restablecer el turno inicial correcto
            currentPlayer = if (ai != null && !playerGoesFirst) Player.YELLOW else Player.RED
        )

        _gameState.value = newState

        // Si la IA juega primero, hacer el primer movimiento
        if (ai != null && !playerGoesFirst && currentState.gameMode == GameMode.SINGLE_PLAYER) {
            makeAIMove()
        }
    }

    /**
     * Reinicia completamente el juego incluyendo el conteo de victorias
     */
    fun resetAll() {
        val currentMode = _gameState.value.gameMode
        _gameState.value = GameLogic.resetAll().copy(
            gameMode = currentMode,
            // 🔧 FIX: Restablecer el turno inicial correcto
            currentPlayer = if (ai != null && !playerGoesFirst) Player.YELLOW else Player.RED
        )

        // Si la IA juega primero, hacer el primer movimiento
        if (ai != null && !playerGoesFirst && currentMode == GameMode.SINGLE_PLAYER) {
            makeAIMove()
        }
    }

    /**
     * Configura el modo de juego
     */
    fun setGameMode(mode: GameMode) {
        _gameState.value = _gameState.value.copy(gameMode = mode)
    }

    /**
     * Carga un estado de juego guardado
     */
    fun loadGameState(savedState: GameState) {
        _gameState.value = savedState

        // Si es un juego contra IA y es el turno de la IA, hacer el movimiento
        if (savedState.gameMode == GameMode.SINGLE_PLAYER &&
            savedState.currentPlayer == Player.YELLOW &&
            !savedState.isGameOver() &&
            ai != null) {
            makeAIMove()
        }
    }

    /**
     * Obtiene el estado actual del juego
     */
    fun getCurrentState(): GameState = _gameState.value
}