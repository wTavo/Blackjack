package com.example.blackjack

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseAvailability {
    const val LOG_TAG = "BlackjackFirebase"

    fun isConfigured(context: Context): Boolean =
        try {
            FirebaseApp.initializeApp(context) != null
        } catch (_: IllegalStateException) {
            false
        }

    fun auth(context: Context): FirebaseAuth? =
        if (isConfigured(context)) FirebaseAuth.getInstance() else null

    fun firestore(context: Context): FirebaseFirestore? =
        if (isConfigured(context)) FirebaseFirestore.getInstance() else null

    fun signInAnonymouslyIfNeeded(
        context: Context,
        onReady: (FirebaseUser?, Exception?) -> Unit
    ) {
        val firebaseAuth = auth(context)
        if (firebaseAuth == null) {
            Log.w(LOG_TAG, "Firebase no está configurado; se continuará en modo offline")
            onReady(null, IllegalStateException("Firebase no está configurado"))
            return
        }

        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            onReady(currentUser, null)
            return
        }

        firebaseAuth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(LOG_TAG, "Error en autenticación anónima", task.exception)
                }
                onReady(
                    if (task.isSuccessful) firebaseAuth.currentUser else null,
                    if (task.isSuccessful) null else task.exception
                )
            }
    }
}
