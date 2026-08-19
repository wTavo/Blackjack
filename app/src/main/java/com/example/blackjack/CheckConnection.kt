package com.example.blackjack

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.app.AlertDialog
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import pl.droidsonroids.gif.GifImageView
import com.example.blackjack.databinding.ActivityCheckConnectionBinding

class CheckConnection : AppCompatActivity() {
    private lateinit var binding: ActivityCheckConnectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_connection)

        // Infla y establece el contenido de la actividad usando View Binding
        binding = ActivityCheckConnectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura la imagen de carga con un recurso Gif
        val gifImageView: GifImageView = findViewById(R.id.imgLoading)
        gifImageView.setImageResource(R.drawable.loading)

        // Verifica la conexión fuera del hilo principal y devuelve el resultado a la interfaz.
        Thread {
            val isConnected = checkInternet()
            Handler(Looper.getMainLooper()).post {
                if (isConnected) {
                    prepararFirebaseYContinuar()
                } else {
                    showErrorDialog()
                }
            }
        }.start()
    }

    private fun prepararFirebaseYContinuar() {
        FirebaseAvailability.signInAnonymouslyIfNeeded(this) { _, _ ->
            val preferences = getSharedPreferences(SavedGameStore.PREFS_NAME, MODE_PRIVATE)
            com.example.blackjack.data.ScoreSyncManager.syncPendingRecordIfAny(this, preferences)
            startMainActivity()
        }
    }

    // Método para iniciar la actividad principal
    private fun startMainActivity() {
        val intent = Intent(this, RoomMain::class.java)
        startActivity(intent)
        finish()
    }

    // Método para mostrar un diálogo de error en caso de falta de conexión
    private fun showErrorDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.no_connection))
            .setMessage(getString(R.string.no_internet))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.exit)) { _, _ ->
                // Cierra la aplicación si se selecciona "salir"
                finish()
            }
            .setNegativeButton(getString(R.string.offline_play)) { _, _ ->
                // Inicia la actividad de juego sin conexión si se selecciona "Jugar sin conexión"
                val intent = Intent(this, Game::class.java)
                startActivity(intent)
                finish()
            }
            .show()
    }

    private fun checkInternet(): Boolean {
        return try {
            val request = Request.Builder()
                .url("https://www.google.com")
                .build()
            OkHttpClient().newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: IOException) {
            false
        }
    }
}

