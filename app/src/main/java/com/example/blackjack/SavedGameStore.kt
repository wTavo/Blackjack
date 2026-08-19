package com.example.blackjack

import android.content.SharedPreferences
import com.example.blackjack.game.SavedGameState
import com.example.blackjack.game.SavedGameStateCodec

object SavedGameStore {
    const val PREFS_NAME = "MyPrefsFile"
    const val KEY_SCORE = "puntuacion"
    const val KEY_RECORD = "record"
    const val KEY_PLAYER_ID = "playerId"
    const val KEY_USERNAME_REGISTERED = "nombreUsuarioRegistrado"
    const val KEY_SAVED_GAME = "partidaGuardada"

    fun hasPendingGame(preferences: SharedPreferences): Boolean =
        !preferences.getString(KEY_SAVED_GAME, null).isNullOrBlank()

    fun save(preferences: SharedPreferences, state: SavedGameState) {
        preferences.edit()
            .putString(KEY_SAVED_GAME, SavedGameStateCodec.encode(state))
            .apply()
    }

    fun load(preferences: SharedPreferences): SavedGameState? =
        preferences.getString(KEY_SAVED_GAME, null)?.let(SavedGameStateCodec::decode)

    fun clear(preferences: SharedPreferences) {
        preferences.edit().remove(KEY_SAVED_GAME).apply()
    }
}
