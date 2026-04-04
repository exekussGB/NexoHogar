package com.nexohogar.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.core.session.RefreshResult
import com.nexohogar.core.session.TokenRefreshCoordinator
import com.nexohogar.presentation.accounts.AccountsScreen
import com.nexohogar.presentation.accounts.AccountsViewModel
// RED-01: Import desde addmovement (paquete no deprecado)
import com.nexohogar.presentation.addmovement.AddTransactionScreen
import com.nexohogar.presentation.addmovement.AddMovementViewModel
import com.nexohogar.presentation.budget.BudgetScreen
import com.nexohogar.presentation.budget.BudgetViewModel
import com.nexohogar.presentation.categoryexpenses.CategoryExpensesScreen
import com.nexohogar.presentation.categoryexpenses.CategoryExpensesViewModel
import com.nexohogar.presentation.dashboard.DashboardScreen
import com.nexohogar.presentation.dashboard.DashboardViewModel
import com.nexohogar.presentation.household.HouseholdScreen
import com.nexohogar.presentation.household.HouseholdViewModel
import com.nexohogar.presentation.householdmembers.HouseholdMembersScreen
import com.nexohogar.presentation.householdmembers.HouseholdMembersViewModel
import com.nexohogar.presentation.hub.HubScreen
import com.nexohogar.presentation.inventory.InventoryScreen
import com.nexohogar.presentation.inventory.InventoryViewModel
import com.nexohogar.presentation.wishlist.WishlistScreen
import com.nexohogar.presentation.wishlist.WishlistViewModel
import com.nexohogar.presentation.invitemember.InviteMemberScreen
import com.nexohogar.presentation.invitemember.InviteMemberViewModel
import com.nexohogar.presentation.login.LoginScreen
import com.nexohogar.presentation.login.LoginViewModel
import com.nexohogar.presentation.personaldashboard.PersonalDashboardScreen
import com.nexohogar.presentation.personaldashboard.PersonalDashboardViewModel
import com.nexohogar.presentation.recurringbills.RecurringBillsScreen
import com.nexohogar.presentation.recurringbills.RecurringBillsViewModel
import com.nexohogar.presentation.register.RegisterScreen
import com.nexohogar.presentation.register.RegisterViewModel
import com.nexohogar.presentation.settings.SettingsScreen
import com.nexohogar.presentation.transactiondetail.TransactionDetailScreen
import com.nexohogar.presentation.transactiondetail.TransactionDetailViewModel
import com.nexohogar.presentation.transactions.TransactionsScreen
import com.nexohogar.presentation.transactions.TransactionsViewModel
import com.nexohogar.presentation.scanner.ReceiptScannerScreen
import com.nexohogar.presentation.scanner.ReceiptScannerViewModel
import androidx.compose.runtime.remember
import com.nexohogar.presentation.forgotpassword.ForgotPasswordScreen
import com.nexohogar.presentation.forgotpassword.ForgotPasswordViewModel
import com.nexohogar.presentation.forgotpassword.ResetPasswordScreen
import android.net.Uri
import com.nexohogar.presentation.forgotpassword.VerifyOtpScreen
import com.nexohogar.presentation.household.DeleteHouseholdViewModel
import com.nexohogar.core.tutorial.TutorialModule
import com.nexohogar.presentation.tutorial.TutorialListScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nexohogar.core.result.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------
// Rutas de la app
// ---------------------------------------------------------------------------
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Household : Screen("household")
    object Hub : Screen("hub")
    object Dashboard : Screen("dashboard")
    object Accounts : Screen("accounts")
    object Transactions : Screen("transactions")
    object InviteMember : Screen("invite_member")
    object RecurringBills : Screen("recurring_bills")
    object Settings : Screen("settings")
    object HouseholdMembers : Screen("household_members")
    object Inventory : Screen("inventory")
    object Budget : Screen("budget")
    object CategoryExpenses : Screen("category_expenses")
    object PersonalDashboard : Screen("personal_dashboard")

    object ForgotPassword : Screen("forgot_password")
    object TutorialList : Screen("tutorial_list")
    object Wishlist : Screen("wishlist")

    object AddTransaction : Screen("add_transaction/{type}") {
        fun createRoute(type: String) = "add_transaction/$type"
    }
    object TransactionDetail : Screen("transaction_detail/{transactionId}") {
        fun createRoute(transactionId: String) = "transaction_detail/$transactionId"
    }
}

