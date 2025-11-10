package com.example.practica4_juegopara2jugadores.model

/**
 * Representa un jugador en el juego
 */
enum class Player {
    RED,
    YELLOW;

    fun other(): Player = if (this == RED) YELLOW else RED
}