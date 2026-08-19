package com.example.blackjack

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.example.blackjack.data.ScoreSyncManager
import com.example.blackjack.databinding.ActivityGameBinding
import com.example.blackjack.game.Card
import com.example.blackjack.game.Deck
import com.example.blackjack.game.GameResult
import com.example.blackjack.game.GameRules
import com.example.blackjack.game.HandScorer
import com.example.blackjack.game.Rank
import com.example.blackjack.game.SavedGameState
import com.example.blackjack.game.resourceId
import com.example.blackjack.game.resourceIdToCard
import com.example.blackjack.ui.GameCardPresenter
import com.example.blackjack.ui.GameDialogHelper

class Game : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding

    // Baraja de cartas y listas para almacenar las cartas en mano
    private var deck = Deck.standard()
    val auxUsuario = mutableListOf<Int>()
    val auxCPU = mutableListOf<Int>()

    // Puntuaciones numéricas calculadas para el jugador y la banca
    var puntajeUsuario: Int = 0
    var puntajeUsuarioAs: Int = 0
    var puntajeCPU: Int = 0
    var puntajeCPUAs: Int = 0

    // Contadores de cartas y recurso de la carta oculta de la banca
    var contadorUsuarioCartas = 0
    var contadorCPUCartas = 0
    var recursoTapado = 0

    // Preferencias compartidas y variables de control del flujo de juego
    private val preferences by lazy { getSharedPreferences(SavedGameStore.PREFS_NAME, MODE_PRIVATE) }
    private var puntuacion: Int = 0
    private var usernameDialogShown = false
    private var usernamePromptDismissed = false
    private var blackjackAutomaticoIniciado = false
    private var turnoBancaIniciado = false
    private var partidaFinalizada = false
    private var accionJugadorEnCurso = false
    private var resultadoDialogMostrado = false
    private var doblarActivo = false

    companion object {
        const val EXTRA_RESUME = "reanudarPartida"
        private const val PLAYER_TURN = "PLAYER_TURN"
        private const val DEALER_TURN = "DEALER_TURN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Desactiva el modo oscuro para mantener los colores del tapete y textos
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        // Infla la vista de la actividad con View Binding
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa el presentador de cartas con la vista flotante de inspección táctil
        GameCardPresenter.init(binding.inspectCardView)

        // Intercepta el botón Atrás del sistema para mostrar diálogo de confirmación
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmExit()
            }
        })

        // Carga la puntuación acumulada de la sesión actual
        puntuacion = ScoreSyncManager.loadScore(preferences)
        binding.tvPuntuacion.text = puntuacion.toString()

        // Configura los escuchadores de eventos y comienza o restaura la partida
        setupListeners()
        startOrRestoreGame()
    }

    // Configura los eventos de clic para los botones de acción del jugador
    private fun setupListeners() {
        // Botón Pedir: Roba una carta adicional para el jugador
        binding.btnPedir.setOnClickListener {
            if (turnoBancaIniciado || partidaFinalizada || accionJugadorEnCurso) return@setOnClickListener
            accionJugadorEnCurso = true
            binding.btnPedir.isEnabled = false
            binding.btnDoblar.visibility = View.GONE
            binding.btnPlantarse.isEnabled = false

            val recurso = drawResource()
            GameCardPresenter.addCard(
                this,
                recurso,
                contadorUsuarioCartas,
                binding.fila1Usuario,
                binding.fila2Usuario,
                animated = true
            )
            auxUsuario.add(recurso)
            contadorUsuarioCartas++

            // Actualiza y evalúa la puntuación del jugador tras la animación
            Handler(Looper.getMainLooper()).postDelayed({
                actualizarPuntajeUsuario()
                binding.puntajesUsuario.visibility = View.VISIBLE
                manejarPuntajeUsuario()
                if (!partidaFinalizada && !turnoBancaIniciado) {
                    accionJugadorEnCurso = false
                    binding.btnPedir.isEnabled = true
                    binding.btnPlantarse.isEnabled = true
                }
            }, 400)
        }

        // Botón Doblar (x2): Roba exactamente una carta, dobla la recompensa y se planta
        binding.btnDoblar.setOnClickListener {
            if (turnoBancaIniciado || partidaFinalizada || accionJugadorEnCurso) return@setOnClickListener
            accionJugadorEnCurso = true
            doblarActivo = true

            // Oculta los botones para impedir pedir más cartas
            binding.btnPedir.visibility = View.GONE
            binding.btnDoblar.visibility = View.GONE
            binding.btnPlantarse.visibility = View.GONE

            val recurso = drawResource()
            GameCardPresenter.addCard(
                this,
                recurso,
                contadorUsuarioCartas,
                binding.fila1Usuario,
                binding.fila2Usuario,
                animated = true
            )
            auxUsuario.add(recurso)
            contadorUsuarioCartas++

            Handler(Looper.getMainLooper()).postDelayed({
                actualizarPuntajeUsuario()
                binding.puntajesUsuario.visibility = View.VISIBLE
                val score = HandScorer.score(auxUsuario.map(::resourceIdToCard))
                if (score.isBust) {
                    // Si se pasa de 21, pierde la ronda de inmediato
                    binding.tvPuntajeUsuario.setBackgroundColor(Color.RED)
                    binding.tvPuntajeUsuario2.setBackgroundColor(Color.RED)
                    binding.tvPuntajeCPU.setBackgroundColor(Color.GREEN)
                    mostrarResultadoPerdida()
                } else {
                    // Si no se pasa, inicia automáticamente el turno de la banca
                    turnoBancaIniciado = true
                    desvelar {
                        Handler(Looper.getMainLooper()).postDelayed({
                            ejecutarTurnoBanca()
                        }, 700)
                    }
                }
            }, 400)
        }

        // Botón Plantarse: Cede el turno a la banca con la mano actual
        binding.btnPlantarse.setOnClickListener {
            if (turnoBancaIniciado || partidaFinalizada || accionJugadorEnCurso) return@setOnClickListener
            accionJugadorEnCurso = true
            turnoBancaIniciado = true

            binding.btnPedir.visibility = View.GONE
            binding.btnDoblar.visibility = View.GONE
            binding.btnPlantarse.visibility = View.GONE

            // Revela la carta oculta de la banca y comienza su turno
            desvelar {
                Handler(Looper.getMainLooper()).postDelayed({
                    ejecutarTurnoBanca()
                }, 700)
            }
        }

        // Botón Salir en la barra superior
        binding.btnSalir.setOnClickListener {
            confirmExit()
        }
    }

    // Inicia una partida nueva o restaura el estado previo guardado
    private fun startOrRestoreGame() {
        val saved = SavedGameStore.load(preferences)
        if (intent.getBooleanExtra(EXTRA_RESUME, false) && saved != null) {
            // Restaura la partida guardada
            binding.puntajesUsuario.visibility = View.VISIBLE
            binding.puntajesCPU.visibility = View.VISIBLE
            restoreGame(saved)
        } else {
            // Inicia una ronda limpia repartiendo 2 cartas al jugador y 2 a la banca
            SavedGameStore.clear(preferences)
            binding.puntajesUsuario.visibility = View.INVISIBLE
            binding.puntajesCPU.visibility = View.INVISIBLE

            val cpuCard = drawResource()
            val playerCard = drawResource()
            recursoTapado = drawResource()
            val secondPlayerCard = drawResource()

            contadorCPUCartas = 0
            contadorUsuarioCartas = 0

            // 1. Reparte primera carta visible de la banca
            Handler(Looper.getMainLooper()).postDelayed({
                auxCPU.add(cpuCard)
                contadorCPUCartas++
                GameCardPresenter.addCard(this, cpuCard, 0, binding.fila1CPU, binding.fila2CPU, animated = true)
                Handler(Looper.getMainLooper()).postDelayed({
                    actualizarPuntajeCPUVisible()
                    binding.puntajesCPU.visibility = View.VISIBLE
                }, 400)
            }, 600)

            // 2. Reparte primera carta del jugador
            Handler(Looper.getMainLooper()).postDelayed({
                auxUsuario.add(playerCard)
                contadorUsuarioCartas++
                GameCardPresenter.addCard(this, playerCard, 0, binding.fila1Usuario, binding.fila2Usuario, animated = true)
                Handler(Looper.getMainLooper()).postDelayed({
                    actualizarPuntajeUsuario()
                    binding.puntajesUsuario.visibility = View.VISIBLE
                }, 400)
            }, 1200)

            // 3. Reparte segunda carta oculta (boca abajo) de la banca
            Handler(Looper.getMainLooper()).postDelayed({
                auxCPU.add(R.drawable.carta_trasera)
                contadorCPUCartas++
                GameCardPresenter.addCard(this, R.drawable.carta_trasera, 1, binding.fila1CPU, binding.fila2CPU, animated = true)
            }, 1800)

            // 4. Reparte segunda carta del jugador y habilita los botones de acción
            Handler(Looper.getMainLooper()).postDelayed({
                auxUsuario.add(secondPlayerCard)
                contadorUsuarioCartas++
                GameCardPresenter.addCard(this, secondPlayerCard, 1, binding.fila1Usuario, binding.fila2Usuario, animated = true)
                Handler(Looper.getMainLooper()).postDelayed({
                    actualizarPuntajeUsuario()
                    binding.btnPedir.visibility = View.VISIBLE
                    binding.btnDoblar.visibility = View.VISIBLE
                    binding.btnPlantarse.visibility = View.VISIBLE
                    manejarPuntajeUsuario()
                }, 400)
            }, 2400)
        }
    }

    override fun onStop() {
        // Guarda la partida automáticamente si la app pasa a segundo plano durante el juego
        if (!isFinishing && !partidaFinalizada && puntuacion > 0) {
            savePendingGame()
        }
        super.onStop()
    }

    // Guarda el estado de la partida no concluida en SharedPreferences
    private fun savePendingGame() {
        SavedGameStore.save(
            preferences,
            SavedGameState(
                auxUsuario.toList(),
                auxCPU.toList(),
                recursoTapado,
                deck.snapshot().map { it.resourceId() },
                puntuacion,
                if (turnoBancaIniciado) DEALER_TURN else PLAYER_TURN
            )
        )
    }

    // Restaura las cartas, puntuación y turno desde el objeto SavedGameState
    private fun restoreGame(state: SavedGameState) {
        GameCardPresenter.resetState()
        deck = Deck.from(state.remainingCards.map(::resourceIdToCard))
        auxUsuario.clear()
        auxUsuario.addAll(state.playerCards)
        auxCPU.clear()
        auxCPU.addAll(state.dealerCards)
        recursoTapado = state.hiddenDealerCard
        puntuacion = state.score
        contadorUsuarioCartas = auxUsuario.size
        contadorCPUCartas = auxCPU.size
        turnoBancaIniciado = state.phase == DEALER_TURN

        binding.tvPuntuacion.text = puntuacion.toString()
        binding.fila1Usuario.removeAllViews()
        binding.fila2Usuario.removeAllViews()
        binding.fila1CPU.removeAllViews()
        binding.fila2CPU.removeAllViews()

        // Agrega todas las cartas guardadas a la mesa
        auxUsuario.forEachIndexed { index, card ->
            GameCardPresenter.addCard(this, card, index, binding.fila1Usuario, binding.fila2Usuario, animated = false)
        }
        auxCPU.forEachIndexed { index, card ->
            GameCardPresenter.addCard(this, card, index, binding.fila1CPU, binding.fila2CPU, animated = false)
        }

        actualizarPuntajeUsuario()
        if (auxCPU.contains(R.drawable.carta_trasera)) {
            actualizarPuntajeCPUVisible()
        } else {
            actualizarPuntajeCPU()
        }

        if (turnoBancaIniciado) {
            binding.btnPedir.visibility = View.GONE
            binding.btnPlantarse.visibility = View.GONE
            if (auxCPU.contains(R.drawable.carta_trasera)) {
                desvelar {
                    Handler(Looper.getMainLooper()).postDelayed({ ejecutarTurnoBanca() }, 700)
                }
            } else {
                Handler(Looper.getMainLooper()).postDelayed({ ejecutarTurnoBanca() }, 700)
            }
        } else if (puntajeUsuario >= 21) {
            manejarPuntajeUsuario()
        } else {
            binding.btnPedir.visibility = View.VISIBLE
            binding.btnPlantarse.visibility = View.VISIBLE
        }
    }

    // Calcula y actualiza las vistas de puntuación (incluyendo valor alternativo del As si existe)
    private fun updateScoreViews(
        cards: List<Card>,
        mainTextView: TextView,
        secondaryTextView: TextView,
        separatorView: View
    ): Pair<Int, Int> {
        val score = HandScorer.score(cards)
        val hasAce = cards.any { it.rank == Rank.ACE }
        mainTextView.text = score.best.toString()
        secondaryTextView.text = score.alternative.toString()

        val hasAlternative = hasAce && score.alternative != score.best
        separatorView.visibility = if (hasAlternative) View.VISIBLE else View.GONE
        secondaryTextView.visibility = if (hasAlternative) View.VISIBLE else View.GONE
        return Pair(score.best, score.best)
    }

    // Actualiza la puntuación en pantalla del jugador
    private fun actualizarPuntajeUsuario() {
        val cards = auxUsuario.map(::resourceIdToCard)
        val (best, alt) = updateScoreViews(
            cards,
            binding.tvPuntajeUsuario,
            binding.tvPuntajeUsuario2,
            binding.tvSeparador
        )
        puntajeUsuario = best
        puntajeUsuarioAs = alt
    }

    // Actualiza la puntuación total de la banca
    private fun actualizarPuntajeCPU() {
        val cards = auxCPU.map(::resourceIdToCard)
        val (best, alt) = updateScoreViews(
            cards,
            binding.tvPuntajeCPU,
            binding.tvPuntajeCPU2,
            binding.tvSeparador2
        )
        puntajeCPU = best
        puntajeCPUAs = alt
    }

    // Actualiza la puntuación visible de la banca mientras una carta esté oculta
    private fun actualizarPuntajeCPUVisible() {
        val cards = auxCPU.filter { it != R.drawable.carta_trasera }.map(::resourceIdToCard)
        val (best, alt) = updateScoreViews(
            cards,
            binding.tvPuntajeCPU,
            binding.tvPuntajeCPU2,
            binding.tvSeparador2
        )
        puntajeCPU = best
        puntajeCPUAs = alt
    }

    // Extrae una carta de la baraja y devuelve su recurso gráfico Drawable
    private fun drawResource(): Int = deck.draw().resourceId()

    // Revela la carta oculta de la banca con animación de volteo (flip)
    private fun desvelar(onFinished: (() -> Unit)? = null) {
        val posicion = auxCPU.indexOf(R.drawable.carta_trasera)
        if (posicion < 0) {
            onFinished?.invoke()
            return
        }

        binding.fila1CPU.setPadding(0, 0, 0, 0)
        val view = binding.fila1CPU.getChildAt(1)
        if (view !is android.widget.ImageView) {
            onFinished?.invoke()
            return
        }
        auxCPU[posicion] = recursoTapado

        GameCardPresenter.animateFlip(view, recursoTapado) {
            actualizarPuntajeCPU()
            if (puntajeCPU > 21) binding.tvPuntajeCPU.setBackgroundColor(Color.RED)
            if (puntajeCPUAs > 21) binding.tvPuntajeCPU2.setBackgroundColor(Color.RED)
            onFinished?.invoke()
        }
    }

    // Evalúa el estado de la mano del jugador (Blackjack natural, pasarse de 21 o alcanzar 21)
    private fun manejarPuntajeUsuario() {
        if (partidaFinalizada) return

        val playerScore = HandScorer.score(auxUsuario.map(::resourceIdToCard))
        if (playerScore.isBlackjack && !blackjackAutomaticoIniciado) {
            // Victoria por Blackjack natural
            blackjackAutomaticoIniciado = true
            turnoBancaIniciado = true
            binding.btnPedir.visibility = View.GONE
            binding.btnDoblar.visibility = View.GONE
            binding.btnPlantarse.visibility = View.GONE
            binding.tvPuntajeUsuario.setBackgroundColor(Color.GREEN)
            desvelar {
                Handler(Looper.getMainLooper()).postDelayed({
                    resolverPartida()
                }, 700)
            }
            return
        }

        when {
            puntajeUsuario > 21 -> {
                // El jugador se pasó de 21 y pierde la ronda
                partidaFinalizada = true
                SavedGameStore.clear(preferences)
                binding.btnPedir.visibility = View.GONE
                binding.btnDoblar.visibility = View.GONE
                binding.btnPlantarse.visibility = View.GONE
                binding.tvPuntajeUsuario.setBackgroundColor(Color.RED)
                binding.tvPuntajeUsuario2.setBackgroundColor(Color.RED)
                Handler(Looper.getMainLooper()).postDelayed({
                    mostrarResultadoPerdida()
                }, 500)
            }
            puntajeUsuario == 21 -> {
                // El jugador llegó a 21 exactos y se inicia automáticamente el turno de la banca
                binding.btnPedir.visibility = View.GONE
                binding.btnDoblar.visibility = View.GONE
                binding.btnPlantarse.visibility = View.GONE
                binding.tvPuntajeUsuario.setBackgroundColor(Color.GREEN)
                if (!turnoBancaIniciado) {
                    turnoBancaIniciado = true
                    desvelar {
                        Handler(Looper.getMainLooper()).postDelayed({
                            ejecutarTurnoBanca()
                        }, 700)
                    }
                }
            }
        }
    }

    // Ejecuta el turno automático de la banca (roba hasta alcanzar al menos 17 puntos)
    private fun ejecutarTurnoBanca() {
        if (partidaFinalizada) return

        val dealerScore = HandScorer.score(auxCPU.map(::resourceIdToCard))
        puntajeCPU = dealerScore.best
        puntajeCPUAs = dealerScore.best

        if (dealerScore.best < 17) {
            // La banca roba una carta adicional
            robarCartaBanca()
            Handler(Looper.getMainLooper()).postDelayed({
                ejecutarTurnoBanca()
            }, 550)
        } else {
            // La banca se planta y se evalúa el ganador
            resolverPartida()
        }
    }

    // Roba una carta para la banca con animación
    private fun robarCartaBanca() {
        val recurso = drawResource()
        GameCardPresenter.addCard(
            this,
            recurso,
            contadorCPUCartas,
            binding.fila1CPU,
            binding.fila2CPU,
            animated = true
        )
        auxCPU.add(recurso)
        contadorCPUCartas++

        Handler(Looper.getMainLooper()).postDelayed({
            actualizarPuntajeCPU()
            binding.puntajesCPU.visibility = View.VISIBLE
        }, 400)
    }

    // Compara las puntuaciones finales de ambos y determina el resultado de la ronda
    private fun resolverPartida() {
        if (partidaFinalizada) return
        partidaFinalizada = true

        val playerScore = HandScorer.score(auxUsuario.map(::resourceIdToCard))
        val dealerScore = HandScorer.score(auxCPU.map(::resourceIdToCard))

        when (GameRules.resolve(playerScore, dealerScore)) {
            GameResult.PLAYER_WIN -> {
                // Victoria del jugador: suma puntos y muestra mensaje
                binding.tvPuntajeUsuario.setBackgroundColor(Color.GREEN)
                binding.tvPuntajeCPU.setBackgroundColor(Color.RED)
                val puntosGanados = if (doblarActivo) 2 else 1
                puntuacion += puntosGanados
                binding.tvPuntuacion.text = puntuacion.toString()
                ScoreSyncManager.saveSessionScore(preferences, puntuacion)
                ScoreSyncManager.saveLocalRecordIfHigher(preferences, puntuacion)
                SavedGameStore.clear(preferences)
                mostrarDialogoResultado(getString(R.string.win)) {
                    iniciarSiguienteRonda()
                }
            }
            GameResult.DEALER_WIN -> {
                // Victoria de la banca: muestra resultado de derrota
                binding.tvPuntajeUsuario.setBackgroundColor(Color.RED)
                binding.tvPuntajeUsuario2.setBackgroundColor(Color.RED)
                binding.tvPuntajeCPU.setBackgroundColor(Color.GREEN)
                mostrarResultadoPerdida()
            }
            GameResult.DRAW -> {
                // Empate: mantiene la puntuación y avanza de ronda
                ScoreSyncManager.saveSessionScore(preferences, puntuacion)
                SavedGameStore.clear(preferences)
                mostrarDialogoResultado(getString(R.string.draw)) {
                    iniciarSiguienteRonda()
                }
            }
        }
    }

    // Limpia el tapete y reparte una nueva ronda sin recrear la Activity
    private fun iniciarSiguienteRonda() {
        SavedGameStore.clear(preferences)
        GameCardPresenter.resetState()
        deck = Deck.standard()
        auxUsuario.clear()
        auxCPU.clear()
        contadorUsuarioCartas = 0
        contadorCPUCartas = 0
        recursoTapado = 0
        puntajeUsuario = 0
        puntajeUsuarioAs = 0
        puntajeCPU = 0
        puntajeCPUAs = 0

        doblarActivo = false
        blackjackAutomaticoIniciado = false
        turnoBancaIniciado = false
        partidaFinalizada = false
        accionJugadorEnCurso = false
        resultadoDialogMostrado = false

        binding.fila1Usuario.removeAllViews()
        binding.fila2Usuario.removeAllViews()
        binding.fila1CPU.removeAllViews()
        binding.fila2CPU.removeAllViews()

        val defaultBgColor = Color.parseColor("#DDD6D6")
        binding.tvPuntajeUsuario.setBackgroundColor(defaultBgColor)
        binding.tvPuntajeUsuario2.setBackgroundColor(defaultBgColor)
        binding.tvPuntajeCPU.setBackgroundColor(defaultBgColor)
        binding.tvPuntajeCPU2.setBackgroundColor(defaultBgColor)
        binding.puntajesUsuario.translationY = 0f

        binding.puntajesUsuario.visibility = View.INVISIBLE
        binding.puntajesCPU.visibility = View.INVISIBLE
        binding.btnPedir.visibility = View.GONE
        binding.btnDoblar.visibility = View.GONE
        binding.btnPlantarse.visibility = View.GONE
        binding.btnPedir.isEnabled = true
        binding.btnDoblar.isEnabled = true
        binding.btnPlantarse.isEnabled = true

        startOrRestoreGame()
    }

    // Muestra el mensaje temporal de derrota y luego abre el diálogo final
    private fun mostrarResultadoPerdida() {
        if (resultadoDialogMostrado) return
        resultadoDialogMostrado = true
        mostrarDialogoResultado(getString(R.string.lose)) {
            mostrarDialogoPerder()
        }
    }

    // Muestra el mensaje temporal en el centro de la mesa
    private fun mostrarDialogoResultado(mensaje: String, onFinished: () -> Unit) {
        GameDialogHelper.showTimedMessagePopup(
            activity = this,
            rootView = binding.root,
            dealerScoreContainer = binding.puntajesCPU,
            playerScoreContainer = binding.puntajesUsuario,
            message = mensaje,
            onFinished = onFinished
        )
    }

    // Procesa el fin de la partida, guardando récords y solicitando nombre si aplica
    private fun mostrarDialogoPerder() {
        partidaFinalizada = true
        SavedGameStore.clear(preferences)
        val scoreFinal = puntuacion
        val username = preferences.getString(ScoreSyncManager.KEY_USERNAME, null)
        val usernameRegistered = preferences.getBoolean(SavedGameStore.KEY_USERNAME_REGISTERED, false)

        if (scoreFinal > 0) {
            ScoreSyncManager.saveLocalRecordIfHigher(preferences, scoreFinal)
            if (!username.isNullOrBlank()) {
                ScoreSyncManager.syncScoreOnline(this, preferences, scoreFinal, username) { remoteNew, _ ->
                    if (remoteNew == true) {
                        preferences.edit { putBoolean(SavedGameStore.KEY_USERNAME_REGISTERED, true) }
                    }
                }
            }
        }

        // Si es la primera vez que consigue récord con puntuación > 0, pide nombre de usuario
        if (shouldRequestUsername(username, scoreFinal, usernameRegistered) && !usernamePromptDismissed) {
            showUsernameDialog(
                onFinished = {
                    val nuevoUsername = preferences.getString(ScoreSyncManager.KEY_USERNAME, null)
                    if (!nuevoUsername.isNullOrBlank()) {
                        ScoreSyncManager.syncScoreOnline(this, preferences, scoreFinal, nuevoUsername) { _, _ -> }
                    }
                    mostrarDialogoPerderFinal()
                },
                onClose = {
                    usernamePromptDismissed = true
                    mostrarDialogoPerderFinal()
                }
            )
        } else {
            mostrarDialogoPerderFinal()
        }
    }

    // Muestra el modal de fin de partida (Jugar de nuevo o Salir)
    private fun mostrarDialogoPerderFinal() {
        GameDialogHelper.showGameOverDialog(
            context = this,
            score = puntuacion,
            onPlayAgain = {
                puntuacion = 0
                ScoreSyncManager.resetSessionScore(preferences)
                binding.tvPuntuacion.text = "0"
                iniciarSiguienteRonda()
            },
            onExit = {
                puntuacion = 0
                ScoreSyncManager.resetSessionScore(preferences)
                finish()
            }
        )
    }

    // Muestra el diálogo para registrar el nombre de usuario
    private fun showUsernameDialog(
        onFinished: () -> Unit = {},
        onClose: () -> Unit = {}
    ) {
        if (usernameDialogShown) return
        usernameDialogShown = true

        GameDialogHelper.showUsernamePromptDialog(
            context = this,
            onUsernameSaved = { username ->
                preferences.edit { putString(ScoreSyncManager.KEY_USERNAME, username) }
                usernameDialogShown = false
                onFinished()
            },
            onDismiss = {
                usernameDialogShown = false
                onClose()
            }
        )
    }

    // Muestra el diálogo de confirmación de abandono de partida
    private fun confirmExit() {
        GameDialogHelper.showExitConfirmationDialog(this) {
            partidaFinalizada = true
            SavedGameStore.clear(preferences)
            val score = puntuacion
            val username = preferences.getString(ScoreSyncManager.KEY_USERNAME, null)
            if (score > 0) {
                ScoreSyncManager.saveLocalRecordIfHigher(preferences, score)
                if (!username.isNullOrBlank()) {
                    ScoreSyncManager.syncScoreOnline(this, preferences, score, username) { _, _ -> }
                }
            }
            ScoreSyncManager.resetSessionScore(preferences)
            finish()
        }
    }
}