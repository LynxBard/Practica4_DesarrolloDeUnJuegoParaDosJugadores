package com.example.practica4_juegopara2jugadores.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import kotlinx.serialization.Serializable

/**
 * Datos serializables para guardar el estado completo de una partida
 */
@Serializable
@JacksonXmlRootElement(localName = "GameSave")
data class GameSaveData(
    @JsonProperty("timestamp")
    @JacksonXmlProperty(localName = "Timestamp")
    val timestamp: Long,

    @JsonProperty("gameMode")
    @JacksonXmlProperty(localName = "GameMode")
    val gameMode: String,

    @JsonProperty("boardState")
    @JacksonXmlElementWrapper(localName = "BoardState")
    @JacksonXmlProperty(localName = "Row")
    val boardState: List<List<String>>,

    @JsonProperty("currentPlayer")
    @JacksonXmlProperty(localName = "CurrentPlayer")
    val currentPlayer: String,

    @JsonProperty("redWins")
    @JacksonXmlProperty(localName = "RedWins")
    val redWins: Int,

    @JsonProperty("yellowWins")
    @JacksonXmlProperty(localName = "YellowWins")
    val yellowWins: Int,

    @JsonProperty("elapsedTimeSeconds")
    @JacksonXmlProperty(localName = "ElapsedTimeSeconds")
    val elapsedTimeSeconds: Int,

    @JsonProperty("moveHistory")
    @JacksonXmlElementWrapper(localName = "MoveHistory")
    @JacksonXmlProperty(localName = "Move")
    val moveHistory: List<SavedMove> = emptyList(),

    @JsonProperty("winner")
    @JacksonXmlProperty(localName = "Winner")
    val winner: String? = null,

    @JsonProperty("isDraw")
    @JacksonXmlProperty(localName = "IsDraw")
    val isDraw: Boolean = false
)

/**
 * Representa un movimiento guardado
 */
@Serializable
@JacksonXmlRootElement(localName = "Move")
data class SavedMove(
    @JsonProperty("player")
    @JacksonXmlProperty(localName = "Player")
    val player: String,

    @JsonProperty("column")
    @JacksonXmlProperty(localName = "Column")
    val column: Int,

    @JsonProperty("row")
    @JacksonXmlProperty(localName = "Row")
    val row: Int,

    @JsonProperty("timestamp")
    @JacksonXmlProperty(localName = "Timestamp")
    val timestamp: Long
)

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
        row.map { cell ->
            when (cell) {
                is Cell.Empty -> "EMPTY"
                is Cell.Occupied -> when (cell.player) {
                    Player.RED -> "RED"
                    Player.YELLOW -> "YELLOW"
                }
            }
        }
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
        row.map { cellStr ->
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
        winningCells = emptyList(), // Se recalculará si es necesario
        redWins = redWins,
        yellowWins = yellowWins,
        lastMove = moves.lastOrNull()?.let { Pair(it.row, it.column) },
        gameMode = GameMode.valueOf(gameMode),
        gameStartTime = timestamp,
        moveHistory = moves,
        elapsedTimeSeconds = elapsedTimeSeconds
    )
}