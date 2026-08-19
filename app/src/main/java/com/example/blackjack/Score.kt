package com.example.blackjack

// Declaración de una clase de datos llamada Score para el tablero de puntuaciones
data class Score(
    val nombreUsuario: String?,
    val puntuacion: Long,
    val playerId: String? = null
)

fun isCurrentPlayer(scorePlayerId: String?, localPlayerId: String?): Boolean =
    !scorePlayerId.isNullOrBlank() && scorePlayerId == localPlayerId
