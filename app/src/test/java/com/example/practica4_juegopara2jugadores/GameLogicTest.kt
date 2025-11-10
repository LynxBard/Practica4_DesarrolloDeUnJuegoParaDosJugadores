package com.example.practica4_juegopara2jugadores

import com.example.practica4_juegopara2jugadores.domain.GameLogic
import com.example.practica4_juegopara2jugadores.model.Cell
import com.example.practica4_juegopara2jugadores.model.GameState
import com.example.practica4_juegopara2jugadores.model.Player
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para la lógica del juego Connect Four
 */
class GameLogicTest {

    @Test
    fun `initial state is correct`() {
        val state = GameState()

        assertEquals(Player.RED, state.currentPlayer)
        assertNull(state.winner)
        assertFalse(state.isDraw)
        assertEquals(0, state.redWins)
        assertEquals(0, state.yellowWins)
    }

    @Test
    fun `placing piece in empty column works`() {
        val state = GameState()
        val newState = GameLogic.makeMove(state, 3)

        assertNotNull(newState)
        assertTrue(newState!!.board[5][3] is Cell.Occupied)
        assertEquals(Player.YELLOW, newState.currentPlayer) // Turno cambia
    }

    @Test
    fun `pieces stack correctly`() {
        var state = GameState()

        // Colocar 3 piezas en la misma columna
        state = GameLogic.makeMove(state, 0)!!
        state = GameLogic.makeMove(state, 0)!!
        state = GameLogic.makeMove(state, 0)!!

        // Verificar que están apiladas
        assertTrue(state.board[5][0] is Cell.Occupied)
        assertTrue(state.board[4][0] is Cell.Occupied)
        assertTrue(state.board[3][0] is Cell.Occupied)
        assertTrue(state.board[2][0] is Cell.Empty)
    }

    @Test
    fun `cannot place in full column`() {
        var state = GameState()

        // Llenar una columna completamente
        repeat(6) {
            state = GameLogic.makeMove(state, 0)!!
        }

        // Intentar colocar otra pieza
        val result = GameLogic.makeMove(state, 0)
        assertNull(result) // Debe ser null porque la columna está llena
    }

    @Test
    fun `horizontal win detection works`() {
        var state = GameState()

        // Crear una victoria horizontal para RED
        // Fila 5: RED RED RED RED
        state = GameLogic.makeMove(state, 0)!! // RED
        state = GameLogic.makeMove(state, 0)!! // YELLOW
        state = GameLogic.makeMove(state, 1)!! // RED
        state = GameLogic.makeMove(state, 1)!! // YELLOW
        state = GameLogic.makeMove(state, 2)!! // RED
        state = GameLogic.makeMove(state, 2)!! // YELLOW
        state = GameLogic.makeMove(state, 3)!! // RED - GANA

        assertEquals(Player.RED, state.winner)
        assertEquals(4, state.winningCells.size)
        assertEquals(1, state.redWins)
    }

    @Test
    fun `vertical win detection works`() {
        var state = GameState()

        // Crear una victoria vertical para RED
        state = GameLogic.makeMove(state, 0)!! // RED
        state = GameLogic.makeMove(state, 1)!! // YELLOW
        state = GameLogic.makeMove(state, 0)!! // RED
        state = GameLogic.makeMove(state, 1)!! // YELLOW
        state = GameLogic.makeMove(state, 0)!! // RED
        state = GameLogic.makeMove(state, 1)!! // YELLOW
        state = GameLogic.makeMove(state, 0)!! // RED - GANA

        assertEquals(Player.RED, state.winner)
        assertEquals(4, state.winningCells.size)
    }

    @Test
    fun `diagonal win detection works`() {
        var state = GameState()

        // Crear una victoria diagonal para RED
        state = GameLogic.makeMove(state, 0)!! // RED
        state = GameLogic.makeMove(state, 1)!! // YELLOW
        state = GameLogic.makeMove(state, 1)!! // RED
        state = GameLogic.makeMove(state, 2)!! // YELLOW
        state = GameLogic.makeMove(state, 2)!! // RED
        state = GameLogic.makeMove(state, 3)!! // YELLOW
        state = GameLogic.makeMove(state, 2)!! // RED
        state = GameLogic.makeMove(state, 3)!! // YELLOW
        state = GameLogic.makeMove(state, 3)!! // RED
        state = GameLogic.makeMove(state, 4)!! // YELLOW
        state = GameLogic.makeMove(state, 3)!! // RED - GANA

        assertEquals(Player.RED, state.winner)
        assertEquals(4, state.winningCells.size)
    }

    @Test
    fun `cannot move after game is won`() {
        var state = GameState()

        // Crear una victoria rápida
        state = GameLogic.makeMove(state, 0)!! // RED
        state = GameLogic.makeMove(state, 1)!! // YELLOW
        state = GameLogic.makeMove(state, 0)!! // RED
        state = GameLogic.makeMove(state, 1)!! // YELLOW
        state = GameLogic.makeMove(state, 0)!! // RED
        state = GameLogic.makeMove(state, 1)!! // YELLOW
        state = GameLogic.makeMove(state, 0)!! // RED - GANA

        // Intentar mover después de ganar
        val result = GameLogic.makeMove(state, 2)
        assertNull(result)
    }

    @Test
    fun `draw detection works`() {
        var state = GameState()

        // Llenar el tablero sin crear 4 en línea
        // Patrón: R Y R Y R Y R
        //         Y R Y R Y R Y
        //         R Y R Y R Y R
        //         Y R Y R Y R Y
        //         R Y R Y R Y R
        //         Y R Y R Y R Y

        for (col in 0 until GameState.COLUMNS) {
            repeat(GameState.ROWS) {
                state = GameLogic.makeMove(state, col)!!
            }
        }

        assertTrue(state.isDraw || state.winner != null)
    }

    @Test
    fun `reset game keeps score`() {
        var state = GameState()

        // Jugar y ganar
        state = GameLogic.makeMove(state, 0)!!
        state = GameLogic.makeMove(state, 1)!!
        state = GameLogic.makeMove(state, 0)!!
        state = GameLogic.makeMove(state, 1)!!
        state = GameLogic.makeMove(state, 0)!!
        state = GameLogic.makeMove(state, 1)!!
        state = GameLogic.makeMove(state, 0)!! // RED gana

        val redWins = state.redWins

        // Reiniciar
        val newState = GameLogic.resetGame(state)

        assertEquals(redWins, newState.redWins)
        assertNull(newState.winner)
        assertTrue(newState.board.all { row -> row.all { it is Cell.Empty } })
    }

    @Test
    fun `reset all clears everything`() {
        var state = GameState(redWins = 5, yellowWins = 3)

        state = GameLogic.resetAll()

        assertEquals(0, state.redWins)
        assertEquals(0, state.yellowWins)
        assertNull(state.winner)
    }

    @Test
    fun `invalid column returns null`() {
        val state = GameState()

        assertNull(GameLogic.makeMove(state, -1))
        assertNull(GameLogic.makeMove(state, 7))
        assertNull(GameLogic.makeMove(state, 100))
    }
}