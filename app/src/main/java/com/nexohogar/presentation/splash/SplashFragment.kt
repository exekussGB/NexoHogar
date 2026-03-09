package com.nexohogar.presentation.splash

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nexohogar.R
import com.nexohogar.data.local.SessionManager
import com.nexohogar.presentation.MainActivity

/**
 * Fragmento inicial para decidir el flujo de navegación (Auto-login).
 * Verifica la existencia de token y de un hogar seleccionado previamente.
 */
class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        
        val token = sessionManager.fetchAuthToken()
        val householdId = sessionManager.fetchSelectedHouseholdId()

        when {
            token != null && householdId != null -> {
                // Sesión completa activa: ir directo al Dashboard (MainActivity)
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            token != null -> {
                // Usuario autenticado pero sin hogar seleccionado
                findNavController().navigate(R.id.action_splash_to_household)
            }
            else -> {
                // Usuario debe iniciar sesión
                findNavController().navigate(R.id.action_splash_to_login)
            }
        }
    }
}
