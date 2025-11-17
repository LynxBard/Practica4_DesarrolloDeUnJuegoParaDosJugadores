package com.example.practica4_juegopara2jugadores.model

import kotlinx.serialization.Serializable
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

/**
 * Datos serializables para guardar el estado completo de una partida
 */
@Serializable
@Root(name = "GameSave", strict = false)
data class GameSaveData(
    @field:Element(name = "Timestamp")
    @get:Element(name = "Timestamp")
    var timestamp: Long = 0L,

    @field:Element(name = "GameMode")
    @get:Element(name = "GameMode")
    var gameMode: String = "",

    @field:ElementList(name = "BoardState", entry = "Row", inline = false)
    @get:ElementList(name = "BoardState", entry = "Row", inline = false)
    var boardState: List<BoardRow> = emptyList(),

    @field:Element(name = "CurrentPlayer")
    @get:Element(name = "CurrentPlayer")
    var currentPlayer: String = "",

    @field:Element(name = "RedWins")
    @get:Element(name = "RedWins")
    var redWins: Int = 0,

    @field:Element(name = "YellowWins")
    @get:Element(name = "YellowWins")
    var yellowWins: Int = 0,

    @field:Element(name = "ElapsedTimeSeconds")
    @get:Element(name = "ElapsedTimeSeconds")
    var elapsedTimeSeconds: Int = 0,

    @field:ElementList(name = "MoveHistory", entry = "Move", inline = false)
    @get:ElementList(name = "MoveHistory", entry = "Move", inline = false)
    var moveHistory: List<SavedMove> = emptyList(),

    @field:Element(name = "Winner", required = false)
    @get:Element(name = "Winner", required = false)
    var winner: String? = null,

    @field:Element(name = "IsDraw")
    @get:Element(name = "IsDraw")
    var isDraw: Boolean = false
) {
    // Constructor sin argumentos requerido por Simple XML
    constructor() : this(0L, "", emptyList(), "", 0, 0, 0, emptyList(), null, false)
}

/**
 * Representa una fila del tablero para serialización XML
 */

@Serializable
@Root(name = "Row", strict = false)
data class BoardRow(
    @field:ElementList(name = "Cells", entry = "Cell", inline = true)
    @get:ElementList(name = "Cells", entry = "Cell", inline = true)
    var cells: List<String> = emptyList()
) {
    constructor() : this(emptyList())
}

/**
 * Representa un movimiento guardado
 */
@Serializable
@Root(name = "Move", strict = false)
data class SavedMove(
    @field:Element(name = "Player")
    @get:Element(name = "Player")
    var player: String = "",

    @field:Element(name = "Column")
    @get:Element(name = "Column")
    var column: Int = 0,

    @field:Element(name = "Row")
    @get:Element(name = "Row")
    var row: Int = 0,

    @field:Element(name = "Timestamp")
    @get:Element(name = "Timestamp")
    var timestamp: Long = 0L
) {
    constructor() : this("", 0, 0, 0L)
}



/**
 * Información resumida de una partida guardada
 */
data class SavedGameInfo(
    val fileName: String,
    val displayName: String,
    val timestamp: Long,
    val gameMode: String,
    val format: SaveFormat,
    val fileSizeBytes: Long
)

/**
 * Formatos de guardado disponibles
 */
enum class SaveFormat(val extension: String, val displayName: String) {
    TXT("txt", "Texto plano"),
    XML("xml", "XML"),
    JSON("json", "JSON")
}

/**
 * Convierte un GameState a GameSaveData
 */
fun GameState.toSaveData(): GameSaveData {
    val boardState = board.map { row ->
        BoardRow(
            cells = row.map { cell ->
                when (cell) {
                    is Cell.Empty -> "EMPTY"
                    is Cell.Occupied -> when (cell.player) {
                        Player.RED -> "RED"
                        Player.YELLOW -> "YELLOW"
                    }
                }
            }
        )
    }

    val savedMoves = moveHistory.map { move ->
        SavedMove(
            player = move.player.name,
            column = move.column,
            row = move.row,
            timestamp = move.timestamp
        )
    }

    return GameSaveData(
        timestamp = System.currentTimeMillis(),
        gameMode = gameMode.name,
        boardState = boardState,
        currentPlayer = currentPlayer.name,
        redWins = redWins,
        yellowWins = yellowWins,
        elapsedTimeSeconds = elapsedTimeSeconds,
        moveHistory = savedMoves,
        winner = winner?.name,
        isDraw = isDraw
    )
}

/**
 * Convierte GameSaveData a GameState
 */
fun GameSaveData.toGameState(): GameState {
    val board = boardState.map { row ->
        row.cells.map { cellStr ->
            when (cellStr) {
                "EMPTY" -> Cell.Empty
                "RED" -> Cell.Occupied(Player.RED)
                "YELLOW" -> Cell.Occupied(Player.YELLOW)
                else -> Cell.Empty
            }
        }
    }

    val moves = moveHistory.map { savedMove ->
        Move(
            player = Player.valueOf(savedMove.player),
            column = savedMove.column,
            row = savedMove.row,
            timestamp = savedMove.timestamp
        )
    }

    return GameState(
        board = board,
        currentPlayer = Player.valueOf(currentPlayer),
        winner = winner?.let { Player.valueOf(it) },
        isDraw = isDraw,
        winningCells = emptyList(),
        redWins = redWins,
        yellowWins = yellowWins,
        lastMove = moves.lastOrNull()?.let { Pair(it.row, it.column) },
        gameMode = GameMode.valueOf(gameMode),
        gameStartTime = timestamp,
        moveHistory = moves,
        elapsedTimeSeconds = elapsedTimeSeconds
    )
}