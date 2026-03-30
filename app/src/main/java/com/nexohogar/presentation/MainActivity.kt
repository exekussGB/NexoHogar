package com.nexohogar.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.rememberNavController
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.core.util.AppLogger
import com.nexohogar.presentation.navigation.NavGraph
import androidx.compose.foundation.isSystemInDarkTheme
import com.nexohogar.presentation.theme.NexoHogarTheme

/**
 * SEC-04: Logs via AppLogger.
 * SEC-05: Token de reset se pasa via NavArgs (sin ResetPasswordTokenHolder).
 */
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
                        AppLogger.d("MainActivity", "Navegando a: $nav")
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
        AppLogger.d("MainActivity", "Deep link recibido: $data")

        if (data.scheme == "nexohogar" && data.host == "reset-password") {
            val accessToken = data.getQueryParameter("access_token")
                ?: data.fragment?.split("&")
                    ?.find { it.startsWith("access_token=") }
                    ?.substringAfter("access_token=")

            val type = data.getQueryParameter("type")
                ?: data.fragment?.split("&")
                    ?.find { it.startsWith("type=") }
                    ?.substringAfter("type=")

            AppLogger.d("MainActivity", "type=$type, hasToken=${!accessToken.isNullOrBlank()}")

            if (type == "recovery" && !accessToken.isNullOrBlank()) {
                // SEC-05: Pasar token como argumento de navegación (URL-encoded)
                pendingNavigation.value = "reset_password/${Uri.encode(accessToken)}"
            }
        }
    }
}
