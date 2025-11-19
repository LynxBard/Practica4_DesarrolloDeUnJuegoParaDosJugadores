package com.example.practica4_juegopara2jugadores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.practica4_juegopara2jugadores.domain.ai.ConnectFourAI
import com.example.practica4_juegopara2jugadores.domain.GameLogic
import com.example.practica4_juegopara2jugadores.model.GameMode
import com.example.practica4_juegopara2jugadores.model.GameState
import com.example.practica4_juegopara2jugadores.model.Player
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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
        GameState(
            currentPlayer = if (ai != null && !playerGoesFirst) Player.YELLOW else Player.RED
        )
    )

    // Estado público inmutable
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Control de si la IA está pensando
    private val _isAIThinking = MutableStateFlow(false)
    val isAIThinking: StateFlow<Boolean> = _isAIThinking.asStateFlow()

    // Job del temporizador
    private var timerJob: Job? = null

    // Estado del temporizador
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    init {
        // Configurar el modo de juego según si hay IA
        if (ai != null) {
            _gameState.value = _gameState.value.copy(gameMode = GameMode.SINGLE_PLAYER)
        }

        // Iniciar el temporizador
        startTimer()

        // Si la IA juega primero, hacer el primer movimiento
        if (ai != null && !playerGoesFirst) {
            makeAIMove()
        }
    }

    /**
     * Inicia el temporizador del juego
     */
    fun startTimer() {
        if (_isTimerRunning.value) return

        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (isActive && !_gameState.value.isGameOver()) {
                delay(1000) // 1 segundo
                _gameState.value = _gameState.value.copy(
                    elapsedTimeSeconds = _gameState.value.elapsedTimeSeconds + 1
                )
            }
            _isTimerRunning.value = false
        }
    }

    /**
     * Pausa el temporizador
     */
    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _isTimerRunning.value = false
    }

    /**
     * Reanuda el temporizador
     */
    fun resumeTimer() {
        if (!_gameState.value.isGameOver()) {
            startTimer()
        }
    }

    /**
     * Formatea el tiempo transcurrido a formato MM:SS
     */
    fun formatElapsedTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
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

            // Pausar el temporizador si el juego terminó
            if (newState.isGameOver()) {
                pauseTimer()
            }

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

                // Pausar el temporizador si el juego terminó
                if (newState.isGameOver()) {
                    pauseTimer()
                }
            }

            _isAIThinking.value = false
        }
    }

    /**
     * Reinicia el juego manteniendo el conteo de victorias
     */
    fun resetGame() {
        pauseTimer()

        val currentState = _gameState.value
        val newState = GameLogic.resetGame(currentState).copy(
            gameMode = currentState.gameMode,
            currentPlayer = if (ai != null && !playerGoesFirst) Player.YELLOW else Player.RED,
            elapsedTimeSeconds = 0 // Reiniciar tiempo
        )

        _gameState.value = newState
        startTimer() // Reiniciar temporizador

        // Si la IA juega primero, hacer el primer movimiento
        if (ai != null && !playerGoesFirst && currentState.gameMode == GameMode.SINGLE_PLAYER) {
            makeAIMove()
        }
    }

    /**
     * Reinicia completamente el juego incluyendo el conteo de victorias
     */
    fun resetAll() {
        pauseTimer()

        val currentMode = _gameState.value.gameMode
        _gameState.value = GameLogic.resetAll().copy(
            gameMode = currentMode,
            currentPlayer = if (ai != null && !playerGoesFirst) Player.YELLOW else Player.RED,
            elapsedTimeSeconds = 0
        )

        startTimer()

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
        pauseTimer()
        _gameState.value = savedState
        startTimer()

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

    override fun onCleared() {
        super.onCleared()
        pauseTimer()
    }
}