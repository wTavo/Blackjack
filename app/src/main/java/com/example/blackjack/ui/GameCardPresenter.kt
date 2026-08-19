package com.example.blackjack.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import com.example.blackjack.R

// Presentador encargado de crear, posicionar y animar las cartas en el tapete de juego
object GameCardPresenter {
    const val CARD_WIDTH = 250
    const val CARD_HEIGHT = 350
    private const val MAX_CARDS_PER_ROW = 4
    private const val STACKED_OVERLAP_MARGIN = -120

    private val bounceHandler = Handler(Looper.getMainLooper())
    private var activeBounceRunnable: Runnable? = null
    private var currentlyInspectedSourceCard: ImageView? = null
    private var inspectCardView: ImageView? = null

    // Inicializa la referencia a la vista flotante de inspección táctil
    fun init(inspectView: ImageView) {
        inspectCardView = inspectView
    }

    // Restablece el estado de animación y oculta la vista flotante al iniciar una nueva ronda
    fun resetState() {
        activeBounceRunnable?.let { bounceHandler.removeCallbacks(it) }
        activeBounceRunnable = null
        currentlyInspectedSourceCard?.alpha = 1f
        currentlyInspectedSourceCard = null
        inspectCardView?.visibility = View.GONE
        inspectCardView?.animate()?.cancel()
    }

    // Crea y configura una nueva vista ImageView para una carta
    fun createCardImageView(context: Context, resourceId: Int, isStacked: Boolean = false): ImageView {
        return ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(CARD_WIDTH, CARD_HEIGHT).apply {
                // Aplica margen negativo para apilar cartas en la segunda fila
                if (isStacked) {
                    leftMargin = STACKED_OVERLAP_MARGIN
                }
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(resourceId)
            // Configura el evento de clic para la inspección táctil de la carta
            setupCardClickListener(this)
        }
    }

    // Configura el evento de toque para inspeccionar la carta con animación de zoom
    private fun setupCardClickListener(image: ImageView) {
        image.setOnClickListener {
            // Cancela el retorno pendiente previo si existe
            activeBounceRunnable?.let { bounceHandler.removeCallbacks(it) }
            activeBounceRunnable = null

            // Restaura la opacidad de la carta anterior si estaba activa
            currentlyInspectedSourceCard?.alpha = 1f

            val inspectView = inspectCardView
            val rootView = image.rootView as? ViewGroup

            if (inspectView != null && rootView != null) {
                currentlyInspectedSourceCard = image
                bounceCardWithOverlay(image, inspectView, rootView)
            }
        }
    }

