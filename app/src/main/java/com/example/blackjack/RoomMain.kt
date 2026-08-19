package com.example.blackjack

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.blackjack.data.ScoreSyncManager
import com.example.blackjack.databinding.ActivityRoomMainBinding

class RoomMain : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityRoomMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Desactiva el modo noche forzado para mantener la apariencia uniforme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        // Infla la vista usando View Binding
        binding = ActivityRoomMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura la barra de herramientas (Toolbar)
        setSupportActionBar(binding.appBarRoomMain.toolbar)

        // Configura el menú lateral de navegación (DrawerLayout y NavigationView)
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_room_main)

        // Define los destinos principales de la barra de navegación
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Configura el botón de salida en la cabecera del menú lateral
        val btnExit = navView.getHeaderView(0).findViewById<Button>(R.id.btnExit)
        btnExit.setOnClickListener {
            // Cierra la actividad principal
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Si hay algún récord pendiente de subir guardado en local, intenta sincronizarlo con Firestore
        val preferences = getSharedPreferences(SavedGameStore.PREFS_NAME, MODE_PRIVATE)
        ScoreSyncManager.syncPendingRecordIfAny(this, preferences)
    }

    // Gestiona la navegación hacia atrás en el menú de la barra superior
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_room_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}