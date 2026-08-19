package com.example.blackjack

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class adapterPuntuaciones(
    private val context: Context,
    private val scores: List<Score>,
    private val localPlayerId: String?
) : BaseAdapter() {

    // Devuelve la cantidad de elementos en la lista
    override fun getCount(): Int = scores.size

    // Devuelve el objeto Score en la posición especificada
    override fun getItem(position: Int): Any = scores[position]

    // Devuelve un identificador único para el elemento en la posición dada
    override fun getItemId(position: Int): Long = position.toLong()

    // Obtiene la vista que representa un elemento en la posición dada
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // Obtiene o infla la vista según sea necesario
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_score, parent, false)

        // Obtiene las referencias a los TextView dentro de la vista
        val rankTextView: TextView = view.findViewById(R.id.textViewRank)
        val usernameTextView: TextView = view.findViewById(R.id.textViewEmail)
        val scoreTextView: TextView = view.findViewById(R.id.textViewScore)
        val scoreRow: View = view.findViewById(R.id.scoreRow)

        // Obtiene el objeto Score en la posición dada
        val score: Score = getItem(position) as Score

        val isCurrentPlayer = isCurrentPlayer(score.playerId, localPlayerId)
        rankTextView.text = context.getString(R.string.rank_prefix, position + 1)
        usernameTextView.text = score.nombreUsuario.orEmpty()
        scoreRow.setBackgroundColor(
            context.getColor(
                if (isCurrentPlayer) R.color.score_highlight else android.R.color.transparent
            )
        )

        // Establece el texto del TextView scoreTextView con la puntuación del objeto Score
        scoreTextView.text = context.getString(R.string.score_prefix, score.puntuacion)

        // Devuelve la vista poblada con los datos correspondientes
        return view
    }
}
