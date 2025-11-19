package com.example.practica4_juegopara2jugadores.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tipos de tema disponibles
 */
enum class ThemeType {
    GUINDA_IPN,    // Tema guinda del IPN
    AZUL_ESCOM     // Tema azul de la ESCOM
}

/**
 * Modo de color
 */
enum class ColorMode {
    LIGHT,      // Modo claro
    DARK,       // Modo oscuro
    SYSTEM      // Seguir el sistema
}

/**
 * Configuración del tema
 */
data class ThemeConfig(
    val themeType: ThemeType = ThemeType.AZUL_ESCOM,
    val colorMode: ColorMode = ColorMode.SYSTEM
)

/**
 * Colores para el tema Guinda IPN
 */
object GuindaColors {
    // Modo Claro
    val GuindaLight = Color(0xFF9F2241)      // Guinda principal claro
    val GuindaLightVariant = Color(0xFFB8304F) // Guinda más claro
    val GuindaLightBackground = Color(0xFFFFF5F7) // Fondo muy claro
    val GuindaLightSurface = Color(0xFFFFFFFF)
    val GuindaLightOnPrimary = Color.White
    val GuindaLightOnBackground = Color(0xFF1C1B1F)

    // Modo Oscuro
    val GuindaDark = Color(0xFFD4516D)        // Guinda más suave para oscuro
    val GuindaDarkVariant = Color(0xFFE57A91) // Guinda aún más suave
    val GuindaDarkBackground = Color(0xFF1A0D10) // Fondo muy oscuro con tinte guinda
    val GuindaDarkSurface = Color(0xFF2D1820)    // Surface con tinte guinda
    val GuindaDarkOnPrimary = Color(0xFF1C1B1F)
    val GuindaDarkOnBackground = Color(0xFFE6E1E5)

    // Colores secundarios (amarillo para las fichas)
    val YellowLight = Color(0xFFFDD835)
    val YellowDark = Color(0xFFFFF176)

    // Color terciario (para fichas rojas)
    val RedLight = Color(0xFFE53935)
    val RedDark = Color(0xFFEF5350)
}

/**
 * Colores para el tema Azul ESCOM
 */
object AzulColors {
    // Modo Claro
    val AzulLight = Color(0xFF1976D2)         // Azul principal claro
    val AzulLightVariant = Color(0xFF1E88E5)  // Azul más claro
    val AzulLightBackground = Color(0xFFF5F9FF) // Fondo muy claro
    val AzulLightSurface = Color(0xFFFFFFFF)
    val AzulLightOnPrimary = Color.White
    val AzulLightOnBackground = Color(0xFF1C1B1F)

    // Modo Oscuro
    val AzulDark = Color(0xFF64B5F6)          // Azul más suave para oscuro
    val AzulDarkVariant = Color(0xFF90CAF9)   // Azul aún más suave
    val AzulDarkBackground = Color(0xFF0A1929) // Fondo muy oscuro con tinte azul
    val AzulDarkSurface = Color(0xFF1E3A5F)   // Surface con tinte azul
    val AzulDarkOnPrimary = Color(0xFF1C1B1F)
    val AzulDarkOnBackground = Color(0xFFE6E1E5)

    // Colores secundarios (amarillo para las fichas)
    val YellowLight = Color(0xFFFDD835)
    val YellowDark = Color(0xFFFFF176)

    // Color terciario (para fichas rojas)
    val RedLight = Color(0xFFE53935)
    val RedDark = Color(0xFFEF5350)
}