package com.nexohogar.presentation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.rememberNavController
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.presentation.navigation.NavGraph
import com.nexohogar.presentation.theme.NexoHogarTheme

class MainActivity : ComponentActivity() {

    private val pendingNavigation = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(this)
        handleDeepLink(intent)

        setContent {
            NexoHogarTheme {
                val navController = rememberNavController()
                val nav = pendingNavigation.value

                LaunchedEffect(nav) {
                    if (nav != null) {
                        Log.d("MainActivity", "Navegando a: $nav")
                        navController.navigate(nav)
                        pendingNavigation.value = null
                    }
                }

                NavGraph(navController = navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val data = intent.data ?: return
        Log.d("MainActivity", "Deep link recibido: $data")

        if (data.scheme == "nexohogar" && data.host == "reset-password") {
            val fragment = data.fragment ?: return
            val params = fragment.split("&").associate {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
            }

            val type = params["type"]
            val accessToken = params["access_token"]
            Log.d("MainActivity", "type=$type, token length=${accessToken?.length}")

            if (type == "recovery" && !accessToken.isNullOrBlank()) {
                ResetPasswordTokenHolder.token = accessToken
                pendingNavigation.value = "reset_password"
            }
        }
    }
}