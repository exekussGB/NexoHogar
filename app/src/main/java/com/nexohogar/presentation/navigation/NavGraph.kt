package com.nexohogar.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.presentation.accounts.AccountsScreen
import com.nexohogar.presentation.accounts.AccountsViewModel
import com.nexohogar.presentation.addtransaction.AddTransactionScreen
import com.nexohogar.presentation.addmovement.AddMovementViewModel
import com.nexohogar.presentation.dashboard.DashboardScreen
import com.nexohogar.presentation.dashboard.DashboardViewModel
import com.nexohogar.presentation.household.HouseholdScreen
import com.nexohogar.presentation.household.HouseholdViewModel
import com.nexohogar.presentation.hub.HubScreen
import com.nexohogar.presentation.invitemember.InviteMemberScreen
import com.nexohogar.presentation.login.LoginScreen
import com.nexohogar.presentation.login.LoginViewModel
import com.nexohogar.presentation.register.RegisterScreen
import com.nexohogar.presentation.register.RegisterViewModel
import com.nexohogar.presentation.transactiondetail.TransactionDetailScreen
import com.nexohogar.presentation.transactiondetail.TransactionDetailViewModel
import com.nexohogar.presentation.transactions.TransactionsScreen
import com.nexohogar.presentation.transactions.TransactionsViewModel

// ---------------------------------------------------------------------------
// Rutas de la app
// ---------------------------------------------------------------------------
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Household : Screen("household")
    object Hub : Screen("hub")                          // ← NUEVO: menú principal
    object Dashboard : Screen("dashboard")
    object Accounts : Screen("accounts")
    object Transactions : Screen("transactions")
    object InviteMember : Screen("invite_member")       // ← NUEVO: invitar miembro

    object AddTransaction : Screen("add_transaction/{type}") {
        fun createRoute(type: String) = "add_transaction/$type"
    }

    object TransactionDetail : Screen("transaction_detail/{transactionId}") {
        fun createRoute(transactionId: String) = "transaction_detail/$transactionId"
    }
}

// ---------------------------------------------------------------------------
// Grafo de navegación
// ---------------------------------------------------------------------------
@Composable
fun NavGraph(
    navController: NavHostController
) {
    val sessionManager            = ServiceLocator.sessionManager
    val authRepository            = ServiceLocator.authRepository
    val householdRepository       = ServiceLocator.householdRepository
    val dashboardRepository       = ServiceLocator.dashboardRepository
    val accountsRepository        = ServiceLocator.accountsRepository
    val transactionsRepository    = ServiceLocator.transactionsRepository
    val categoriesRepository      = ServiceLocator.categoriesRepository
    val transactionDetailRepository = ServiceLocator.transactionDetailRepository
    val tenantContext             = ServiceLocator.tenantContext

    val startDestination =
        if (sessionManager.fetchAuthToken() != null) Screen.Household.route
        else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        // ------------------------------------------------------------------ Login
        composable(Screen.Login.route) {
            val viewModel = LoginViewModel(authRepository, sessionManager)
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Household.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ------------------------------------------------------------------ Register
        composable(Screen.Register.route) {
            val viewModel = RegisterViewModel(authRepository, sessionManager)
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Household.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        // ------------------------------------------------------------------ Household
        composable(Screen.Household.route) {
            val viewModel = HouseholdViewModel(
                householdRepository = householdRepository,
                tenantContext = tenantContext
            )
            HouseholdScreen(viewModel = viewModel) { householdId ->
                tenantContext.setHouseholdId(householdId)
                // Después de seleccionar hogar → HUB (no directo al Dashboard)
                navController.navigate(Screen.Hub.route) {
                    popUpTo(Screen.Household.route) { inclusive = true }
                }
            }
        }

        // ------------------------------------------------------------------ Hub (menú principal)
        composable(Screen.Hub.route) {
            HubScreen(
                householdName = "",   // TODO: guardar nombre en TenantContext/SessionManager si se necesita
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route)
                },
                onNavigateToTransactions = {
                    navController.navigate(Screen.Transactions.route)
                },
                onNavigateToAddMovement = { type ->
                    navController.navigate(Screen.AddTransaction.createRoute(type))
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Accounts.route)
                },
                onNavigateToInviteMember = {
                    navController.navigate(Screen.InviteMember.route)
                },
                onNavigateToOptions = {
                    // TODO: implementar pantalla de opciones
                }
            )
        }

        // ------------------------------------------------------------------ Dashboard
        composable(Screen.Dashboard.route) {
            val viewModel: DashboardViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return DashboardViewModel(
                            dashboardRepository,
                            transactionsRepository,
                            tenantContext
                        ) as T
                    }
                }
            )
            DashboardScreen(
                viewModel = viewModel,
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetail.createRoute(transactionId))
                },
                onSeeAllClick = {
                    navController.navigate(Screen.Transactions.route)
                },
                onCreateTransaction = { type ->
                    navController.navigate(Screen.AddTransaction.createRoute(type))
                }
            )
        }

        // ------------------------------------------------------------------ Accounts
        composable(Screen.Accounts.route) {
            val viewModel = AccountsViewModel(accountsRepository, tenantContext)
            AccountsScreen(viewModel = viewModel)
        }

        // ------------------------------------------------------------------ Transactions
        composable(Screen.Transactions.route) {
            val viewModel = TransactionsViewModel(transactionsRepository, tenantContext)
            TransactionsScreen(
                viewModel = viewModel,
                onTransactionClick = { transaction ->
                    navController.navigate(
                        Screen.TransactionDetail.createRoute(transaction.id)
                    )
                },
                onAddTransactionClick = {
                    navController.navigate(Screen.AddTransaction.createRoute("expense"))
                }
            )
        }

        // ------------------------------------------------------------------ AddTransaction
        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "expense"
            val viewModel: AddMovementViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return AddMovementViewModel(
                            transactionsRepository,
                            categoriesRepository,
                            tenantContext
                        ) as T
                    }
                }
            )
            AddTransactionScreen(
                transactionType = type,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ------------------------------------------------------------------ TransactionDetail
        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            val viewModel: TransactionDetailViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return TransactionDetailViewModel(transactionDetailRepository) as T
                    }
                }
            )
            TransactionDetailScreen(
                transactionId = transactionId,
                viewModel = viewModel
            )
        }

        // ------------------------------------------------------------------ InviteMember
        composable(Screen.InviteMember.route) {
            InviteMemberScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
