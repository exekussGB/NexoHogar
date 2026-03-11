package com.nexohogar.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.RetrofitClient
import com.nexohogar.presentation.navigation.NavGraph
import com.nexohogar.presentation.theme.NexoHogarTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(this)
        RetrofitClient.sessionManager = sessionManager

        setContent {
            NexoHogarTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
