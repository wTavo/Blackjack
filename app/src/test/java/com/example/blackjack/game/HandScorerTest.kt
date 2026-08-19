package com.example.blackjack.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandScorerTest {
    @Test
    fun scoresNumberCardsNormally() {
        val score = HandScorer.score(listOf(Card(Rank.SEVEN, Suit.HEARTS), Card(Rank.FOUR, Suit.SPADES)))

        assertEquals(11, score.best)
        assertEquals(11, score.alternative)
        assertFalse(score.isBust)
        assertFalse(score.isBlackjack)
    }

    @Test
    fun scoresFaceCardsAsTen() {
        val score = HandScorer.score(listOf(Card(Rank.JACK, Suit.CLUBS), Card(Rank.QUEEN, Suit.DIAMONDS)))

        assertEquals(20, score.best)
    }

    @Test
    fun scoresAnAceAsElevenWhenItDoesNotBust() {
        val score = HandScorer.score(listOf(Card(Rank.ACE, Suit.HEARTS), Card(Rank.SIX, Suit.SPADES)))

        assertEquals(17, score.best)
        assertEquals(7, score.alternative)
    }

    @Test
    fun changesAceToOneWhenElevenWouldBust() {
        val score = HandScorer.score(
            listOf(Card(Rank.ACE, Suit.HEARTS), Card(Rank.KING, Suit.SPADES), Card(Rank.FIVE, Suit.CLUBS))
        )

        assertEquals(16, score.best)
        assertEquals(16, score.alternative)
        assertFalse(score.isBust)
    }

    @Test
    fun scoresTwoAcesWithoutGoingOverTwentyOne() {
        val score = HandScorer.score(listOf(Card(Rank.ACE, Suit.HEARTS), Card(Rank.ACE, Suit.SPADES)))

        assertEquals(12, score.best)
        assertEquals(2, score.alternative)
    }

    @Test
    fun identifiesNaturalBlackjack() {
        val score = HandScorer.score(listOf(Card(Rank.ACE, Suit.HEARTS), Card(Rank.KING, Suit.SPADES)))

        assertEquals(21, score.best)
        assertTrue(score.isBlackjack)
    }

    @Test
    fun identifiesBust() {
        val score = HandScorer.score(
            listOf(Card(Rank.KING, Suit.HEARTS), Card(Rank.QUEEN, Suit.SPADES), Card(Rank.FIVE, Suit.CLUBS))
        )

        assertEquals(25, score.best)
        assertTrue(score.isBust)
        assertFalse(score.isBlackjack)
    }
}
