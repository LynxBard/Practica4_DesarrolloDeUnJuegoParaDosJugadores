package com.example.practica4_juegopara2jugadores.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.practica4_juegopara2jugadores.ui.theme.ColorMode
import com.example.practica4_juegopara2jugadores.ui.theme.ThemeConfig
import com.example.practica4_juegopara2jugadores.ui.theme.ThemeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension para crear DataStore
private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

/**
 * Repositorio para gestionar las preferencias del tema
 */
class ThemePreferencesRepository(private val context: Context) {

    private companion object {
        val THEME_TYPE = stringPreferencesKey("theme_type")
        val COLOR_MODE = stringPreferencesKey("color_mode")
    }

    /**
     * Flow que emite la configuración actual del tema
     */
    val themeConfigFlow: Flow<ThemeConfig> = context.themeDataStore.data.map { preferences ->
        val themeTypeString = preferences[THEME_TYPE] ?: ThemeType.AZUL_ESCOM.name
        val colorModeString = preferences[COLOR_MODE] ?: ColorMode.SYSTEM.name

        ThemeConfig(
            themeType = try {
                ThemeType.valueOf(themeTypeString)
            } catch (e: IllegalArgumentException) {
                ThemeType.AZUL_ESCOM
            },
            colorMode = try {
                ColorMode.valueOf(colorModeString)
            } catch (e: IllegalArgumentException) {
                ColorMode.SYSTEM
            }
        )
    }

    /**
     * Actualiza el tipo de tema
     */
    suspend fun setThemeType(themeType: ThemeType) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_TYPE] = themeType.name
        }
    }

    /**
     * Actualiza el modo de color
     */
    suspend fun setColorMode(colorMode: ColorMode) {
        context.themeDataStore.edit { preferences ->
            preferences[COLOR_MODE] = colorMode.name
        }
    }

    /**
     * Actualiza toda la configuración del tema
     */
    suspend fun setThemeConfig(config: ThemeConfig) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_TYPE] = config.themeType.name
            preferences[COLOR_MODE] = config.colorMode.name
        }
    }

    /**
     * Obtiene la configuración actual del tema de forma suspendida
     */
    suspend fun getCurrentThemeConfig(): ThemeConfig {
        var config = ThemeConfig()
        context.themeDataStore.data.collect { preferences ->
            val themeTypeString = preferences[THEME_TYPE] ?: ThemeType.AZUL_ESCOM.name
            val colorModeString = preferences[COLOR_MODE] ?: ColorMode.SYSTEM.name

            config = ThemeConfig(
                themeType = try {
                    ThemeType.valueOf(themeTypeString)
                } catch (e: IllegalArgumentException) {
                    ThemeType.AZUL_ESCOM
                },
                colorMode = try {
                    ColorMode.valueOf(colorModeString)
                } catch (e: IllegalArgumentException) {
                    ColorMode.SYSTEM
                }
            )
        }
        return config
    }
}