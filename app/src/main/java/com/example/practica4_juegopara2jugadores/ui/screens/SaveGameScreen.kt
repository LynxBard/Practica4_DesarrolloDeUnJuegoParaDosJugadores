package com.example.practica4_juegopara2jugadores.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.practica4_juegopara2jugadores.data.GameSaveRepository
import com.example.practica4_juegopara2jugadores.model.GameState
import com.example.practica4_juegopara2jugadores.model.SaveFormat
import com.example.practica4_juegopara2jugadores.model.toSaveData
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveGameScreen(
    gameState: GameState,
    repository: GameSaveRepository,
    onDismiss: () -> Unit,
    onSaveSuccess: (String) -> Unit
) {
    var fileName by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf(SaveFormat.JSON) }
    var showPreview by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Generar nombre predeterminado
    LaunchedEffect(Unit) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            .format(Date())
        fileName = "ConnectFour_$dateStr"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Guardar Partida", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección: Nombre del archivo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Nombre del Archivo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = fileName,
                        onValueChange = {
                            fileName = it.filter { char ->
                                char.isLetterOrDigit() || char in listOf('_', '-')
                            }
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Nombre de la partida") },
                        supportingText = {
                            Text(
                                "Solo letras, números, guiones y guiones bajos",
                                fontSize = 12.sp
                            )
                        },
                        isError = errorMessage != null,
                        singleLine = true,
                        trailingIcon = {
                            if (fileName.isNotEmpty()) {
                                IconButton(onClick = { fileName = "" }) {
                                    Icon(Icons.Default.Clear, "Limpiar")
                                }
                            }
                        }
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Sección: Formato de archivo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Formato de Archivo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SaveFormat.values().forEach { format ->
                            FormatOption(
                                format = format,
                                selected = selectedFormat == format,
                                onClick = { selectedFormat = format }
                            )
                        }
                    }
                }
            }

            // Sección: Preview del contenido
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Vista Previa",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { showPreview = !showPreview }) {
                            Icon(
                                if (showPreview) Icons.Default.KeyboardArrowUp
                                else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (showPreview) "Ocultar" else "Mostrar"
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showPreview,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            val previewContent = remember(selectedFormat) {
                                generatePreviewContent(gameState, selectedFormat)
                            }

                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = previewContent,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón de guardar
            Button(
                onClick = {
                    if (fileName.isBlank()) {
                        errorMessage = "El nombre no puede estar vacío"
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        val result = repository.saveGame(
                            gameState = gameState,
                            format = selectedFormat,
                            fileName = "$fileName.${selectedFormat.extension}"
                        )

                        isLoading = false

                        result.fold(
                            onSuccess = { path ->
                                snackbarHostState.showSnackbar(
                                    message = "Partida guardada exitosamente",
                                    duration = SnackbarDuration.Short
                                )
                                onSaveSuccess(path)
                            },
                            onFailure = { error ->
                                errorMessage = error.message ?: "Error desconocido"
                                snackbarHostState.showSnackbar(
                                    message = "Error al guardar: ${error.message}",
                                    duration = SnackbarDuration.Long
                                )
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading && fileName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardando...")
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Guardar Partida",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatOption(
    format: SaveFormat,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val containerColor = when (format) {
        SaveFormat.TXT -> Color(0xFF4CAF50)
        SaveFormat.XML -> Color(0xFFFF9800)
        SaveFormat.JSON -> Color(0xFF2196F3)
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(100.dp)
            .height(80.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                containerColor.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, containerColor)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = format.extension.uppercase(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) containerColor else Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = format.displayName,
                fontSize = 10.sp,
                color = Color.Gray
            )
            if (selected) {
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = containerColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun generatePreviewContent(gameState: GameState, format: SaveFormat): String {
    val saveData = gameState.toSaveData()

    return when (format) {
        SaveFormat.TXT -> buildString {
            appendLine("[METADATA]")
            appendLine("GameMode: ${saveData.gameMode}")
            appendLine("CurrentPlayer: ${saveData.currentPlayer}")
            appendLine("RedWins: ${saveData.redWins}")
            appendLine("YellowWins: ${saveData.yellowWins}")
            appendLine()
            appendLine("[BOARD]")
            saveData.boardState.take(3).forEachIndexed { idx, row ->
                appendLine("Row$idx: ${row.cells.joinToString(",")}")
            }
            appendLine("...")
        }

        SaveFormat.XML -> buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<GameSave>")
            appendLine("  <GameMode>${saveData.gameMode}</GameMode>")
            appendLine("  <CurrentPlayer>${saveData.currentPlayer}</CurrentPlayer>")
            appendLine("  <RedWins>${saveData.redWins}</RedWins>")
            appendLine("  <YellowWins>${saveData.yellowWins}</YellowWins>")
            appendLine("  <BoardState>")
            appendLine("    <Row>")
            appendLine("      <Cells>")
            appendLine("        ...")
            appendLine("      </Cells>")
            appendLine("    </Row>")
            appendLine("  </BoardState>")
            appendLine("</GameSave>")
        }

        SaveFormat.JSON -> buildString {
            appendLine("{")
            appendLine("  \"gameMode\": \"${saveData.gameMode}\",")
            appendLine("  \"currentPlayer\": \"${saveData.currentPlayer}\",")
            appendLine("  \"redWins\": ${saveData.redWins},")
            appendLine("  \"yellowWins\": ${saveData.yellowWins},")
            appendLine("  \"boardState\": [")
            appendLine("    {")
            appendLine("      \"cells\": [...]")
            appendLine("    }")
            appendLine("  ]")
            appendLine("}")
        }
    }
}