package com.nexohogar.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.presentation.accounts.AccountsScreen
import com.nexohogar.presentation.accounts.AccountsViewModel
import com.nexohogar.presentation.dashboard.DashboardScreen
import com.nexohogar.presentation.dashboard.DashboardViewModel
import com.nexohogar.presentation.household.HouseholdScreen
import com.nexohogar.presentation.household.HouseholdViewModel
import com.nexohogar.presentation.login.LoginScreen
import com.nexohogar.presentation.login.LoginViewModel
import com.nexohogar.presentation.transactiondetail.TransactionDetailScreen
import com.nexohogar.presentation.transactiondetail.TransactionDetailViewModel
import com.nexohogar.presentation.transactions.TransactionsScreen
import com.nexohogar.presentation.transactions.TransactionsViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Household : Screen("household")
    object Dashboard : Screen("dashboard")
    object Accounts : Screen("accounts")
    object Transactions : Screen("transactions")
    object TransactionDetail : Screen("transaction_detail/{transactionId}") {
        fun createRoute(transactionId: String) = "transaction_detail/$transactionId"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController
) {
    val sessionManager = ServiceLocator.sessionManager
    val authRepository = ServiceLocator.authRepository
    val householdRepository = ServiceLocator.householdRepository
    val dashboardRepository = ServiceLocator.dashboardRepository
    val accountsRepository = ServiceLocator.accountsRepository
    val transactionsRepository = ServiceLocator.transactionsRepository
    val transactionDetailRepository = ServiceLocator.transactionDetailRepository
    val tenantContext = ServiceLocator.tenantContext

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
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Household.route) { inclusive = true }
                }
            }
        }

        composable(Screen.Dashboard.route) {
            val viewModel = DashboardViewModel(dashboardRepository, tenantContext)
            DashboardScreen(
                viewModel = viewModel,
                onAccountsClick = { navController.navigate(Screen.Accounts.route) },
                onTransactionsClick = { navController.navigate(Screen.Transactions.route) }
            )
        }

        composable(Screen.Accounts.route) {
            val viewModel = AccountsViewModel(accountsRepository, tenantContext)
            AccountsScreen(viewModel = viewModel)
        }

        composable(Screen.Transactions.route) {
            val viewModel = TransactionsViewModel(transactionsRepository, tenantContext)
            TransactionsScreen(
                viewModel = viewModel,
                onTransactionClick = { transaction ->
                    navController.navigate(Screen.TransactionDetail.createRoute(transaction.id))
                }
            )
        }

        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            val viewModel = TransactionDetailViewModel(transactionDetailRepository)
            TransactionDetailScreen(
                transactionId = transactionId,
                viewModel = viewModel
            )
        }
    }
}
