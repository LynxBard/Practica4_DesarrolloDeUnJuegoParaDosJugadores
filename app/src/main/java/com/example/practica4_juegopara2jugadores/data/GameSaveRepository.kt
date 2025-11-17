package com.example.practica4_juegopara2jugadores.data

import android.content.Context
import com.example.practica4_juegopara2jugadores.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.simpleframework.xml.core.Persister
import java.io.File
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repositorio para gestionar el guardado y carga de partidas
 */
class GameSaveRepository(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val xmlSerializer = Persister()

    companion object {
        private const val SAVE_DIRECTORY = "SavedGames"
        private const val DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss"
    }

    /**
     * Obtiene el directorio de guardados
     */
    fun getSaveDirectory(): File {
        val dir = File(context.filesDir, SAVE_DIRECTORY)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Guarda una partida en el formato especificado
     */
    suspend fun saveGame(
        gameState: GameState,
        format: SaveFormat,
        fileName: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val saveData = gameState.toSaveData()
            val finalFileName = fileName ?: generateFileName(format)
            val file = File(getSaveDirectory(), finalFileName)

            val content = when (format) {
                SaveFormat.TXT -> serializeToTxt(saveData)
                SaveFormat.XML -> serializeToXml(saveData)
                SaveFormat.JSON -> serializeToJson(saveData)
            }

            file.writeText(content)
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Error al guardar partida: ${e.message}", e))
        }
    }

    /**
     * Carga una partida desde un archivo
     */
    suspend fun loadGame(file: File, format: SaveFormat): Result<GameSaveData> =
        withContext(Dispatchers.IO) {
            try {
                if (!file.exists()) {
                    return@withContext Result.failure(Exception("El archivo no existe"))
                }

                val content = file.readText()
                val saveData = when (format) {
                    SaveFormat.TXT -> deserializeFromTxt(content)
                    SaveFormat.XML -> deserializeFromXml(content)
                    SaveFormat.JSON -> deserializeFromJson(content)
                }

                Result.success(saveData)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(Exception("Error al cargar partida: ${e.message}", e))
            }
        }

    /**
     * Lista todas las partidas guardadas
     */
    suspend fun listSavedGames(): List<SavedGameInfo> = withContext(Dispatchers.IO) {
        try {
            val saveDir = getSaveDirectory()
            val files = saveDir.listFiles() ?: return@withContext emptyList()

            files.mapNotNull { file ->
                try {
                    val format = when (file.extension) {
                        "txt" -> SaveFormat.TXT
                        "xml" -> SaveFormat.XML
                        "json" -> SaveFormat.JSON
                        else -> return@mapNotNull null
                    }

                    val content = file.readText()
                    val timestamp = extractTimestamp(content, format)
                    val gameMode = extractGameMode(content, format)

                    SavedGameInfo(
                        fileName = file.name,
                        displayName = formatDisplayName(file.name, timestamp),
                        timestamp = timestamp,
                        gameMode = gameMode,
                        format = format,
                        fileSizeBytes = file.length()
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Elimina una partida guardada
     */
    suspend fun deleteGame(fileName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(getSaveDirectory(), fileName)
            val deleted = file.delete()
            Result.success(deleted)
        } catch (e: Exception) {
            Result.failure(Exception("Error al eliminar partida: ${e.message}", e))
        }
    }

    // ==================== Serialización TXT ====================

    private fun serializeToTxt(data: GameSaveData): String {
        val sb = StringBuilder()

        sb.appendLine("[METADATA]")
        sb.appendLine("Timestamp: ${data.timestamp}")
        sb.appendLine("Date: ${formatDate(data.timestamp)}")
        sb.appendLine("GameMode: ${data.gameMode}")
        sb.appendLine("CurrentPlayer: ${data.currentPlayer}")
        sb.appendLine("RedWins: ${data.redWins}")
        sb.appendLine("YellowWins: ${data.yellowWins}")
        sb.appendLine("ElapsedTimeSeconds: ${data.elapsedTimeSeconds}")
        sb.appendLine("Winner: ${data.winner ?: "NONE"}")
        sb.appendLine("IsDraw: ${data.isDraw}")
        sb.appendLine()

        sb.appendLine("[BOARD]")
        data.boardState.forEachIndexed { rowIndex, row ->
            sb.append("Row$rowIndex: ")
            sb.appendLine(row.cells.joinToString(","))
        }
        sb.appendLine()

        sb.appendLine("[MOVES]")
        sb.appendLine("TotalMoves: ${data.moveHistory.size}")
        data.moveHistory.forEachIndexed { index, move ->
            sb.appendLine("Move${index + 1}: ${move.player},${move.column},${move.row},${move.timestamp}")
        }

        return sb.toString()
    }

    private fun deserializeFromTxt(content: String): GameSaveData {
        val lines = content.lines()
        val sections = mutableMapOf<String, List<String>>()
        var currentSection = ""

        lines.forEach { line ->
            when {
                line.startsWith("[") && line.endsWith("]") -> {
                    currentSection = line.substring(1, line.length - 1)
                    sections[currentSection] = mutableListOf()
                }
                line.isNotBlank() -> {
                    val list = sections[currentSection] as? MutableList ?: mutableListOf()
                    list.add(line)
                    sections[currentSection] = list
                }
            }
        }

        val metadata = sections["METADATA"]?.associate { line ->
            val parts = line.split(": ", limit = 2)
            parts[0] to parts.getOrNull(1)
        } ?: emptyMap()

        val boardState = sections["BOARD"]?.map { line ->
            val cells = line.substringAfter(": ").split(",")
            BoardRow(cells)
        } ?: emptyList()

        val moveHistory = sections["MOVES"]?.filter { it.startsWith("Move") }?.map { line ->
            val parts = line.substringAfter(": ").split(",")
            SavedMove(
                player = parts[0],
                column = parts[1].toInt(),
                row = parts[2].toInt(),
                timestamp = parts[3].toLong()
            )
        } ?: emptyList()

        return GameSaveData(
            timestamp = metadata["Timestamp"]?.toLongOrNull() ?: 0L,
            gameMode = metadata["GameMode"] ?: "LOCAL_MULTIPLAYER",
            boardState = boardState,
            currentPlayer = metadata["CurrentPlayer"] ?: "RED",
            redWins = metadata["RedWins"]?.toIntOrNull() ?: 0,
            yellowWins = metadata["YellowWins"]?.toIntOrNull() ?: 0,
            elapsedTimeSeconds = metadata["ElapsedTimeSeconds"]?.toIntOrNull() ?: 0,
            moveHistory = moveHistory,
            winner = metadata["Winner"]?.takeIf { it != "NONE" },
            isDraw = metadata["IsDraw"]?.toBoolean() ?: false
        )
    }

    // ==================== Serialización XML ====================

    private fun serializeToXml(data: GameSaveData): String {
        val writer = StringWriter()
        xmlSerializer.write(data, writer)
        return writer.toString()
    }

    private fun deserializeFromXml(content: String): GameSaveData {
        return xmlSerializer.read(GameSaveData::class.java, content)
    }

    // ==================== Serialización JSON ====================

    private fun serializeToJson(data: GameSaveData): String {
        return json.encodeToString(data)
    }

    private fun deserializeFromJson(content: String): GameSaveData {
        return json.decodeFromString<GameSaveData>(content)
    }

    // ==================== Utilidades ====================

    private fun generateFileName(format: SaveFormat): String {
        val dateStr = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            .format(Date())
        return "ConnectFour_$dateStr.${format.extension}"
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))
    }

    private fun formatDisplayName(fileName: String, timestamp: Long): String {
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(timestamp))
        return fileName.substringBeforeLast(".") + " ($date)"
    }

    private fun extractTimestamp(content: String, format: SaveFormat): Long {
        return try {
            when (format) {
                SaveFormat.TXT -> {
                    val line = content.lines().find { it.startsWith("Timestamp:") }
                    line?.substringAfter(":")?.trim()?.toLongOrNull() ?: 0L
                }
                SaveFormat.XML -> {
                    val regex = """<Timestamp>(\d+)</Timestamp>""".toRegex()
                    regex.find(content)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                }
                SaveFormat.JSON -> {
                    val regex = """"timestamp"\s*:\s*(\d+)""".toRegex()
                    regex.find(content)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                }
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun extractGameMode(content: String, format: SaveFormat): String {
        return try {
            when (format) {
                SaveFormat.TXT -> {
                    val line = content.lines().find { it.startsWith("GameMode:") }
                    line?.substringAfter(":")?.trim() ?: "UNKNOWN"
                }
                SaveFormat.XML -> {
                    val regex = """<GameMode>([^<]+)</GameMode>""".toRegex()
                    regex.find(content)?.groupValues?.get(1) ?: "UNKNOWN"
                }
                SaveFormat.JSON -> {
                    val regex = """"gameMode"\s*:\s*"([^"]+)"""".toRegex()
                    regex.find(content)?.groupValues?.get(1) ?: "UNKNOWN"
                }
            }
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }
}