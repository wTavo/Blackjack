package com.example.blackjack.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedGameStateTest {
    @Test
    fun encodesAndDecodesACompleteGameState() {
        val original = SavedGameState(
            playerCards = listOf(1, 2),
            dealerCards = listOf(3, 4),
            hiddenDealerCard = 5,
            remainingCards = listOf(6, 7, 8),
            score = 3,
            phase = "PLAYER_TURN"
        )

        assertEquals(original, SavedGameStateCodec.decode(SavedGameStateCodec.encode(original)))
    }

    @Test
    fun rejectsMalformedSavedState() {
        assertNull(SavedGameStateCodec.decode("not-a-game-state"))
    }

    @Test
    fun preservesAnEmptyCardList() {
        val original = SavedGameState(emptyList(), emptyList(), 0, emptyList(), 1, "PLAYER_TURN")

        assertEquals(original, SavedGameStateCodec.decode(SavedGameStateCodec.encode(original)))
    }
}
