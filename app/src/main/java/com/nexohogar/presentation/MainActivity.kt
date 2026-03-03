package com.nexohogar.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nexohogar.R
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.RetrofitClient

/**
 * Actividad principal que aloja el NavHostFragment para la navegación entre fragmentos.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🔥 Inicialización del SessionManager para el RetrofitClient
        val sessionManager = SessionManager(this)
        RetrofitClient.sessionManager = sessionManager

        setContentView(R.layout.activity_main)
    }
}
