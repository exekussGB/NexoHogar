package com.nexohogar.presentation.splash

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nexohogar.R
import com.nexohogar.data.local.SessionManager

/**
 * Fragmento inicial para decidir el flujo de navegación (Auto-login).
 */
class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        
        // Simulación de pequeña espera para el splash o validación inmediata
        if (sessionManager.fetchAuthToken() != null) {
            // Usuario con sesión activa
            findNavController().navigate(R.id.action_splash_to_household)
        } else {
            // Usuario debe loguearse
            findNavController().navigate(R.id.action_splash_to_login)
        }
    }
}
