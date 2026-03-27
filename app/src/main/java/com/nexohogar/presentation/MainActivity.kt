package com.nexohogar.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.presentation.navigation.NavGraph
import com.nexohogar.presentation.theme.NexoHogarTheme

class MainActivity : ComponentActivity() {

    private var resetPasswordToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(this)
        handleResetPasswordDeepLink(intent)

        setContent {
            NexoHogarTheme {
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    resetPasswordToken?.let { token ->
                        navController.navigate("reset_password/$token")
                        resetPasswordToken = null
                    }
                }

                NavGraph(navController = navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleResetPasswordDeepLink(intent)
    }

    private fun handleResetPasswordDeepLink(intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme == "nexohogar" && data.host == "reset-password") {
            val fragment = data.fragment ?: return
            val params = fragment.split("&").associate {
                val (key, value) = it.split("=", limit = 2)
                key to value
            }
            val type = params["type"]
            val accessToken = params["access_token"]
            if (type == "recovery" && !accessToken.isNullOrBlank()) {
                resetPasswordToken = accessToken
            }
        }
    }
}