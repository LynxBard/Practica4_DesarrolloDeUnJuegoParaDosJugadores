package com.example.practica4_juegopara2jugadores.model

/**
 * Representa el contenido de una celda en el tablero
 */
sealed class Cell {
    object Empty : Cell()
    data class Occupied(val player: Player) : Cell()
}