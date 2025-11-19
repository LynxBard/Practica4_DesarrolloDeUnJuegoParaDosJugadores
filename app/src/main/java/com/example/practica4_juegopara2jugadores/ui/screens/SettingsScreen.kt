package com.example.practica4_juegopara2jugadores.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.practica4_juegopara2jugadores.data.ThemePreferencesRepository
import com.example.practica4_juegopara2jugadores.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentThemeConfig: ThemeConfig,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ThemePreferencesRepository(context) }

    var selectedThemeType by remember { mutableStateOf(currentThemeConfig.themeType) }
    var selectedColorMode by remember { mutableStateOf(currentThemeConfig.colorMode) }

    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        contentVisible = true
    }

    // Guardar cambios automáticamente
    LaunchedEffect(selectedThemeType, selectedColorMode) {
        val newConfig = ThemeConfig(selectedThemeType, selectedColorMode)
        scope.launch {
            repository.setThemeConfig(newConfig)
            onThemeConfigChange(newConfig)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Configuración",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(animationSpec = tween(400)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(400)
                        )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Título decorativo
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "🎨",
                            fontSize = 48.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Personaliza tu experiencia",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Card de Selección de Tema
                    ThemeSelectionCard(
                        selectedTheme = selectedThemeType,
                        onThemeSelected = { selectedThemeType = it }
                    )

                    // Card de Modo de Color
                    ColorModeCard(
                        selectedMode = selectedColorMode,
                        onModeSelected = { selectedColorMode = it }
                    )

                    // Vista previa de colores
                    ColorPreviewCard(
                        themeType = selectedThemeType,
                        colorMode = selectedColorMode
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Información adicional
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Los cambios se aplican inmediatamente",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectionCard(
    selectedTheme: ThemeType,
    onThemeSelected: (ThemeType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "🏛️",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "Tema de la Aplicación",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOption(
                    theme = ThemeType.GUINDA_IPN,
                    label = "Guinda IPN",
                    description = "Color representativo del IPN",
                    icon = "🎓",
                    previewColor = GuindaColors.GuindaLight,
                    selected = selectedTheme == ThemeType.GUINDA_IPN,
                    onClick = { onThemeSelected(ThemeType.GUINDA_IPN) }
                )

                ThemeOption(
                    theme = ThemeType.AZUL_ESCOM,
                    label = "Azul ESCOM",
                    description = "Color representativo de la ESCOM",
                    icon = "💻",
                    previewColor = AzulColors.AzulLight,
                    selected = selectedTheme == ThemeType.AZUL_ESCOM,
                    onClick = { onThemeSelected(ThemeType.AZUL_ESCOM) }
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    theme: ThemeType,
    label: String,
    description: String,
    icon: String,
    previewColor: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "theme_scale"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                previewColor.copy(alpha = 0.1f)
            } else {
                Color(0xFFF5F5F5)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono
            Text(
                text = icon,
                fontSize = 28.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            // Color preview
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(previewColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Texto
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) previewColor else Color.Black
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            // Radio button
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = previewColor
                )
            )
        }
    }
}

@Composable
private fun ColorModeCard(
    selectedMode: ColorMode,
    onModeSelected: (ColorMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "🌓",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "Modo de Color",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ColorModeOption(
                    mode = ColorMode.SYSTEM,
                    label = "Automático",
                    description = "Sigue la configuración del sistema",
                    icon = Icons.Default.PhoneAndroid,
                    selected = selectedMode == ColorMode.SYSTEM,
                    onClick = { onModeSelected(ColorMode.SYSTEM) }
                )

                ColorModeOption(
                    mode = ColorMode.LIGHT,
                    label = "Claro",
                    description = "Siempre usar modo claro",
                    icon = Icons.Default.LightMode,
                    selected = selectedMode == ColorMode.LIGHT,
                    onClick = { onModeSelected(ColorMode.LIGHT) }
                )

                ColorModeOption(
                    mode = ColorMode.DARK,
                    label = "Oscuro",
                    description = "Siempre usar modo oscuro",
                    icon = Icons.Default.DarkMode,
                    selected = selectedMode == ColorMode.DARK,
                    onClick = { onModeSelected(ColorMode.DARK) }
                )
            }
        }
    }
}

@Composable
private fun ColorModeOption(
    mode: ColorMode,
    label: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "mode_scale"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                Color(0xFFF5F5F5)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Black
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun ColorPreviewCard(
    themeType: ThemeType,
    colorMode: ColorMode
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "🎨",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "Vista Previa de Colores",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Mostrar colores del tema actual
                ColorCircle(
                    color = MaterialTheme.colorScheme.primary,
                    label = "Principal"
                )
                ColorCircle(
                    color = MaterialTheme.colorScheme.secondary,
                    label = "Secundario"
                )
                ColorCircle(
                    color = MaterialTheme.colorScheme.tertiary,
                    label = "Terciario"
                )
                ColorCircle(
                    color = MaterialTheme.colorScheme.surface,
                    label = "Superficie"
                )
            }
        }
    }
}

@Composable
private fun ColorCircle(
    color: Color,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}