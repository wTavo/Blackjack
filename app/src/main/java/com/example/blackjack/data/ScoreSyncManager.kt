package com.example.blackjack.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import com.example.blackjack.FirebaseAvailability
import com.example.blackjack.R
import com.example.blackjack.SavedGameStore
import com.example.blackjack.isNewRecord
import com.example.blackjack.registrationStatusAfterWrite
import java.util.UUID

// Administrador centralizado de puntuaciones, récords y sincronización con Firebase Firestore
object ScoreSyncManager {
    const val KEY_USERNAME = "nombreUsuario"
    const val KEY_PENDING_RECORD_SYNC = "recordPendienteSincronizar"
    private const val KEY_SCORE_FORMAT_V2 = "puntuacionFormatoV2"

    // Obtiene el identificador único del jugador o crea uno nuevo de forma persistente
    fun getOrCreatePlayerId(preferences: SharedPreferences): String =
        preferences.getString(SavedGameStore.KEY_PLAYER_ID, null)
            ?: UUID.randomUUID().toString().also {
                preferences.edit { putString(SavedGameStore.KEY_PLAYER_ID, it) }
            }

    // Carga la puntuación de la sesión actual
    fun loadScore(preferences: SharedPreferences): Int {
        return if (!preferences.getBoolean(KEY_SCORE_FORMAT_V2, false)) {
            preferences.edit {
                putBoolean(KEY_SCORE_FORMAT_V2, true)
                putInt(SavedGameStore.KEY_SCORE, 0)
            }
            0
        } else {
            preferences.getInt(SavedGameStore.KEY_SCORE, 0)
        }
    }

    // Guarda la puntuación de la sesión en curso en almacenamiento local
    fun saveSessionScore(preferences: SharedPreferences, score: Int) {
        preferences.edit { putInt(SavedGameStore.KEY_SCORE, score) }
    }

    // Reinicia la puntuación de la sesión tras perder o abandonar
    fun resetSessionScore(preferences: SharedPreferences) {
        preferences.edit { putInt(SavedGameStore.KEY_SCORE, 0) }
        SavedGameStore.clear(preferences)
    }

    // Evalúa y guarda el récord local si la nueva puntuación es mayor que la anterior
    fun saveLocalRecordIfHigher(preferences: SharedPreferences, score: Int): Boolean {
        val previousRecord = preferences.getInt(SavedGameStore.KEY_RECORD, 0)
        val isNew = isNewRecord(score, previousRecord)
        if (isNew) {
            // Guarda el nuevo récord local y lo marca como pendiente de sincronización online
            preferences.edit {
                putInt(SavedGameStore.KEY_RECORD, score)
                putBoolean(KEY_PENDING_RECORD_SYNC, true)
            }
        }
        return isNew
    }

