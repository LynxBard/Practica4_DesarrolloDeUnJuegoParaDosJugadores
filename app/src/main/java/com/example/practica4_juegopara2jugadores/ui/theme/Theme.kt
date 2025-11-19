package com.example.practica4_juegopara2jugadores.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============ Esquemas de Color para Tema Azul ESCOM ============

private val AzulLightColorScheme = lightColorScheme(
    primary = AzulColors.AzulLight,
    secondary = AzulColors.YellowLight,
    tertiary = AzulColors.RedLight,
    background = AzulColors.AzulLightBackground,
    surface = AzulColors.AzulLightSurface,
    onPrimary = AzulColors.AzulLightOnPrimary,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = AzulColors.AzulLightOnBackground,
    onSurface = AzulColors.AzulLightOnBackground
)

private val AzulDarkColorScheme = darkColorScheme(
    primary = AzulColors.AzulDark,
    secondary = AzulColors.YellowDark,
    tertiary = AzulColors.RedDark,
    background = AzulColors.AzulDarkBackground,
    surface = AzulColors.AzulDarkSurface,
    onPrimary = AzulColors.AzulDarkOnPrimary,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = AzulColors.AzulDarkOnBackground,
    onSurface = AzulColors.AzulDarkOnBackground
)

// ============ Esquemas de Color para Tema Guinda IPN ============

private val GuindaLightColorScheme = lightColorScheme(
    primary = GuindaColors.GuindaLight,
    secondary = GuindaColors.YellowLight,
    tertiary = GuindaColors.RedLight,
    background = GuindaColors.GuindaLightBackground,
    surface = GuindaColors.GuindaLightSurface,
    onPrimary = GuindaColors.GuindaLightOnPrimary,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = GuindaColors.GuindaLightOnBackground,
    onSurface = GuindaColors.GuindaLightOnBackground
)

private val GuindaDarkColorScheme = darkColorScheme(
    primary = GuindaColors.GuindaDark,
    secondary = GuindaColors.YellowDark,
    tertiary = GuindaColors.RedDark,
    background = GuindaColors.GuindaDarkBackground,
    surface = GuindaColors.GuindaDarkSurface,
    onPrimary = GuindaColors.GuindaDarkOnPrimary,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = GuindaColors.GuindaDarkOnBackground,
    onSurface = GuindaColors.GuindaDarkOnBackground
)

/**
 * Tema principal de Connect Four con soporte para múltiples temas y modos
 */
@Composable
fun ConnectFourTheme(
    themeType: ThemeType = ThemeType.AZUL_ESCOM,
    colorMode: ColorMode = ColorMode.SYSTEM,
    content: @Composable () -> Unit
) {
    // Determinar si usar modo oscuro
    val useDarkTheme = when (colorMode) {
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
        ColorMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Seleccionar el esquema de color según el tema y el modo
    val colorScheme = when (themeType) {
        ThemeType.AZUL_ESCOM -> {
            if (useDarkTheme) AzulDarkColorScheme else AzulLightColorScheme
        }
        ThemeType.GUINDA_IPN -> {
            if (useDarkTheme) GuindaDarkColorScheme else GuindaLightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}