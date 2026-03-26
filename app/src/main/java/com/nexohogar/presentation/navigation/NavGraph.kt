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
import com.nexohogar.presentation.budgets.BudgetsScreen
import com.nexohogar.presentation.budgets.BudgetsViewModel
import com.nexohogar.presentation.dashboard.DashboardScreen
import com.nexohogar.presentation.dashboard.DashboardViewModel
import com.nexohogar.presentation.expenses.ExpenseByCategoryScreen
import com.nexohogar.presentation.expenses.ExpenseByCategoryViewModel
import com.nexohogar.presentation.household.HouseholdScreen
import com.nexohogar.presentation.household.HouseholdViewModel
import com.nexohogar.presentation.householdmembers.HouseholdMembersScreen
import com.nexohogar.presentation.householdmembers.HouseholdMembersViewModel
import com.nexohogar.presentation.hub.HubScreen
import com.nexohogar.presentation.inventory.InventoryScreen
import com.nexohogar.presentation.inventory.InventoryViewModel
import com.nexohogar.presentation.invitemember.InviteMemberScreen
import com.nexohogar.presentation.invitemember.InviteMemberViewModel
import com.nexohogar.presentation.login.LoginScreen
import com.nexohogar.presentation.login.LoginViewModel
import com.nexohogar.presentation.recurringbills.RecurringBillsScreen
import com.nexohogar.presentation.recurringbills.RecurringBillsViewModel
import com.nexohogar.presentation.register.RegisterScreen
import com.nexohogar.presentation.register.RegisterViewModel
import com.nexohogar.presentation.settings.SettingsScreen
import com.nexohogar.presentation.transactiondetail.TransactionDetailScreen
import com.nexohogar.presentation.transactiondetail.TransactionDetailViewModel
import com.nexohogar.presentation.transactions.TransactionsScreen
import com.nexohogar.presentation.transactions.TransactionsViewModel

// ---------------------------------------------------------------------------
// Rutas de la app
// ---------------------------------------------------------------------------
sealed class Screen(val route: String) {
    object Login              : Screen("login")
    object Register           : Screen("register")
    object Household          : Screen("household")
    object Hub                : Screen("hub")
    object Dashboard          : Screen("dashboard")
    object Accounts           : Screen("accounts")
    object Transactions       : Screen("transactions")
    object InviteMember       : Screen("invite_member")
    object RecurringBills     : Screen("recurring_bills")
    object Settings           : Screen("settings")
    object HouseholdMembers   : Screen("household_members")
    object Inventory          : Screen("inventory")
    object Budgets            : Screen("budgets")
    object ExpensesByCategory : Screen("expenses_by_category")

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
    val sessionManager              = ServiceLocator.sessionManager
    val authRepository              = ServiceLocator.authRepository
    val householdRepository         = ServiceLocator.householdRepository
    val dashboardRepository         = ServiceLocator.dashboardRepository
    val accountsRepository          = ServiceLocator.accountsRepository
    val transactionsRepository      = ServiceLocator.transactionsRepository
    val categoriesRepository        = ServiceLocator.categoriesRepository
    val transactionDetailRepository = ServiceLocator.transactionDetailRepository
    val recurringBillsRepository    = ServiceLocator.recurringBillsRepository
    val inventoryRepository         = ServiceLocator.inventoryRepository
    val budgetsRepository           = ServiceLocator.budgetsRepository
    val tenantContext               = ServiceLocator.tenantContext