// ---------------------------------------------------------------------------
// NavGraph
// ---------------------------------------------------------------------------
@Composable
fun NavGraph(navController: NavHostController) {
    val sessionManager = ServiceLocator.sessionManager
    val authRepository = ServiceLocator.authRepository
    val householdRepository = ServiceLocator.householdRepository
    val dashboardRepository = ServiceLocator.dashboardRepository
    val accountsRepository = ServiceLocator.accountsRepository
    val transactionsRepository = ServiceLocator.transactionsRepository
    val categoriesRepository = ServiceLocator.categoriesRepository
    val transactionDetailRepository = ServiceLocator.transactionDetailRepository
    val recurringBillsRepository = ServiceLocator.recurringBillsRepository
    val inventoryRepository = ServiceLocator.inventoryRepository
    val budgetRepository = ServiceLocator.budgetRepository
    val categoryExpensesRepository = ServiceLocator.categoryExpensesRepository
    val personalDashboardRepository = ServiceLocator.personalDashboardRepository
    val tenantContext = ServiceLocator.tenantContext
    val tutorialManager = ServiceLocator.tutorialManager

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        // ── Splash (session gate) ────────────────────────────────────────────
        composable(Screen.Splash.route) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cargando...", style = MaterialTheme.typography.bodyMedium)
                }
            }

            LaunchedEffect(Unit) {
                val session = sessionManager.fetchSession()
                if (session == null) {
                    // No session at all → login
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }

                // Try to refresh if expired
                if (sessionManager.isTokenExpired()) {
                    val result = withContext(Dispatchers.IO) {
                        TokenRefreshCoordinator.refresh(sessionManager)
                    }
                    if (!result.isSuccess) {
                        // Refresh failed → clean up and go to login
                        if (result is RefreshResult.ServerRejected) {
                            sessionManager.clearSession()
                        }
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                        return@LaunchedEffect
                    }
                }

                // Token valid → go to household
                navController.navigate(Screen.Household.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }

        // ── Login ──────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            val vm = LoginViewModel(authRepository, sessionManager)
            LoginScreen(
                viewModel = vm,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Household.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        // ── Forgot Password ───────────────────────────────────────────────────
        composable(Screen.ForgotPassword.route) {
            val vm = ForgotPasswordViewModel(authRepository)
            ForgotPasswordScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVerifyOtp = { email ->
                    navController.navigate("verify_otp/${Uri.encode(email)}")
                }
            )
        }
        // ── Register ───────────────────────────────────────────────────────
        composable(Screen.Register.route) {
            val vm = RegisterViewModel(authRepository, sessionManager)
            RegisterScreen(
                viewModel = vm,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Household.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }
        // ── Settings ───────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            val deleteHouseholdViewModel = remember {
                DeleteHouseholdViewModel(householdRepository)
            }
            SettingsScreen(
                sessionManager = sessionManager,
                deleteHouseholdViewModel = deleteHouseholdViewModel,
                householdRepository = householdRepository,
                tenantContext = tenantContext,
                tutorialManager = tutorialManager,
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onChangeHousehold = {
                    navController.navigate(Screen.Household.route) {
                        popUpTo(Screen.Household.route) { inclusive = true }
                    }
                },
                onViewMembers = {
                    navController.navigate(Screen.HouseholdMembers.route)
                },
                onNavigateToTutorial = {
                    navController.navigate(Screen.TutorialList.route)
                },
                onHouseholdDeleted = {
                    navController.navigate(Screen.Household.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        // ── Tutorial List ──────────────────────────────────────────────────
        composable(Screen.TutorialList.route) {
            TutorialListScreen(
                tutorialManager = tutorialManager,
                onStartTutorial = { module ->
                    navController.popBackStack()
                    when (module) {
                        TutorialModule.DASHBOARD -> navController.navigate(Screen.Dashboard.route)
                        TutorialModule.ACCOUNTS -> navController.navigate(Screen.Accounts.route)
                        TutorialModule.TRANSACTIONS -> navController.navigate(Screen.Transactions.route)
                        TutorialModule.BUDGETS -> navController.navigate(Screen.Budget.route)
                        TutorialModule.INVENTORY -> navController.navigate(Screen.Inventory.route)
                        TutorialModule.WISHLIST -> navController.navigate(Screen.Wishlist.route)
                        TutorialModule.RECURRING_BILLS -> navController.navigate(Screen.RecurringBills.route)
                        TutorialModule.HOUSEHOLD -> navController.navigate(Screen.Settings.route)
                        TutorialModule.INVITE_MEMBER -> navController.navigate(Screen.InviteMember.route)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Household ──────────────────────────────────────────────────────
        composable(Screen.Household.route) {
            val vm = HouseholdViewModel(householdRepository, tenantContext)
            HouseholdScreen(
                viewModel = vm,
                onHouseholdSelected = { householdId ->
                    tenantContext.setHouseholdId(householdId)
                    navController.navigate(Screen.Hub.route) {
                        popUpTo(Screen.Household.route) { inclusive = true }
                    }
                },
                onSessionExpired = {
                    // La sesión expiró irrecuperablemente — limpiar back stack y volver al login
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Hub ────────────────────────────────────────────────────────────
        composable(Screen.Hub.route) {
            var hubHouseholdName by remember { mutableStateOf("") }
            val hubHouseholdId = remember { tenantContext.getCurrentHouseholdId() ?: "" }
            LaunchedEffect(hubHouseholdId) {
                if (hubHouseholdId.isNotBlank()) {
                    when (val result = householdRepository.getHouseholds()) {
                        is AppResult.Success -> {
                            hubHouseholdName = result.data
                                .firstOrNull { it.id == hubHouseholdId }?.name ?: ""
                        }
                        else -> {}
                    }
                }
            }
            HubScreen(
                householdName = hubHouseholdName,
                onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) },
                onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                onNavigateToAddMovement = { type -> navController.navigate(Screen.AddTransaction.createRoute(type)) },
                onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                onNavigateToInviteMember = { navController.navigate(Screen.InviteMember.route) },
                onNavigateToRecurringBills = { navController.navigate(Screen.RecurringBills.route) },
                onNavigateToBudget = { navController.navigate(Screen.Budget.route) },
                onNavigateToInventory = { navController.navigate(Screen.Inventory.route) },
                onNavigateToWishlist = { navController.navigate(Screen.Wishlist.route) },
                onNavigateToOptions = { navController.navigate(Screen.Settings.route) }
            )
        }

        // ── Dashboard (Fondo Común) ────────────────────────────────────────
        composable(Screen.Dashboard.route) {
            val vm: DashboardViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return DashboardViewModel(
                            dashboardRepository,
                            transactionsRepository,
                            accountsRepository,
                            tenantContext
                        ) as T
                    }
                }
            )
            DashboardScreen(
                viewModel = vm,
                tutorialManager = tutorialManager,
                onTransactionClick = { id -> navController.navigate(Screen.TransactionDetail.createRoute(id)) },
                onSeeAllClick = { navController.navigate(Screen.Transactions.route) },
                onNavigateToCategoryExp = { navController.navigate(Screen.CategoryExpenses.route) },
                onNavigateToPersonal = { navController.navigate(Screen.PersonalDashboard.route) }
            )
        }

        // ── Accounts ───────────────────────────────────────────────────────
        composable(Screen.Accounts.route) {
            val vm = AccountsViewModel(accountsRepository, transactionsRepository, tenantContext)
            AccountsScreen(
                viewModel = vm,
                tutorialManager = tutorialManager,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Transactions ───────────────────────────────────────────────────
        composable(Screen.Transactions.route) {
            val vm = TransactionsViewModel(transactionsRepository, tenantContext)
            TransactionsScreen(
                viewModel = vm,
                tutorialManager = tutorialManager,
                onTransactionClick = { t -> navController.navigate(Screen.TransactionDetail.createRoute(t.id)) },
                onAddTransactionClick = { navController.navigate(Screen.AddTransaction.createRoute("expense")) }
            )
        }

        // ── AddTransaction ─────────────────────────────────────────────────
        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "expense"
            val vm: AddMovementViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return AddMovementViewModel(
                            transactionsRepository,
                            categoriesRepository,
                            recurringBillsRepository,
                            tenantContext
                        ) as T
                    }
                }
            )
            AddTransactionScreen(
                transactionType = type,
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── TransactionDetail ──────────────────────────────────────────────
        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            val vm: TransactionDetailViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return TransactionDetailViewModel(transactionDetailRepository) as T
                    }
                }
            )
            TransactionDetailScreen(
                transactionId = transactionId,
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── InviteMember ───────────────────────────────────────────────────
        composable(Screen.InviteMember.route) {
            val vm = InviteMemberViewModel(householdRepository, tenantContext)
            InviteMemberScreen(
                viewModel = vm,
                tutorialManager = tutorialManager,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── RecurringBills ─────────────────────────────────────────────────
        composable(Screen.RecurringBills.route) {
            val vm: RecurringBillsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return RecurringBillsViewModel(recurringBillsRepository, accountsRepository, tenantContext) as T
                    }
                }
            )
            RecurringBillsScreen(
                viewModel = vm,
                tutorialManager = tutorialManager,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── HouseholdMembers ───────────────────────────────────────────────
        composable(Screen.HouseholdMembers.route) {
            val vm = HouseholdMembersViewModel(householdRepository, tenantContext)
            HouseholdMembersScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Wishlist ───────────────────────────────────────────────────────
        composable(Screen.Wishlist.route) {
            val vm = WishlistViewModel(
                repository = ServiceLocator.wishlistRepository,
                tenantContext = ServiceLocator.tenantContext,
                sessionManager = ServiceLocator.sessionManager
            )
            WishlistScreen(
                viewModel = vm,
                tutorialManager = ServiceLocator.tutorialManager,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Inventory ──────────────────────────────────────────────────────
        composable(Screen.Inventory.route) {
            val vm: InventoryViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return InventoryViewModel(inventoryRepository, tenantContext) as T
                    }
                }
            )
            InventoryScreen(
                viewModel = vm,
                tutorialManager = tutorialManager,
                onBack = { navController.popBackStack() },
                onNavigateToScanner = { navController.navigate("receipt_scanner") }
            )
        }

        // ── Budget ─────────────────────────────────────────────────────────
        composable(Screen.Budget.route) {
            val vm: BudgetViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return BudgetViewModel(budgetRepository, tenantContext) as T
                    }
                }
            )
            BudgetScreen(
                viewModel = vm,
                tutorialManager = tutorialManager,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── CategoryExpenses ───────────────────────────────────────────────
        composable(Screen.CategoryExpenses.route) {
            val vm: CategoryExpensesViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return CategoryExpensesViewModel(categoryExpensesRepository, tenantContext) as T
                    }
                }
            )
            CategoryExpensesScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── PersonalDashboard ──────────────────────────────────────────────
        composable(Screen.PersonalDashboard.route) {
            val vm: PersonalDashboardViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return PersonalDashboardViewModel(
                            personalDashboardRepository,
                            tenantContext,
                            sessionManager
                        ) as T
                    }
                }
            )
            PersonalDashboardScreen(
                viewModel = vm,
                onTransactionClick = { id -> navController.navigate(Screen.TransactionDetail.createRoute(id)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Receipt Scanner ────────────────────────────────────────────────
        composable("receipt_scanner") {
            val viewModel = remember {
                ReceiptScannerViewModel(
                    inventoryRepository = ServiceLocator.inventoryRepository,
                    accountsRepository = ServiceLocator.accountsRepository,
                    tenantContext = ServiceLocator.tenantContext
                )
            }
            ReceiptScannerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── SEC-05: Reset Password ahora recibe token como argumento ──────
        composable(
            "reset_password/{accessToken}",
            arguments = listOf(navArgument("accessToken") { type = NavType.StringType })
        ) { backStackEntry ->
            val accessToken = Uri.decode(backStackEntry.arguments?.getString("accessToken") ?: "")
            ResetPasswordScreen(
                accessToken = accessToken,
                onResetSuccess = {
                    navController.navigate("login") {
                        popUpTo("reset_password/{accessToken}") { inclusive = true }
                    }
                }
            )
        }

        // ── SEC-05: Verify OTP ahora pasa token al navegar ────────────────
        composable(
            "verify_otp/{email}",
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = Uri.decode(backStackEntry.arguments?.getString("email") ?: "")
            VerifyOtpScreen(
                email = email,
                onVerified = { accessToken ->
                    navController.navigate("reset_password/${Uri.encode(accessToken)}") {
                        popUpTo("verify_otp/{email}") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
