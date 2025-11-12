package com.example.practica4_juegopara2jugadores.model

data class Move(
    val player: Player,
    val column: Int,
    val row: Int,
    val timestamp: Long
)
