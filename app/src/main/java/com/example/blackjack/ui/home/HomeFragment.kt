package com.example.blackjack.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.blackjack.Game
import com.example.blackjack.SavedGameStore
import com.example.blackjack.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Obtener una referencia al botón de juego desde el archivo de diseño mediante View Binding
        val btnPlay: TextView = binding.btnPlay
        val btnResume: TextView = binding.btnResume

        // Observar los cambios en el texto de un ViewModel asociado a la vista
        homeViewModel.text.observe(viewLifecycleOwner) {
            // Cuando cambia el valor del texto en el ViewModel, establecer el texto del botón de juego
            btnPlay.text = getString(com.example.blackjack.R.string.new_game)
        }

        // Configurar un listener para el clic en el botón de juego
        btnPlay.setOnClickListener {
            SavedGameStore.clear(
                requireContext().getSharedPreferences(SavedGameStore.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            )
            val intent = Intent(requireContext(), Game::class.java)
            startActivity(intent)
        }

        btnResume.setOnClickListener {
            startActivity(Intent(requireContext(), Game::class.java).apply {
                putExtra(Game.EXTRA_RESUME, true)
            })
        }

        actualizarBotonReanudar(btnResume)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        _binding?.let { actualizarBotonReanudar(it.btnResume) }
    }

    private fun actualizarBotonReanudar(button: TextView) {
        val preferences = requireContext()
            .getSharedPreferences(SavedGameStore.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        button.visibility = if (SavedGameStore.hasPendingGame(preferences)) View.VISIBLE else View.GONE
    }
}
