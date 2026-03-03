package com.nexohogar.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.presentation.household.HouseholdScreen
import com.nexohogar.presentation.household.HouseholdViewModel
import com.nexohogar.presentation.login.LoginScreen
import com.nexohogar.presentation.login.LoginViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Household : Screen("household")
    object Main : Screen("main")
}

@Composable
fun NavGraph(
    navController: NavHostController
) {
    val sessionManager = ServiceLocator.sessionManager
    val authRepository = ServiceLocator.authRepository
    val householdRepository = ServiceLocator.householdRepository

    val startDestination = if (sessionManager.fetchAuthToken() != null) {
        Screen.Household.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            val viewModel = LoginViewModel(authRepository, sessionManager)
            LoginScreen(viewModel = viewModel) {
                navController.navigate(Screen.Household.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        }

        composable(Screen.Household.route) {
            val viewModel = HouseholdViewModel(householdRepository, sessionManager)
            HouseholdScreen(viewModel = viewModel) {
                // Navegar a Main...
            }
        }
    }
}