    // Consulta en Firestore si el usuario ya tiene un nombre registrado previamente
    fun fetchRemoteUserProfile(
        context: Context,
        preferences: SharedPreferences,
        onComplete: (Boolean?) -> Unit
    ) {
        getOrCreatePlayerId(preferences)
        val database = FirebaseAvailability.firestore(context)
        if (database == null) {
            onComplete(null)
            return
        }

        // Inicia sesión anónima en Firebase si aún no está autenticado
        FirebaseAvailability.signInAnonymouslyIfNeeded(context) { firebaseUser, _ ->
            if (firebaseUser == null) {
                onComplete(null)
                return@signInAnonymouslyIfNeeded
            }

            // Consulta el documento del usuario en la colección de puntuaciones
            database.collection("puntuaciones")
                .document(firebaseUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val remoteUsername = document.getString(KEY_USERNAME)
                    if (document.exists() && !remoteUsername.isNullOrBlank()) {
                        preferences.edit {
                            putString(KEY_USERNAME, remoteUsername)
                            putBoolean(SavedGameStore.KEY_USERNAME_REGISTERED, true)
                        }
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(
                        FirebaseAvailability.LOG_TAG,
                        "Error al consultar el registro del jugador",
                        exception
                    )
                    onComplete(null)
                }
        }
    }

    // Sube automáticamente a Firestore cualquier récord que haya quedado pendiente por falta de conexión
    fun syncPendingRecordIfAny(context: Context, preferences: SharedPreferences) {
        val localRecord = preferences.getInt(SavedGameStore.KEY_RECORD, 0)
        val username = preferences.getString(KEY_USERNAME, null)
        val hasPending = preferences.getBoolean(KEY_PENDING_RECORD_SYNC, false)

        if (localRecord <= 0 || username.isNullOrBlank() || !hasPending) return

        syncScoreOnline(
            context = context,
            preferences = preferences,
            scoreToSync = localRecord,
            usernameToSync = username,
            showToastOnError = false
        ) { isNew, _ ->
            if (isNew != null) {
                preferences.edit { putBoolean(KEY_PENDING_RECORD_SYNC, false) }
            }
        }
    }

    // Método para sincronizar la puntuación y nombre del jugador con Firebase Firestore
    fun syncScoreOnline(
        context: Context,
        preferences: SharedPreferences,
        scoreToSync: Int,
        usernameToSync: String?,
        showToastOnError: Boolean = true,
        onComplete: (isNewRecord: Boolean?, error: Exception?) -> Unit = { _, _ -> }
    ) {
        val database = FirebaseAvailability.firestore(context) ?: run {
            val error = IllegalStateException(context.getString(R.string.unknown_firebase_error))
            preferences.edit { putBoolean(KEY_PENDING_RECORD_SYNC, true) }
            if (showToastOnError) {
                showErrorToast(context)
            }
            onComplete(null, error)
            return
        }
        val username = usernameToSync ?: run {
            val error = IllegalArgumentException(context.getString(R.string.username_required))
            if (showToastOnError) {
                showErrorToast(context)
            }
            onComplete(null, error)
            return
        }

        val playerId = getOrCreatePlayerId(preferences)

        // Autentica de forma anónima antes de escribir en la base de datos
        FirebaseAvailability.signInAnonymouslyIfNeeded(context) { firebaseUser, authError ->
            val currentUser = firebaseUser ?: run {
                Log.e(FirebaseAvailability.LOG_TAG, "No se pudo autenticar en Firebase", authError)
                preferences.edit { putBoolean(KEY_PENDING_RECORD_SYNC, true) }
                if (showToastOnError) {
                    showErrorToast(context)
                }
                onComplete(null, authError ?: IllegalStateException("No se pudo autenticar en Firebase"))
                return@signInAnonymouslyIfNeeded
            }

            val scoreDocument = database.collection("puntuaciones").document(currentUser.uid)
            val scorePayload = hashMapOf<String, Any>(
                "playerId" to playerId,
                "nombreUsuario" to username,
                "puntuacion" to scoreToSync.toLong()
            )

            // Comprueba si el récord nuevo supera al existente en Firestore
            scoreDocument.get()
                .addOnSuccessListener { document ->
                    val remoteScore = document.getLong("puntuacion")?.toInt() ?: 0
                    val remoteNew = scoreToSync > remoteScore
                    if (scoreToSync >= remoteScore) {
                        // Guarda los datos actualizados en Firestore
                        scoreDocument.set(scorePayload)
                            .addOnCompleteListener { task ->
                                val isSuccessful = task.isSuccessful
                                if (!isSuccessful) {
                                    Log.e(FirebaseAvailability.LOG_TAG, "Error al guardar el récord en Firestore", task.exception)
                                    preferences.edit { putBoolean(KEY_PENDING_RECORD_SYNC, true) }
                                    if (showToastOnError) {
                                        showErrorToast(context)
                                    }
                                } else {
                                    // Marca el usuario y el récord como sincronizados
                                    preferences.edit {
                                        putBoolean(SavedGameStore.KEY_USERNAME_REGISTERED, true)
                                        putBoolean(KEY_PENDING_RECORD_SYNC, false)
                                    }
                                    if (remoteNew) {
                                        // Muestra notificación de éxito en la interfaz
                                        Handler(Looper.getMainLooper()).post {
                                            Toast.makeText(
                                                context.applicationContext,
                                                context.getString(R.string.record_registered),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                                val status = registrationStatusAfterWrite(remoteNew, isSuccessful)
                                onComplete(status, task.exception)
                            }
                    } else {
                        preferences.edit { putBoolean(KEY_PENDING_RECORD_SYNC, false) }
                        onComplete(false, null)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(FirebaseAvailability.LOG_TAG, "Error al consultar la puntuación remota", exception)
                    // Intenta guardar directamente si no se pudo leer el documento previo
                    scoreDocument.set(scorePayload)
                        .addOnCompleteListener { task ->
                            val isSuccessful = task.isSuccessful
                            if (!isSuccessful) {
                                Log.e(FirebaseAvailability.LOG_TAG, "Error al guardar el récord en Firestore", task.exception ?: exception)
                                preferences.edit { putBoolean(KEY_PENDING_RECORD_SYNC, true) }
                                if (showToastOnError) {
                                    showErrorToast(context)
                                }
                            } else {
                                preferences.edit {
                                    putBoolean(SavedGameStore.KEY_USERNAME_REGISTERED, true)
                                    putBoolean(KEY_PENDING_RECORD_SYNC, false)
                                }
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(
                                        context.applicationContext,
                                        context.getString(R.string.record_registered),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            onComplete(if (isSuccessful) true else null, task.exception ?: exception)
                        }
                }
        }
    }

    // Muestra un mensaje Toast informativo cuando ocurre un error de sincronización de red
    private fun showErrorToast(context: Context) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context.applicationContext,
                context.getString(R.string.score_sync_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}