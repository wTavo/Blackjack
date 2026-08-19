package com.example.blackjack.game

import org.junit.Assert.assertEquals
import org.junit.Test

class GameRulesTest {
    @Test
    fun playerWinsWhenDealerBusts() {
        val player = HandScorer.score(cards(Rank.TEN, Rank.SIX))
        val dealer = HandScorer.score(cards(Rank.KING, Rank.QUEEN, Rank.FIVE))

        assertEquals(GameResult.PLAYER_WIN, GameRules.resolve(player, dealer))
    }

    @Test
    fun playerLosesWhenPlayerBusts() {
        val player = HandScorer.score(cards(Rank.KING, Rank.QUEEN, Rank.FIVE))
        val dealer = HandScorer.score(cards(Rank.TEN, Rank.SIX))

        assertEquals(GameResult.DEALER_WIN, GameRules.resolve(player, dealer))
    }

    @Test
    fun naturalBlackjackBeatsNormalTwentyOne() {
        val player = HandScorer.score(cards(Rank.ACE, Rank.KING))
        val dealer = HandScorer.score(cards(Rank.SEVEN, Rank.SEVEN, Rank.SEVEN))

        assertEquals(GameResult.PLAYER_WIN, GameRules.resolve(player, dealer))
    }

    @Test
    fun twoNaturalBlackjacksAreDraw() {
        val player = HandScorer.score(cards(Rank.ACE, Rank.KING))
        val dealer = HandScorer.score(cards(Rank.ACE, Rank.QUEEN))

        assertEquals(GameResult.DRAW, GameRules.resolve(player, dealer))
    }

    @Test
    fun dealerNaturalBlackjackBeatsNormalTwentyOne() {
        val player = HandScorer.score(cards(Rank.SEVEN, Rank.SEVEN, Rank.SEVEN))
        val dealer = HandScorer.score(cards(Rank.ACE, Rank.KING))

        assertEquals(GameResult.DEALER_WIN, GameRules.resolve(player, dealer))
    }

    @Test
    fun equalNormalScoresAreDraw() {
        val player = HandScorer.score(cards(Rank.TEN, Rank.SIX))
        val dealer = HandScorer.score(cards(Rank.NINE, Rank.SEVEN))

        assertEquals(GameResult.DRAW, GameRules.resolve(player, dealer))
    }

    private fun cards(vararg ranks: Rank): List<Card> =
        ranks.mapIndexed { index, rank -> Card(rank, Suit.entries[index % Suit.entries.size]) }
}
