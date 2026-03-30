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
import androidx.compose.foundation.isSystemInDarkTheme
import com.nexohogar.presentation.theme.NexoHogarTheme

class MainActivity : ComponentActivity() {

    private val pendingNavigation = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(this)
        handleDeepLink(intent)

        setContent {
            val themeMode = ServiceLocator.themePreferences.themeMode
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }
            NexoHogarTheme(darkTheme = useDarkTheme) {
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
            // Try query parameter first (from intent:// URL), then fall back to fragment
            val accessToken = data.getQueryParameter("access_token")
                ?: data.fragment?.split("&")
                    ?.find { it.startsWith("access_token=") }
                    ?.substringAfter("access_token=")

            val type = data.getQueryParameter("type")
                ?: data.fragment?.split("&")
                    ?.find { it.startsWith("type=") }
                    ?.substringAfter("type=")

            Log.d("MainActivity", "type=$type, token length=${accessToken?.length}")

            if (type == "recovery" && !accessToken.isNullOrBlank()) {
                ResetPasswordTokenHolder.token = accessToken
                pendingNavigation.value = "reset_password"
            }
        }
    }
}