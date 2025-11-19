package com.example.practica4_juegopara2jugadores.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.practica4_juegopara2jugadores.model.GameMode
import com.example.practica4_juegopara2jugadores.model.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension para crear DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_statistics")

/**
 * Data class para las estadísticas del juego
 */
data class GameStatistics(
    // Partidas jugadas por modo
    val localGamesPlayed: Int = 0,
    val singlePlayerGamesPlayed: Int = 0,
    val bluetoothGamesPlayed: Int = 0,

    // Victorias totales
    val redWinsTotal: Int = 0,
    val yellowWinsTotal: Int = 0,

    // Empates
    val drawsTotal: Int = 0,

    // Tiempo promedio (en segundos)
    val averageGameTime: Int = 0,
    val totalGameTime: Int = 0,

    // Racha actual
    val currentStreak: Int = 0,
    val lastWinner: String? = null,

    // Mejor racha
    val bestStreak: Int = 0,

    // Victorias contra IA por dificultad
    val easyAIWins: Int = 0,
    val mediumAIWins: Int = 0,
    val hardAIWins: Int = 0,

    // Derrotas contra IA
    val easyAILosses: Int = 0,
    val mediumAILosses: Int = 0,
    val hardAILosses: Int = 0
)

/**
 * Repositorio para gestionar estadísticas persistentes del juego
 */
class StatisticsRepository(private val context: Context) {

    private companion object {
        // Keys para DataStore
        val LOCAL_GAMES = intPreferencesKey("local_games_played")
        val SINGLE_PLAYER_GAMES = intPreferencesKey("single_player_games_played")
        val BLUETOOTH_GAMES = intPreferencesKey("bluetooth_games_played")

        val RED_WINS = intPreferencesKey("red_wins_total")
        val YELLOW_WINS = intPreferencesKey("yellow_wins_total")
        val DRAWS = intPreferencesKey("draws_total")

        val AVERAGE_TIME = intPreferencesKey("average_game_time")
        val TOTAL_TIME = intPreferencesKey("total_game_time")

        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val LAST_WINNER = stringPreferencesKey("last_winner")
        val BEST_STREAK = intPreferencesKey("best_streak")

        val EASY_AI_WINS = intPreferencesKey("easy_ai_wins")
        val MEDIUM_AI_WINS = intPreferencesKey("medium_ai_wins")
        val HARD_AI_WINS = intPreferencesKey("hard_ai_wins")

        val EASY_AI_LOSSES = intPreferencesKey("easy_ai_losses")
        val MEDIUM_AI_LOSSES = intPreferencesKey("medium_ai_losses")
        val HARD_AI_LOSSES = intPreferencesKey("hard_ai_losses")
    }

    /**
     * Flow que emite las estadísticas actuales
     */
    val statisticsFlow: Flow<GameStatistics> = context.dataStore.data.map { preferences ->
        GameStatistics(
            localGamesPlayed = preferences[LOCAL_GAMES] ?: 0,
            singlePlayerGamesPlayed = preferences[SINGLE_PLAYER_GAMES] ?: 0,
            bluetoothGamesPlayed = preferences[BLUETOOTH_GAMES] ?: 0,

            redWinsTotal = preferences[RED_WINS] ?: 0,
            yellowWinsTotal = preferences[YELLOW_WINS] ?: 0,
            drawsTotal = preferences[DRAWS] ?: 0,

            averageGameTime = preferences[AVERAGE_TIME] ?: 0,
            totalGameTime = preferences[TOTAL_TIME] ?: 0,

            currentStreak = preferences[CURRENT_STREAK] ?: 0,
            lastWinner = preferences[LAST_WINNER],
            bestStreak = preferences[BEST_STREAK] ?: 0,

            easyAIWins = preferences[EASY_AI_WINS] ?: 0,
            mediumAIWins = preferences[MEDIUM_AI_WINS] ?: 0,
            hardAIWins = preferences[HARD_AI_WINS] ?: 0,

            easyAILosses = preferences[EASY_AI_LOSSES] ?: 0,
            mediumAILosses = preferences[MEDIUM_AI_LOSSES] ?: 0,
            hardAILosses = preferences[HARD_AI_LOSSES] ?: 0
        )
    }

