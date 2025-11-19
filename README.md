# 🎮 Connect Four - Juego Multiplataforma Android

Una implementación moderna y completa del clásico juego Conecta Cuatro desarrollada en Kotlin con Jetpack Compose.

## ✨ Características Principales

### 🎯 Múltiples Modos de Juego

- **Multijugador Local:** Juega contra un amigo en el mismo dispositivo
- **Un Jugador vs IA:** Enfrenta a una inteligencia artificial con diferentes niveles de dificultad
- **Multijugador Bluetooth:** Conecta con otros dispositivos cercanos para jugar de forma inalámbrica

### 🤖 Inteligencia Artificial Avanzada

- Implementación del algoritmo **Minimax con poda Alpha-Beta**
- Tres niveles de dificultad: Fácil, Medio, Difícil
- Función de evaluación heurística inteligente

### 💾 Sistema de Persistencia

- Guardado y carga de partidas
- Historial de partidas jugadas
- Estadísticas detalladas de victorias y derrotas
- Sistema de logros y desbloqueos

### 🎨 Interfaz Moderna

- Desarrollada con **Jetpack Compose**
- Animaciones fluidas y atractivas
- Múltiples temas visuales
- Diseño responsivo para tablets y teléfonos
- Soporte para modo oscuro

## 🛠️ Tecnologías Utilizadas

- **Kotlin:** Lenguaje de programación principal
- **Jetpack Compose:** Framework moderno para UI declarativa
- **Coroutines & Flow:** Para operaciones asíncronas y gestión de estado
- **Room Database:** Persistencia de datos local
- **Bluetooth API:** Conectividad entre dispositivos
- **Material Design 3:** Componentes y directrices de diseño

## 📋 Requisitos

- **Android Studio:** Hedgehog o superior
- **SDK mínimo:** Android 8.0 (API 26)
- **SDK objetivo:** Android 14 (API 34)
- **Kotlin:** 1.9.0 o superior

## 🚀 Instalación

1. Clona el repositorio:

```bash
git clone https://github.com/LynxBard/Practica4_DesarrolloDeUnJuegoParaDosJugadores.git
cd connect-four
```

1. Abre el proyecto en Android Studio
2. Sincroniza las dependencias de Gradle
3. Ejecuta la aplicación en un emulador o dispositivo físico

## 🎮 Cómo Jugar

1. Selecciona tu modo de juego preferido desde el menú principal
2. Los jugadores alternan turnos dejando caer fichas en las columnas
3. El objetivo es conectar cuatro fichas del mismo color en línea (horizontal, vertical o diagonal)
4. El primer jugador en lograr cuatro en línea gana la partida

## 📁 Estructura del Proyecto

```
app/
├── model/
│   ├── GameMode.kt          # Enumeración de modos de juego
│   ├── GameState.kt         # Estado del juego
│   ├── Player.kt            # Representación de jugadores
│   └── BoardTheme.kt        # Temas visuales
├── viewmodel/
│   ├── GameViewModel.kt     # Lógica de juego principal
│   ├── AIViewModel.kt       # Implementación de IA
│   └── BluetoothViewModel.kt # Gestión de conexiones
├── ui/
│   ├── screens/
│   │   ├── MenuScreen.kt    # Pantalla principal
│   │   ├── GameScreen.kt    # Pantalla de juego
│   │   └── SettingsScreen.kt
│   └── components/
│       ├── GameBoard.kt     # Tablero de juego
│       └── PlayerIndicator.kt
├── data/
│   ├── repository/
│   │   └── GameRepository.kt
│   └── database/
│       ├── GameDatabase.kt
│       └── GameDao.kt
└── bluetooth/
    ├── BluetoothService.kt
    └── BluetoothManager.kt
```

## 🧠 Algoritmo de IA

La IA utiliza el algoritmo **Minimax con poda Alpha-Beta** para tomar decisiones óptimas:

- **Minimax:** Explora el árbol de posibles jugadas buscando maximizar las posibilidades de victoria
- **Poda Alpha-Beta:** Optimiza el algoritmo descartando ramas que no pueden mejorar el resultado
- **Función de Evaluación:** Asigna valores a las posiciones del tablero basándose en patrones estratégicos


## 📝 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo `LICENSE` para más detalles.

## 👨‍💻 Autor

**Carlos David González Sánchez**

## 📧 Contacto

Para preguntas, sugerencias o reportar bugs, por favor abre un issue en GitHub.

*Desarrollado con ❤️ usando Kotlin y Jetpack Compose*
