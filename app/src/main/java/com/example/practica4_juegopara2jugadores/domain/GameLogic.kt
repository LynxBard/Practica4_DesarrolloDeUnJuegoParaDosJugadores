package com.example.practica4_juegopara2jugadores.domain

import com.example.practica4_juegopara2jugadores.model.Cell
import com.example.practica4_juegopara2jugadores.model.GameState
import com.example.practica4_juegopara2jugadores.model.GameState.Companion.COLUMNS
import com.example.practica4_juegopara2jugadores.model.GameState.Companion.ROWS
import com.example.practica4_juegopara2jugadores.model.GameState.Companion.WIN_LENGTH
import com.example.practica4_juegopara2jugadores.model.Player

/**
 * Contiene toda la lógica del juego Connect Four
 */
object GameLogic {

    /**
     * Intenta hacer un movimiento en la columna especificada
     * Retorna el nuevo estado del juego si el movimiento es válido, null si no lo es
     */
    fun makeMove(state: GameState, column: Int): GameState? {
        // Validar que el juego no haya terminado
        if (state.isGameOver()) return null

        // Validar que la columna esté en rango
        if (column !in 0 until COLUMNS) return null

        // Validar que la columna no esté llena
        if (state.isColumnFull(column)) return null

        // Encontrar la fila más baja disponible
        val row = state.getLowestAvailableRow(column) ?: return null

        // Crear el nuevo tablero con la ficha colocada
        val newBoard = state.board.mapIndexed { r, rowList ->
            if (r == row) {
                rowList.mapIndexed { c, cell ->
                    if (c == column) Cell.Occupied(state.currentPlayer) else cell
                }
            } else {
                rowList
            }
        }

        // Crear el nuevo estado
        var newState = state.copy(
            board = newBoard,
            lastMove = Pair(row, column)
        )

        // Verificar si hay un ganador
        val winningCells = checkWinner(newBoard, row, column, state.currentPlayer)
        if (winningCells.isNotEmpty()) {
            newState = newState.copy(
                winner = state.currentPlayer,
                winningCells = winningCells,
                redWins = if (state.currentPlayer == Player.RED) state.redWins + 1 else state.redWins,
                yellowWins = if (state.currentPlayer == Player.YELLOW) state.yellowWins + 1 else state.yellowWins
            )
        } else if (newState.isBoardFull()) {
            // Verificar empate
            newState = newState.copy(isDraw = true)
        } else {
            // Cambiar de turno
            newState = newState.copy(currentPlayer = state.currentPlayer.other())
        }

        return newState
    }

    /**
     * Verifica si hay un ganador después del último movimiento
     * Retorna la lista de celdas ganadoras si hay un ganador, lista vacía si no
     */
    private fun checkWinner(
        board: List<List<Cell>>,
        row: Int,
        col: Int,
        player: Player
    ): List<Pair<Int, Int>> {
        // Direcciones: horizontal, vertical, diagonal \, diagonal /
        val directions = listOf(
            Pair(0, 1),  // Horizontal
            Pair(1, 0),  // Vertical
            Pair(1, 1),  // Diagonal \
            Pair(1, -1)  // Diagonal /
        )

        for (direction in directions) {
            val cells = checkDirection(board, row, col, direction.first, direction.second, player)
            if (cells.size >= WIN_LENGTH) {
                return cells
            }
        }

        return emptyList()
    }

    /**
     * Verifica una dirección específica para encontrar fichas consecutivas
     */
    private fun checkDirection(
        board: List<List<Cell>>,
        startRow: Int,
        startCol: Int,
        deltaRow: Int,
        deltaCol: Int,
        player: Player
    ): List<Pair<Int, Int>> {
        val cells = mutableListOf<Pair<Int, Int>>()

        // Buscar en ambas direcciones desde la posición inicial
        var row = startRow
        var col = startCol

        // Ir hacia atrás primero
        while (isValidPosition(row - deltaRow, col - deltaCol) &&
            board[row - deltaRow][col - deltaCol] is Cell.Occupied &&
            (board[row - deltaRow][col - deltaCol] as Cell.Occupied).player == player) {
            row -= deltaRow
            col -= deltaCol
        }

        // Ahora ir hacia adelante y contar
        while (isValidPosition(row, col) &&
            board[row][col] is Cell.Occupied &&
            (board[row][col] as Cell.Occupied).player == player) {
            cells.add(Pair(row, col))
            row += deltaRow
            col += deltaCol
        }

        return cells
    }

    /**
     * Verifica si una posición es válida en el tablero
     */
    private fun isValidPosition(row: Int, col: Int): Boolean {
        return row in 0 until ROWS && col in 0 until COLUMNS
    }

    /**
     * Reinicia el juego manteniendo el conteo de victorias
     */
    fun resetGame(currentState: GameState): GameState {
        return GameState(
            redWins = currentState.redWins,
            yellowWins = currentState.yellowWins
        )
    }

    /**
     * Reinicia completamente el juego incluyendo el conteo
     */
    fun resetAll(): GameState {
        return GameState()
    }
}