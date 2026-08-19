package com.example.blackjack

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordPolicyTest {
    @Test
    fun identifiesARecordOnlyWhenScoreIsHigher() {
        assertTrue(isNewRecord(4, 3))
        assertFalse(isNewRecord(3, 3))
        assertFalse(isNewRecord(2, 3))
    }
}
