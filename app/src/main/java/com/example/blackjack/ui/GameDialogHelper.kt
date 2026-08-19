package com.example.blackjack.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import com.example.blackjack.R

// Clase auxiliar encargada de construir y mostrar todos los diálogos y mensajes emergentes de la partida
object GameDialogHelper {

    // Diálogo de confirmación para salir de la partida en curso
    fun showExitConfirmationDialog(context: Context, onConfirmExit: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.confirm_exit_title))
            .setMessage(context.getString(R.string.confirm_exit_message))
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.abandon)) { _, _ ->
                // Confirma el abandono de la partida
                onConfirmExit()
            }
            .setNegativeButton(context.getString(R.string.continue_game)) { dialog, _ ->
                // Cancela y continúa jugando
                dialog.dismiss()
            }
            .show()
    }

    // Diálogo final al perder una partida con opciones para volver a jugar o salir
    fun showGameOverDialog(
        context: Context,
        score: Int,
        onPlayAgain: () -> Unit,
        onExit: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.lose))
            .setMessage(context.getString(R.string.game_score_message, score))
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.abandon)) { _, _ ->
                // Sale de la mesa al menú principal
                onExit()
            }
            .setNegativeButton(context.getString(R.string.play)) { _, _ ->
                // Reinicia la partida inmediatamente
                onPlayAgain()
            }
            .show()
    }

    // Diálogo para solicitar y guardar el nombre de usuario del jugador para el ranking
    fun showUsernamePromptDialog(
        context: Context,
        onUsernameSaved: (String) -> Unit,
        onDismiss: () -> Unit
    ) {
        // Campo de texto con validación de caracteres permitidos
        val input = EditText(context).apply {
            hint = context.getString(R.string.username_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            maxLines = 1
            filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
                source.filter { character ->
                    character.isLetterOrDigit() || character == '.' || character == '_' || character == '-'
                }.toString()
            })
        }

        var handled = false
        fun handleSave(username: String) {
            if (handled) return
            handled = true
            onUsernameSaved(username)
        }

        fun handleDismiss() {
            if (handled) return
            handled = true
            onDismiss()
        }

        // Construcción del cuadro de diálogo modal
        val dialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.username_title))
            .setMessage(context.getString(R.string.username_message))
            .setView(input)
            .setPositiveButton(context.getString(R.string.save_username), null)
            .setNegativeButton(context.getString(R.string.close)) { _, _ -> handleDismiss() }
            .create()

        // Validación para evitar guardar nombres en blanco
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val username = input.text.toString().trim()
                if (username.isBlank()) {
                    input.error = context.getString(R.string.username_required)
                    return@setOnClickListener
                }
                dialog.dismiss()
                handleSave(username)
            }
        }
        dialog.setOnCancelListener { handleDismiss() }
        dialog.show()
    }

    // Mensaje flotante temporal que aparece en el centro de la mesa (Has ganado, Empate, etc.)
    fun showTimedMessagePopup(
        activity: Activity,
        rootView: View,
        dealerScoreContainer: View,
        playerScoreContainer: View,
        message: String,
        durationMs: Long = 1500L,
        onFinished: () -> Unit
    ) {
        // Infla la vista personalizada del mensaje emergente
        val content = activity.layoutInflater.inflate(R.layout.layout_mensaje_temporal, null)
        content.findViewById<TextView>(R.id.tvMensaje).text = message
        content.measure(
            View.MeasureSpec.makeMeasureSpec(rootView.width, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(rootView.height, View.MeasureSpec.AT_MOST)
        )

        // Calcula las coordenadas exactas entre los puntajes del CPU y del jugador
        val cpuLocation = IntArray(2)
        val playerLocation = IntArray(2)
        val rootLocation = IntArray(2)
        dealerScoreContainer.getLocationOnScreen(cpuLocation)
        playerScoreContainer.getLocationOnScreen(playerLocation)
        rootView.getLocationOnScreen(rootLocation)

        val cpuBottom = cpuLocation[1] + dealerScoreContainer.height
        val playerTop = playerLocation[1]
        val popupHeight = content.measuredHeight
        val minimumGap = popupHeight + 24
        val extraGap = (minimumGap - (playerTop - cpuBottom)).coerceAtLeast(0)
        if (extraGap > 0) playerScoreContainer.translationY = extraGap.toFloat()

        val adjustedPlayerTop = playerTop + extraGap
        val gap = adjustedPlayerTop - cpuBottom
        val targetScreenY = cpuBottom + (gap - popupHeight) / 2

        // Crea el popup flotante y lo muestra centrado
        val popup = PopupWindow(content, content.measuredWidth, popupHeight, false).apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            elevation = 8f
        }
        popup.showAtLocation(
            rootView,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            0,
            targetScreenY - rootLocation[1]
        )

        // Oculta el popup automáticamente tras la duración indicada
        Handler(Looper.getMainLooper()).postDelayed({
            popup.dismiss()
            onFinished()
        }, durationMs)
    }
}