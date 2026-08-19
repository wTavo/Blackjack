package com.example.blackjack.game

data class SavedGameState(
    val playerCards: List<Int>,
    val dealerCards: List<Int>,
    val hiddenDealerCard: Int,
    val remainingCards: List<Int>,
    val score: Int,
    val phase: String
)

object SavedGameStateCodec {
    fun encode(state: SavedGameState): String {
        return listOf(
            state.phase,
            state.score.toString(),
            state.hiddenDealerCard.toString(),
            state.playerCards.joinToString(","),
            state.dealerCards.joinToString(","),
            state.remainingCards.joinToString(",")
        ).joinToString("|")
    }

    fun decode(value: String): SavedGameState? {
        val fields = value.split("|", limit = 6)
        if (fields.size != 6 || fields[0].isBlank()) return null

        return try {
            SavedGameState(
                playerCards = parseCards(fields[3]),
                dealerCards = parseCards(fields[4]),
                hiddenDealerCard = fields[2].toInt(),
                remainingCards = parseCards(fields[5]),
                score = fields[1].toInt().takeIf { it >= 0 } ?: return null,
                phase = fields[0]
            )
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun parseCards(value: String): List<Int> {
        if (value.isBlank()) return emptyList()
        return value.split(",").map { it.toInt() }
    }
}
