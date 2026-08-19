package com.example.blackjack.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.blackjack.FirebaseAvailability
import com.example.blackjack.SavedGameStore
import com.example.blackjack.Score
import com.example.blackjack.adapterPuntuaciones
import com.example.blackjack.databinding.FragmentGalleryBinding
import com.google.firebase.firestore.Query

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Infla el diseño del fragmento de puntuaciones
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        // Carga la lista de mejores puntuaciones desde Firestore
        cargarPuntuaciones()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Recarga las puntuaciones al volver a mostrar el fragmento
        if (_binding != null) cargarPuntuaciones()
    }

    // Método para consultar y listar las puntuaciones del ranking global
    private fun cargarPuntuaciones() {
        val prefs = requireContext().getSharedPreferences(SavedGameStore.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        // Sube cualquier récord local pendiente antes de consultar la lista
        com.example.blackjack.data.ScoreSyncManager.syncPendingRecordIfAny(requireContext(), prefs)

        val database = FirebaseAvailability.firestore(requireContext()) ?: return
        val localPlayerId = prefs.getString(SavedGameStore.KEY_PLAYER_ID, null)

        // Obtiene las puntuaciones ordenadas de mayor a menor
        database.collection("puntuaciones")
            .orderBy("puntuacion", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Mapea los documentos de Firestore a la lista de objetos Score
                val scores = querySnapshot.documents.map { document ->
                    Score(
                        document.getString("nombreUsuario")
                            ?: getString(com.example.blackjack.R.string.default_username),
                        document.getLong("puntuacion") ?: 0,
                        document.getString("playerId")
                    )
                }
                // Asigna el adaptador personalizado a la lista de la interfaz
                binding.listPuntuaciones.adapter = adapterPuntuaciones(
                    requireContext(),
                    scores,
                    localPlayerId
                )
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Libera la referencia del View Binding al destruir la vista
        _binding = null
    }
}