    val startDestination =
        if (sessionManager.fetchAuthToken() != null) Screen.Household.route
        else Screen.Login.route

    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {

        // ── Login ──────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            val vm = LoginViewModel(authRepository, sessionManager)
            LoginScreen(
                viewModel            = vm,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess       = {
                    navController.navigate(Screen.Household.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Register ───────────────────────────────────────────────────────
        composable(Screen.Register.route) {
            val vm = RegisterViewModel(authRepository, sessionManager)
            RegisterScreen(
                viewModel         = vm,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Household.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Household ──────────────────────────────────────────────────────
        composable(Screen.Household.route) {
            val vm = HouseholdViewModel(
                householdRepository = householdRepository,
                tenantContext       = tenantContext
            )
            HouseholdScreen(viewModel = vm) { householdId ->
                tenantContext.setHouseholdId(householdId)
                navController.navigate(Screen.Hub.route) {
                    popUpTo(Screen.Household.route) { inclusive = true }
                }
            }
        }

        // ── Hub (menú principal) ───────────────────────────────────────────
        composable(Screen.Hub.route) {
            HubScreen(
                householdName              = "",
                onNavigateToDashboard      = { navController.navigate(Screen.Dashboard.route) },
                onNavigateToTransactions   = { navController.navigate(Screen.Transactions.route) },
                onNavigateToAddMovement    = { type -> navController.navigate(Screen.AddTransaction.createRoute(type)) },
                onNavigateToAccounts       = { navController.navigate(Screen.Accounts.route) },
                onNavigateToInviteMember   = { navController.navigate(Screen.InviteMember.route) },
                onNavigateToRecurringBills = { navController.navigate(Screen.RecurringBills.route) },
                onNavigateToInventory      = { navController.navigate(Screen.Inventory.route) },
                onNavigateToBudgets        = { navController.navigate(Screen.Budgets.route) },
                onNavigateToOptions        = { navController.navigate(Screen.Settings.route) }
            )
        }

        // ── Dashboard ──────────────────────────────────────────────────────
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
                viewModel                = vm,
                onTransactionClick       = { id -> navController.navigate(Screen.TransactionDetail.createRoute(id)) },
                onSeeAllClick            = { navController.navigate(Screen.Transactions.route) },
                onNavigateToExpensesByCategory = { navController.navigate(Screen.ExpensesByCategory.route) },
                onNavigateToPersonalSummary = { navController.navigate(Screen.Accounts.route) }
            )
        }

        // ── ExpensesByCategory ─────────────────────────────────────────────
        composable(Screen.ExpensesByCategory.route) {
            val vm: ExpenseByCategoryViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ExpenseByCategoryViewModel(
                            budgetsRepository,
                            householdRepository,
                            tenantContext
                        ) as T
                    }
                }
            )
            ExpenseByCategoryScreen(
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Budgets ────────────────────────────────────────────────────────
        composable(Screen.Budgets.route) {
            val vm: BudgetsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return BudgetsViewModel(
                            budgetsRepository,
                            categoriesRepository,
                            tenantContext
                        ) as T
                    }
                }
            )
            BudgetsScreen(
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Accounts ───────────────────────────────────────────────────────
        composable(Screen.Accounts.route) {
            val vm = AccountsViewModel(accountsRepository, tenantContext)
            AccountsScreen(
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Transactions ───────────────────────────────────────────────────
        composable(Screen.Transactions.route) {
            val vm = TransactionsViewModel(transactionsRepository, tenantContext)
            TransactionsScreen(
                viewModel             = vm,
                onTransactionClick    = { t -> navController.navigate(Screen.TransactionDetail.createRoute(t.id)) },
                onAddTransactionClick = { navController.navigate(Screen.AddTransaction.createRoute("expense")) }
            )
        }

        // ── AddTransaction ─────────────────────────────────────────────────
        composable(
            route     = Screen.AddTransaction.route,
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
                viewModel       = vm,
                onNavigateBack  = { navController.popBackStack() }
            )
        }

        // ── TransactionDetail ──────────────────────────────────────────────
        composable(
            route     = Screen.TransactionDetail.route,
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
                transactionId  = transactionId,
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── InviteMember ───────────────────────────────────────────────────
        composable(Screen.InviteMember.route) {
            val vm = InviteMemberViewModel(householdRepository, tenantContext)
            InviteMemberScreen(
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── RecurringBills ─────────────────────────────────────────────────
        composable(Screen.RecurringBills.route) {
            val vm: RecurringBillsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return RecurringBillsViewModel(recurringBillsRepository, tenantContext) as T
                    }
                }
            )
            RecurringBillsScreen(
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Settings ───────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(
                sessionManager    = sessionManager,
                onNavigateBack    = { navController.popBackStack() },
                onLogout          = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onChangeHousehold = {
                    navController.navigate(Screen.Household.route) {
                        popUpTo(Screen.Hub.route) { inclusive = true }
                    }
                },
                onViewMembers     = { navController.navigate(Screen.HouseholdMembers.route) }
            )
        }

        // ── HouseholdMembers ───────────────────────────────────────────────
        composable(Screen.HouseholdMembers.route) {
            val vm = HouseholdMembersViewModel(householdRepository, tenantContext)
            HouseholdMembersScreen(
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Inventory ─────────────────────────────────────────────────────
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
                onBack    = { navController.popBackStack() }
            )
        }
    }
}