    /**
     * Actualiza las estadísticas después de un juego
     */
    suspend fun updateAfterGame(
        gameMode: GameMode,
        winner: Player?,
        isDraw: Boolean,
        gameTimeSeconds: Int,
        aiDifficulty: String? = null
    ) {
        context.dataStore.edit { preferences ->
            // Incrementar contador de partidas por modo
            when (gameMode) {
                GameMode.LOCAL_MULTIPLAYER -> {
                    preferences[LOCAL_GAMES] = (preferences[LOCAL_GAMES] ?: 0) + 1
                }
                GameMode.SINGLE_PLAYER -> {
                    preferences[SINGLE_PLAYER_GAMES] = (preferences[SINGLE_PLAYER_GAMES] ?: 0) + 1
                }
                GameMode.BLUETOOTH_MULTIPLAYER -> {
                    preferences[BLUETOOTH_GAMES] = (preferences[BLUETOOTH_GAMES] ?: 0) + 1
                }
            }

            // Actualizar victorias o empates
            if (isDraw) {
                preferences[DRAWS] = (preferences[DRAWS] ?: 0) + 1
            } else if (winner != null) {
                when (winner) {
                    Player.RED -> {
                        preferences[RED_WINS] = (preferences[RED_WINS] ?: 0) + 1

                        // Si es contra IA y gana el jugador (RED)
                        if (gameMode == GameMode.SINGLE_PLAYER && aiDifficulty != null) {
                            when (aiDifficulty.uppercase()) {
                                "EASY" -> preferences[EASY_AI_WINS] = (preferences[EASY_AI_WINS] ?: 0) + 1
                                "MEDIUM" -> preferences[MEDIUM_AI_WINS] = (preferences[MEDIUM_AI_WINS] ?: 0) + 1
                                "HARD" -> preferences[HARD_AI_WINS] = (preferences[HARD_AI_WINS] ?: 0) + 1
                            }
                        }
                    }
                    Player.YELLOW -> {
                        preferences[YELLOW_WINS] = (preferences[YELLOW_WINS] ?: 0) + 1

                        // Si es contra IA y pierde el jugador (gana IA/YELLOW)
                        if (gameMode == GameMode.SINGLE_PLAYER && aiDifficulty != null) {
                            when (aiDifficulty.uppercase()) {
                                "EASY" -> preferences[EASY_AI_LOSSES] = (preferences[EASY_AI_LOSSES] ?: 0) + 1
                                "MEDIUM" -> preferences[MEDIUM_AI_LOSSES] = (preferences[MEDIUM_AI_LOSSES] ?: 0) + 1
                                "HARD" -> preferences[HARD_AI_LOSSES] = (preferences[HARD_AI_LOSSES] ?: 0) + 1
                            }
                        }
                    }
                }

                // Actualizar racha
                val lastWinner = preferences[LAST_WINNER]
                val currentStreak = preferences[CURRENT_STREAK] ?: 0

                if (lastWinner == winner.name) {
                    // Continúa la racha
                    val newStreak = currentStreak + 1
                    preferences[CURRENT_STREAK] = newStreak

                    // Actualizar mejor racha si es necesario
                    val bestStreak = preferences[BEST_STREAK] ?: 0
                    if (newStreak > bestStreak) {
                        preferences[BEST_STREAK] = newStreak
                    }
                } else {
                    // Nueva racha
                    preferences[CURRENT_STREAK] = 1
                    preferences[LAST_WINNER] = winner.name
                }
            } else {
                // Empate rompe la racha
                preferences[CURRENT_STREAK] = 0
                preferences.remove(LAST_WINNER)
            }

            // Actualizar tiempo promedio
            val totalTime = (preferences[TOTAL_TIME] ?: 0) + gameTimeSeconds
            preferences[TOTAL_TIME] = totalTime

            val totalGames = (preferences[LOCAL_GAMES] ?: 0) +
                    (preferences[SINGLE_PLAYER_GAMES] ?: 0) +
                    (preferences[BLUETOOTH_GAMES] ?: 0)

            if (totalGames > 0) {
                preferences[AVERAGE_TIME] = totalTime / totalGames
            }
        }
    }

    /**
     * Reinicia todas las estadísticas
     */
    suspend fun resetAllStatistics() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Obtiene las estadísticas actuales de forma suspendida
     */
    suspend fun getCurrentStatistics(): GameStatistics {
        var stats = GameStatistics()
        context.dataStore.data.collect { preferences ->
            stats = GameStatistics(
                localGamesPlayed = preferences[LOCAL_GAMES] ?: 0,
                singlePlayerGamesPlayed = preferences[SINGLE_PLAYER_GAMES] ?: 0,
                bluetoothGamesPlayed = preferences[BLUETOOTH_GAMES] ?: 0,

                redWinsTotal = preferences[RED_WINS] ?: 0,
                yellowWinsTotal = preferences[YELLOW_WINS] ?: 0,
                drawsTotal = preferences[DRAWS] ?: 0,

                averageGameTime = preferences[AVERAGE_TIME] ?: 0,
                totalGameTime = preferences[TOTAL_TIME] ?: 0,

                currentStreak = preferences[CURRENT_STREAK] ?: 0,
                lastWinner = preferences[LAST_WINNER],
                bestStreak = preferences[BEST_STREAK] ?: 0,

                easyAIWins = preferences[EASY_AI_WINS] ?: 0,
                mediumAIWins = preferences[MEDIUM_AI_WINS] ?: 0,
                hardAIWins = preferences[HARD_AI_WINS] ?: 0,

                easyAILosses = preferences[EASY_AI_LOSSES] ?: 0,
                mediumAILosses = preferences[MEDIUM_AI_LOSSES] ?: 0,
                hardAILosses = preferences[HARD_AI_LOSSES] ?: 0
            )
        }
        return stats
    }
}