package com.example.blackjack

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerProfileTest {
    @Test
    fun asksForNameOnlyWhenScoreIsPositiveAndNameIsMissing() {
        assertFalse(shouldRequestUsername(null, 0))
        assertTrue(shouldRequestUsername(null, 1))
        assertTrue(shouldRequestUsername("", 1))
        assertFalse(shouldRequestUsername("Jugador", 10))
    }

    @Test
    fun doesNotAskWhenRemotePlayerProfileAlreadyExists() {
        assertFalse(shouldRequestUsernameAfterRemoteLookup(10, remoteProfileExists = true))
        assertTrue(shouldRequestUsernameAfterRemoteLookup(10, remoteProfileExists = false))
        assertFalse(shouldRequestUsernameAfterRemoteLookup(0, remoteProfileExists = false))
    }

    @Test
    fun onlyConfirmsRegistrationAfterSuccessfulRemoteWrite() {
        assertTrue(registrationStatusAfterWrite(remoteNewRecord = true, writeSucceeded = true) == true)
        assertFalse(registrationStatusAfterWrite(remoteNewRecord = false, writeSucceeded = true) == true)
        assertNull(registrationStatusAfterWrite(remoteNewRecord = true, writeSucceeded = false))
    }

    @Test
    fun highlightsOnlyTheScoreWithTheLocalPlayerId() {
        assertTrue(isCurrentPlayer("player-1", "player-1"))
        assertFalse(isCurrentPlayer("player-2", "player-1"))
        assertFalse(isCurrentPlayer(null, "player-1"))
    }
}
