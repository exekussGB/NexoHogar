package com.nexohogar.presentation.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nexohogar.R

/**
 * Actividad que gestiona el flujo de autenticación (Splash, Login, Selección de Hogar).
 */
class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
    }
}
