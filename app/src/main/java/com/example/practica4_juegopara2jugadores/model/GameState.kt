package com.example.practica4_juegopara2jugadores.model

/**
 * Representa el estado actual del juego
 */
data class GameState(
    val board: List<List<Cell>> = List(ROWS) { List(COLUMNS) { Cell.Empty } },
    val currentPlayer: Player = Player.RED,
    val winner: Player? = null,
    val isDraw: Boolean = false,
    val winningCells: List<Pair<Int, Int>> = emptyList(),
    val redWins: Int = 0,
    val yellowWins: Int = 0,
    val lastMove: Pair<Int, Int>? = null,
    val gameMode: GameMode = GameMode.LOCAL_MULTIPLAYER,
    val gameStartTime: Long = System.currentTimeMillis(),
    val moveHistory: List<Move> = emptyList(),
    val elapsedTimeSeconds: Int = 0
) {
    companion object {
        const val ROWS = 6
        const val COLUMNS = 7
        const val WIN_LENGTH = 4
    }

    /**
     * Verifica si el juego ha terminado
     */
    fun isGameOver(): Boolean = winner != null || isDraw

    /**
     * Obtiene el número de la columna más baja disponible
     */
    fun getLowestAvailableRow(column: Int): Int? {
        for (row in ROWS - 1 downTo 0) {
            if (board[row][column] is Cell.Empty) {
                return row
            }
        }
        return null
    }

    /**
     * Verifica si una columna está llena
     */
    fun isColumnFull(column: Int): Boolean {
        return board[0][column] is Cell.Occupied
    }

    /**
     * Verifica si el tablero está completamente lleno
     */
    fun isBoardFull(): Boolean {
        return board[0].all { it is Cell.Occupied }
    }
}