    // Ejecuta la animación de rebote y zoom elevando la carta a una capa superior flotante
    private fun bounceCardWithOverlay(sourceImage: ImageView, inspectView: ImageView, rootView: ViewGroup) {
        inspectView.animate().cancel()

        // Obtiene la posición exacta en pantalla de la carta seleccionada
        val sourceLoc = IntArray(2)
        sourceImage.getLocationOnScreen(sourceLoc)
        val rootLoc = IntArray(2)
        rootView.getLocationOnScreen(rootLoc)

        val startX = (sourceLoc[0] - rootLoc[0]).toFloat()
        val startY = (sourceLoc[1] - rootLoc[1]).toFloat()

        val cardWidth = sourceImage.width.takeIf { it > 0 } ?: CARD_WIDTH
        val cardHeight = sourceImage.height.takeIf { it > 0 } ?: CARD_HEIGHT

        // Configura la imagen y dimensiones de la vista flotante de inspección
        inspectView.setImageDrawable(sourceImage.drawable)
        inspectView.layoutParams = (inspectView.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
            width = cardWidth
            height = cardHeight
        } ?: ViewGroup.LayoutParams(cardWidth, cardHeight)

        // Establece el pivote central y posición inicial
        inspectView.pivotX = cardWidth / 2f
        inspectView.pivotY = cardHeight / 2f
        inspectView.x = startX
        inspectView.y = startY
        inspectView.scaleX = 1f
        inspectView.scaleY = 1f
        inspectView.visibility = View.VISIBLE

        // Oculta temporalmente la carta original para evitar duplicidad visual
        sourceImage.alpha = 0f

        // Determina la dirección de elevación según sea jugador o banca
        val isDealer = (sourceImage.parent as? View)?.id == R.id.fila1CPU || (sourceImage.parent as? View)?.id == R.id.fila2CPU
        val targetY = if (isDealer) 35f else -35f

        // Animación de rebote (pop con zoom y elevación)
        inspectView.animate()
            .scaleX(1.28f)
            .scaleY(1.28f)
            .y(startY + targetY)
            .setDuration(280)
            .setInterpolator(OvershootInterpolator(2.2f))
            .withEndAction {
                // Programa el regreso automático a su tamaño y posición original
                val returnRunnable = Runnable {
                    inspectView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .y(startY)
                        .setDuration(220)
                        .setInterpolator(DecelerateInterpolator(1.5f))
                        .withEndAction {
                            inspectView.visibility = View.GONE
                            sourceImage.alpha = 1f
                            if (currentlyInspectedSourceCard == sourceImage) {
                                currentlyInspectedSourceCard = null
                            }
                        }
                        .start()
                }
                activeBounceRunnable = returnRunnable
                bounceHandler.postDelayed(returnRunnable, 800)
            }
            .start()
    }

    // Determina si la carta debe agregarse a la primera o a la segunda fila
    fun getTargetRow(index: Int, row1: LinearLayout, row2: LinearLayout): LinearLayout {
        return if (index < MAX_CARDS_PER_ROW) row1 else row2
    }

    // Método principal para crear y agregar una carta a la fila correspondiente
    fun addCard(
        context: Context,
        resourceId: Int,
        index: Int,
        row1: LinearLayout,
        row2: LinearLayout,
        animated: Boolean = false
    ): ImageView {
        val targetRow = getTargetRow(index, row1, row2)
        val isStacked = targetRow == row2 && row2.childCount > 0
        val image = createCardImageView(context, resourceId, isStacked = isStacked)
        targetRow.setPadding(0, 0, 0, 0)

        if (animated) {
            // Agrega la carta aplicando la animación de deslizamiento y recentrado
            addCardWithAnimation(targetRow, image)
        } else {
            row1.layoutTransition = null
            row2.layoutTransition = null
            targetRow.addView(image)
        }
        return image
    }

    // Agrega la carta con animación de deslizamiento desde la derecha y recentrado suave
    fun addCardWithAnimation(row: LinearLayout, image: ImageView) {
        row.layoutTransition = null

        // Aplica ChangeBounds para deslizar suavemente las cartas existentes hacia la izquierda
        if (row.childCount > 0) {
            val transition = ChangeBounds().apply {
                duration = 450
                interpolator = DecelerateInterpolator(1.4f)
            }
            TransitionManager.beginDelayedTransition(row, transition)
        }

        // Configura la posición inicial fuera de pantalla y opacidad cero
        image.translationX = 450f
        image.alpha = 0f
        row.addView(image)

        // Anima la entrada de la carta a su posición final
        image.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(450)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()
    }

    // Animación de giro (flip) para revelar la carta oculta de la banca
    fun animateFlip(
        imageView: ImageView,
        newResourceId: Int,
        onFlipped: (() -> Unit)? = null
    ) {
        // Reduce el ancho a cero simulando el volteo
        imageView.animate()
            .scaleX(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                // Cambia el recurso a la carta boca arriba
                imageView.setImageResource(newResourceId)
                // Expande de nuevo el ancho mostrando la nueva carta
                imageView.animate()
                    .scaleX(1f)
                    .setDuration(350)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        onFlipped?.invoke()
                    }
                    .start()
            }
            .start()
    }
}