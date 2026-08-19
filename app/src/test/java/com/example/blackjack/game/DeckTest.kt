package com.example.blackjack.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DeckTest {
    @Test
    fun standardDeckStartsWithFiftyTwoCards() {
        assertEquals(52, Deck.standard().remaining())
    }

    @Test
    fun drawingRemovesOneCardFromTheDeck() {
        val deck = Deck.standard()

        val first = deck.draw()

        assertEquals(51, deck.remaining())
        assertNotEquals(null, first)
    }

    @Test
    fun drawingTheFullDeckProducesNoDuplicates() {
        val deck = Deck.standard()
        val cards = (1..52).map { deck.draw() }

        assertEquals(52, cards.toSet().size)
        assertEquals(0, deck.remaining())
    }

    @Test
    fun reconstructsTheRemainingCardsWithoutShufflingThem() {
        val original = Deck.standard()
        val first = original.draw()
        val restored = Deck.from(original.snapshot())

        assertEquals(original.snapshot(), restored.snapshot())
        assertEquals(first, Deck.from(listOf(first)).draw())
    }
}
