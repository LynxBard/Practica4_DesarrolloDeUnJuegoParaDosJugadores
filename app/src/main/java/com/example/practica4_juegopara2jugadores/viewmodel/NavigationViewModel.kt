package com.example.practica4_juegopara2jugadores.viewmodel

import androidx.lifecycle.ViewModel
import com.example.practica4_juegopara2jugadores.domain.ai.Difficulty
import com.example.practica4_juegopara2jugadores.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel que maneja la navegación entre pantallas
 */
class NavigationViewModel : ViewModel() {

    // Estado privado mutable
    private val _currentScreen = MutableStateFlow<Screen>(Screen.MainMenu)

    // Estado público inmutable
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Stack de navegación para el botón "atrás"
    private val navigationStack = mutableListOf<Screen>()

    /**
     * Navega a una nueva pantalla
     * @param screen La pantalla de destino
     */
    fun navigateTo(screen: Screen) {
        // Agregar la pantalla actual al stack antes de navegar
        navigationStack.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    /**
     * Navega a la pantalla de juego single player con configuración
     */
    fun navigateToSinglePlayerWithConfig(difficulty: Difficulty, playerGoesFirst: Boolean) {
        navigationStack.add(_currentScreen.value)
        _currentScreen.value = Screen.SinglePlayerGameWithConfig(difficulty, playerGoesFirst)
    }

    /**
     * Navega hacia atrás en el stack de navegación
     * @return true si se pudo navegar hacia atrás, false si ya estamos en la raíz
     */
    fun navigateBack(): Boolean {
        return if (navigationStack.isNotEmpty()) {
            _currentScreen.value = navigationStack.removeAt(navigationStack.lastIndex)
            true
        } else {
            false
        }
    }

    /**
     * Limpia el stack de navegación y vuelve al menú principal
     */
    fun navigateToMainMenu() {
        navigationStack.clear()
        _currentScreen.value = Screen.MainMenu
    }

    /**
     * Obtiene la pantalla actual
     */
    fun getCurrentScreen(): Screen = _currentScreen.value
}