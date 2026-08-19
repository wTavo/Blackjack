package com.example.blackjack.game

class Deck private constructor(private val cards: MutableList<Card>) {
    fun draw(): Card {
        check(cards.isNotEmpty()) { "No quedan cartas en la baraja" }
        return cards.removeAt(cards.lastIndex)
    }

    fun remaining(): Int = cards.size

    fun snapshot(): List<Card> = cards.toList()

    companion object {
        fun standard(): Deck {
            val cards = Suit.entries.flatMap { suit ->
                Rank.entries.map { rank -> Card(rank, suit) }
            }.toMutableList()
            cards.shuffle()
            return Deck(cards)
        }

        fun from(cards: List<Card>): Deck = Deck(cards.toMutableList())
    }
}
