package com.example.practica4_juegopara2jugadores.domain.ai

import com.example.practica4_juegopara2jugadores.model.Cell
import com.example.practica4_juegopara2jugadores.model.GameState
import com.example.practica4_juegopara2jugadores.model.Player
import kotlin.math.max
import kotlin.math.min

/**
 * Niveles de dificultad de la IA
 */
enum class Difficulty(val depth: Int) {
    EASY(2),
    MEDIUM(4),
    HARD(6)
}

/**
 * Implementación de IA para Connect Four usando Minimax con poda Alpha-Beta
 */
class ConnectFourAI(
    private val difficulty: Difficulty = Difficulty.MEDIUM,
    private val aiPlayer: Player = Player.YELLOW
) {
    private val humanPlayer = aiPlayer.other()

    companion object {
        private const val WIN_SCORE = 100_000
        private const val THREE_IN_ROW = 100
        private const val TWO_IN_ROW = 10
        private const val CENTER_BONUS = 3
    }

    /**
     * Obtiene el mejor movimiento para la IA
     */
    fun getBestMove(state: GameState): Int {
        var bestMove = 3 // Default: columna central
        var bestScore = Int.MIN_VALUE
        val alpha = Int.MIN_VALUE
        val beta = Int.MAX_VALUE

        // Evaluar cada columna posible
        for (col in 0 until GameState.COLUMNS) {
            if (!state.isColumnFull(col)) {
                val newState = simulateMove(state, col, aiPlayer)
                val score = minimax(newState, difficulty.depth - 1, alpha, beta, false)

                if (score > bestScore) {
                    bestScore = score
                    bestMove = col
                }
            }
        }

        return bestMove
    }

    /**
     * Algoritmo Minimax con poda Alpha-Beta
     */
    private fun minimax(
        state: GameState,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean
    ): Int {
        // Casos base
        if (depth == 0 || state.isGameOver()) {
            return evaluateBoard(state)
        }

        var currentAlpha = alpha
        var currentBeta = beta

        if (isMaximizing) {
            var maxScore = Int.MIN_VALUE

            for (col in 0 until GameState.COLUMNS) {
                if (!state.isColumnFull(col)) {
                    val newState = simulateMove(state, col, aiPlayer)
                    val score = minimax(newState, depth - 1, currentAlpha, currentBeta, false)
                    maxScore = max(maxScore, score)
                    currentAlpha = max(currentAlpha, score)

                    // Poda Beta
                    if (currentBeta <= currentAlpha) {
                        break
                    }
                }
            }
            return maxScore

        } else {
            var minScore = Int.MAX_VALUE

            for (col in 0 until GameState.COLUMNS) {
                if (!state.isColumnFull(col)) {
                    val newState = simulateMove(state, col, humanPlayer)
                    val score = minimax(newState, depth - 1, currentAlpha, currentBeta, true)
                    minScore = min(minScore, score)
                    currentBeta = min(currentBeta, score)

                    // Poda Alpha
                    if (currentBeta <= currentAlpha) {
                        break
                    }
                }
            }
            return minScore
        }
    }

    /**
     * Evalúa el tablero desde la perspectiva de la IA
     */
    private fun evaluateBoard(state: GameState): Int {
        // Victoria/Derrota
        when (state.winner) {
            aiPlayer -> return WIN_SCORE
            humanPlayer -> return -WIN_SCORE
            null -> {}
            else -> {}
        }

        if (state.isDraw) return 0

        var score = 0

        // Evaluar filas
        for (row in 0 until GameState.ROWS) {
            for (col in 0 until GameState.COLUMNS - 3) {
                score += evaluateWindow(
                    state.board[row][col],
                    state.board[row][col + 1],
                    state.board[row][col + 2],
                    state.board[row][col + 3]
                )
            }
        }

        // Evaluar columnas
        for (col in 0 until GameState.COLUMNS) {
            for (row in 0 until GameState.ROWS - 3) {
                score += evaluateWindow(
                    state.board[row][col],
                    state.board[row + 1][col],
                    state.board[row + 2][col],
                    state.board[row + 3][col]
                )
            }
        }

        // Evaluar diagonales (/)
        for (row in 3 until GameState.ROWS) {
            for (col in 0 until GameState.COLUMNS - 3) {
                score += evaluateWindow(
                    state.board[row][col],
                    state.board[row - 1][col + 1],
                    state.board[row - 2][col + 2],
                    state.board[row - 3][col + 3]
                )
            }
        }

        // Evaluar diagonales (\)
        for (row in 0 until GameState.ROWS - 3) {
            for (col in 0 until GameState.COLUMNS - 3) {
                score += evaluateWindow(
                    state.board[row][col],
                    state.board[row + 1][col + 1],
                    state.board[row + 2][col + 2],
                    state.board[row + 3][col + 3]
                )
            }
        }

        // Bonus por control del centro
        val centerColumn = GameState.COLUMNS / 2
        for (row in 0 until GameState.ROWS) {
            if (state.board[row][centerColumn] is Cell.Occupied) {
                val cell = state.board[row][centerColumn] as Cell.Occupied
                if (cell.player == aiPlayer) {
                    score += CENTER_BONUS
                }
            }
        }

        return score
    }

    /**
     * Evalúa una ventana de 4 celdas consecutivas
     */
    private fun evaluateWindow(c1: Cell, c2: Cell, c3: Cell, c4: Cell): Int {
        val cells = listOf(c1, c2, c3, c4)

        var aiCount = 0
        var humanCount = 0
        var emptyCount = 0

        for (cell in cells) {
            when (cell) {
                is Cell.Occupied -> {
                    if (cell.player == aiPlayer) aiCount++
                    else humanCount++
                }
                is Cell.Empty -> emptyCount++
            }
        }

        // No mezclar fichas de ambos jugadores
        if (aiCount > 0 && humanCount > 0) return 0

        // Evaluar patrones de la IA
        if (aiCount > 0) {
            return when (aiCount) {
                4 -> WIN_SCORE
                3 -> if (emptyCount == 1) THREE_IN_ROW else 0
                2 -> if (emptyCount == 2) TWO_IN_ROW else 0
                else -> 0
            }
        }

        // Evaluar patrones del humano (para bloquear)
        if (humanCount > 0) {
            return when (humanCount) {
                4 -> -WIN_SCORE
                3 -> if (emptyCount == 1) -THREE_IN_ROW else 0
                2 -> if (emptyCount == 2) -TWO_IN_ROW else 0
                else -> 0
            }
        }

        return 0
    }

    /**
     * Simula un movimiento sin modificar el estado original
     */
    private fun simulateMove(state: GameState, column: Int, player: Player): GameState {
        val row = state.getLowestAvailableRow(column) ?: return state

        val newBoard = state.board.mapIndexed { r, rowList ->
            if (r == row) {
                rowList.mapIndexed { c, cell ->
                    if (c == column) Cell.Occupied(player) else cell
                }
            } else {
                rowList
            }
        }

        var newState = state.copy(
            board = newBoard,
            lastMove = Pair(row, column),
            currentPlayer = player.other()
        )

        // Verificar victoria
        val winningCells = checkWinSimple(newBoard, row, column, player)
        if (winningCells.isNotEmpty()) {
            newState = newState.copy(
                winner = player,
                winningCells = winningCells
            )
        } else if (newState.isBoardFull()) {
            newState = newState.copy(isDraw = true)
        }

        return newState
    }

    /**
     * Verificación rápida de victoria (sin actualizar puntuación)
     */
    private fun checkWinSimple(
        board: List<List<Cell>>,
        row: Int,
        col: Int,
        player: Player
    ): List<Pair<Int, Int>> {
        val directions = listOf(
            Pair(0, 1),  // Horizontal
            Pair(1, 0),  // Vertical
            Pair(1, 1),  // Diagonal \
            Pair(1, -1)  // Diagonal /
        )

        for (direction in directions) {
            val cells = checkDirectionSimple(board, row, col, direction.first, direction.second, player)
            if (cells.size >= GameState.WIN_LENGTH) {
                return cells
            }
        }

        return emptyList()
    }

    private fun checkDirectionSimple(
        board: List<List<Cell>>,
        startRow: Int,
        startCol: Int,
        deltaRow: Int,
        deltaCol: Int,
        player: Player
    ): List<Pair<Int, Int>> {
        val cells = mutableListOf<Pair<Int, Int>>()
        var row = startRow
        var col = startCol

        // Ir hacia atrás
        while (isValidPosition(row - deltaRow, col - deltaCol) &&
            board[row - deltaRow][col - deltaCol] is Cell.Occupied &&
            (board[row - deltaRow][col - deltaCol] as Cell.Occupied).player == player) {
            row -= deltaRow
            col -= deltaCol
        }

        // Contar hacia adelante
        while (isValidPosition(row, col) &&
            board[row][col] is Cell.Occupied &&
            (board[row][col] as Cell.Occupied).player == player) {
            cells.add(Pair(row, col))
            row += deltaRow
            col += deltaCol
        }

        return cells
    }

    private fun isValidPosition(row: Int, col: Int): Boolean {
        return row in 0 until GameState.ROWS && col in 0 until GameState.COLUMNS
    }
}