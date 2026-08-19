package com.example.blackjack.game

data class HandScore(
    val best: Int,
    val alternative: Int,
    val isBust: Boolean,
    val isBlackjack: Boolean
)

object HandScorer {
    fun score(cards: List<Card>): HandScore {
        val alternative = cards.sumOf { it.rank.points }
        val aces = cards.count { it.rank == Rank.ACE }
        val best = if (aces > 0 && alternative + 10 <= 21) alternative + 10 else alternative

        return HandScore(
            best = best,
            alternative = alternative,
            isBust = best > 21,
            isBlackjack = cards.size == 2 && best == 21
        )
    }
}
