package com.nexohogar.presentation.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.nexohogar.core.di.ServiceLocator
import androidx.compose.runtime.mutableIntStateOf
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tutorial.TutorialModule
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
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
import com.nexohogar.presentation.forgotpassword.ForgotPasswordScreen
import com.nexohogar.presentation.forgotpassword.ForgotPasswordViewModel
import com.nexohogar.presentation.forgotpassword.ResetPasswordScreen
import com.nexohogar.presentation.forgotpassword.VerifyOtpScreen
import com.nexohogar.presentation.household.DeleteHouseholdViewModel
import com.nexohogar.presentation.household.HouseholdScreen
import com.nexohogar.presentation.household.HouseholdViewModel
import com.nexohogar.presentation.householdmembers.HouseholdMembersScreen
import com.nexohogar.presentation.householdmembers.HouseholdMembersViewModel
import com.nexohogar.presentation.hub.AddMovementDialog
import com.nexohogar.presentation.hub.HubScreen
import com.nexohogar.presentation.inventory.InventoryScreen
import com.nexohogar.presentation.inventory.InventoryViewModel
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
import com.nexohogar.presentation.scanner.ReceiptScannerScreen
import com.nexohogar.presentation.scanner.ReceiptScannerViewModel
import com.nexohogar.presentation.settings.SettingsScreen
import com.nexohogar.presentation.transactiondetail.TransactionDetailScreen
import com.nexohogar.presentation.transactiondetail.TransactionDetailViewModel
import com.nexohogar.presentation.transactions.TransactionsScreen
import com.nexohogar.presentation.transactions.TransactionsViewModel
import com.nexohogar.presentation.tutorial.TutorialListScreen
import com.nexohogar.presentation.wishlist.WishlistScreen
import com.nexohogar.presentation.wishlist.WishlistViewModel
import kotlinx.coroutines.flow.first
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
    object TransactionDetail : Screen("transaction_detail/{transactionId}?edit={edit}") {
        fun createRoute(transactionId: String, edit: Boolean = false) = "transaction_detail/$transactionId?edit=$edit"
    }
}

// ---------------------------------------------------------------------------
// Routes where BottomBar should be hidden
// (auth screens + Hub — the bar only appears after selecting a module)
// ---------------------------------------------------------------------------
private val hiddenBarRoutes = setOf(
    Screen.Splash.route,
    Screen.Login.route,
    Screen.Register.route,
    Screen.ForgotPassword.route,
    "verify_otp/{email}",
    "reset_password/{accessToken}",
    Screen.Household.route,
    Screen.Hub.route              // ← Bottom bar hidden on Hub
)

// ---------------------------------------------------------------------------
// NavGraph
// ---------------------------------------------------------------------------
@Composable
fun NavGraph(navController: NavHostController) {
    val sessionManager = ServiceLocator.sessionManager
    val supabaseClient = ServiceLocator.supabaseClient
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
    val wishlistRepository = ServiceLocator.wishlistRepository

    // Determine if BottomBar should be shown based on current route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != null && currentRoute !in hiddenBarRoutes

    // ── Estado del diálogo "Agregar Movimiento" del botón "+" ────────────
    var showAddMovementDialog by remember { mutableStateOf(false) }
    // Badge counts para BottomBar y HubScreen
    var overdueCount       by remember { mutableIntStateOf(0) }
    var budgetAlertCount   by remember { mutableIntStateOf(0) }
    var lowStockCount      by remember { mutableIntStateOf(0) }
    var wishlistHighCount  by remember { mutableIntStateOf(0) }
    var availableBalance   by remember { mutableStateOf(0.0) }
    var hubAlertCount      by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentRoute) {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return@LaunchedEffect
        // Facturas vencidas o por vencer en 3 dias
        try {
            when (val r = recurringBillsRepository.getBillsWithStatus(householdId)) {
                is AppResult.Success -> overdueCount = r.data.count {
                    it.isActive && !it.isPaidThisMonth &&
                    (it.isOverdue || (it.daysUntilDue != null && it.daysUntilDue <= 3))
                }
                else -> {}
            }
        } catch (_: Exception) {}
        // Presupuestos al 80% o mas
        try {
            when (val r = budgetRepository.getBudgetConsumption(householdId)) {
                is AppResult.Success -> budgetAlertCount = r.data.count { item ->
                    item.budgetedAmount > 0 &&
                    item.spentAmount.toDouble() / item.budgetedAmount >= 0.8
                }
                else -> {}
            }
        } catch (_: Exception) {}
        // Productos con stock bajo o en minimo
        try {
            when (val r = inventoryRepository.getProducts(householdId)) {
                is AppResult.Success -> lowStockCount = r.data.count { p ->
                    val min = (p.minStock?.toDouble()) ?: 1.0
                    p.currentStock in 0.0..min
                }
                else -> {}
            }
        } catch (_: Exception) {}
        // Items de alta prioridad pendientes en wishlist
        try {
            when (val r = wishlistRepository.getWishlistItems(householdId)) {
                is AppResult.Success -> wishlistHighCount = r.data.count {
                    it.priority == "HIGH" && !it.isPurchased
                }
                else -> {}
            }
        } catch (_: Exception) {}
        // Balance disponible (para chip en Wishlist)
        try {
            when (val r = accountsRepository.getAccountBalances(householdId)) {
                is AppResult.Success -> availableBalance = r.data.sumOf { it.movementBalance.toDouble() }
                else -> {}
            }
        } catch (_: Exception) {}
        // Tutoriales pendientes
        hubAlertCount = try {
            com.nexohogar.core.tutorial.TutorialModule.values()
                .count { !tutorialManager.isTutorialCompleted(it) }
        } catch (_: Exception) { 0 }
    }


    if (showAddMovementDialog) {
        AddMovementDialog(
            onDismiss = { showAddMovementDialog = false },
            onSelect  = { type ->
                showAddMovementDialog = false
                navController.navigate(Screen.AddTransaction.createRoute(type))
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NexoHogarBottomNavBar(
                    currentRoute = currentRoute,
                    lowStockCount = lowStockCount,
                    hubTotalAlerts = overdueCount + budgetAlertCount + wishlistHighCount + hubAlertCount,
                    onNavigate = { route ->
                        if (route == Screen.Hub.route) {
                            // "Más" SIEMPRE navega al Hub limpiamente
                            navController.navigate(Screen.Hub.route) {
                                launchSingleTop = true
                                // Limpiar todo el back stack hasta Household
                                popUpTo(Screen.Hub.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(route) {
                                launchSingleTop = true
                                popUpTo(Screen.Hub.route) { saveState = true }
                                restoreState = true
                            }
                        }
                    },
                    onAddMovement = { showAddMovementDialog = true },
                    onShowTransactionTypeDialog = { showAddMovementDialog = true }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            // ── Splash (session gate + biometric + refresh) ─────────────────────
            composable(Screen.Splash.route) {
                val context = LocalContext.current
                val biometricHelper = ServiceLocator.biometricHelper
                val sessionRefresher = ServiceLocator.sessionRefresher

                // UI states: loading, biometric, biometric_failed, refreshing, network_error, session_expired
                var splashPhase by remember { mutableStateOf("loading") }
                var isRetrying by remember { mutableStateOf(false) }

                // Biometric callback handler
                val biometricSuccess = remember { mutableStateOf(false) }
                val biometricAttempted = remember { mutableStateOf(false) }

                when (splashPhase) {
                    "biometric" -> {
                        // Show biometric prompt UI
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Verificación biométrica", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Verifica tu identidad para continuar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    "biometric_failed" -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔒", style = MaterialTheme.typography.displayMedium)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Verificación fallida", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(onClick = { splashPhase = "biometric"; biometricAttempted.value = false }) {
                                    Text("Reintentar")
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(onClick = {
                                    sessionManager.clearSession()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                }) {
                                    Text("Usar contraseña")
                                }
                            }
                        }
                    }
                    "network_error" -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("😕", style = MaterialTheme.typography.displayMedium)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Sin conexión", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No se pudo verificar tu sesión.\nRevisa tu conexión a internet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(onClick = { splashPhase = "refreshing"; isRetrying = true }) {
                                    Text("Reintentar")
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(onClick = {
                                    sessionManager.clearSession()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                }) {
                                    Text("Iniciar sesión")
                                }
                            }
                        }
                    }
                    "session_expired" -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⏰", style = MaterialTheme.typography.displayMedium)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Tu sesión expiró", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Por seguridad, necesitas iniciar sesión nuevamente.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(onClick = { splashPhase = "refreshing"; isRetrying = true }) {
                                    Text("Reintentar")
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(onClick = {
                                    sessionManager.clearSession()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                }) {
                                    Text("Iniciar sesión")
                                }
                            }
                        }
                    }
                    else -> {
                        // loading / refreshing
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
                    }
                }

                // ── Main navigation logic ──
                // supabase-kt carga la sesión desde DataStore y la refresca automáticamente.
                // Solo esperamos a que el SDK termine de inicializar, luego decidimos la ruta.
                LaunchedEffect(splashPhase, isRetrying) {
                    if (splashPhase == "biometric" || splashPhase == "biometric_failed") return@LaunchedEffect
                    if (isRetrying) isRetrying = false

                    // Esperar a que el SDK cargue la sesión guardada (DataStore)
                    val status = supabaseClient.auth.sessionStatus
                        .first { it !is SessionStatus.Initializing }

                    if (status !is SessionStatus.Authenticated) {
                        Log.d("Splash", "❌ No hay sesión autenticada → Login")
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                        return@LaunchedEffect
                    }

                    Log.d("Splash", "✅ Sesión activa restaurada por supabase-kt")

                    // Verificación biométrica si está habilitada
                    if (sessionManager.isBiometricEnabled() && !biometricSuccess.value && !biometricAttempted.value) {
                        splashPhase = "biometric"
                        return@LaunchedEffect
                    }

                    navController.navigate(Screen.Household.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }

                // ── Biometric launcher ──
                LaunchedEffect(splashPhase) {
                    if (splashPhase == "biometric" && !biometricAttempted.value) {
                        biometricAttempted.value = true
                        val activity = context as? androidx.fragment.app.FragmentActivity
                        if (activity != null && biometricHelper.isBiometricAvailable()) {
                            biometricHelper.showBiometricPrompt(
                                activity = activity,
                                onSuccess = {
                                    biometricSuccess.value = true
                                    splashPhase = "loading" // re-trigger the main logic
                                },
                                onError = { _ ->
                                    splashPhase = "biometric_failed"
                                }
                            )
                        } else {
                            // Biometric not available — skip
                            biometricSuccess.value = true
                            splashPhase = "loading"
                        }
                    }
                }
            }

            // ── Login ──────────────────────────────────────────────────────────
            composable(Screen.Login.route) {
                val vm = LoginViewModel(authRepository, sessionManager)
                val biometricHelper = ServiceLocator.biometricHelper
                val offerBiometric = biometricHelper.isBiometricAvailable() && sessionManager.shouldOfferBiometric()
                val context = LocalContext.current
                var showBiometricDialog by remember { mutableStateOf(false) }

                // ── Diálogo: ¿Activar biometría? ──
                if (showBiometricDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = {
                            // Si descarta el diálogo, continúa sin biometría
                            showBiometricDialog = false
                            navController.navigate(Screen.Household.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = { Text("Inicio rápido") },
                        text = {
                            Text(
                                "¿Quieres activar el inicio de sesión con huella digital o reconocimiento facial?\n\n" +
                                        "La próxima vez podrás ingresar sin escribir tu contraseña."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showBiometricDialog = false
                                val activity = context as? androidx.fragment.app.FragmentActivity
                                if (activity != null) {
                                    biometricHelper.showBiometricPrompt(
                                        activity = activity,
                                        onSuccess = {
                                            // Biometría confirmada → habilitar y continuar
                                            sessionManager.setBiometricEnabled(true)
                                            navController.navigate(Screen.Household.route) {
                                                popUpTo(Screen.Login.route) { inclusive = true }
                                            }
                                        },
                                        onError = { _ ->
                                            // Falló la verificación → continuar sin habilitar
                                            navController.navigate(Screen.Household.route) {
                                                popUpTo(Screen.Login.route) { inclusive = true }
                                            }
                                        }
                                    )
                                } else {
                                    navController.navigate(Screen.Household.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            }) { Text("Activar") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showBiometricDialog = false
                                navController.navigate(Screen.Household.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }) { Text("Ahora no") }
                        }
                    )
                }

                LoginScreen(
                    viewModel = vm,
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                    onLoginSuccess = {
                        if (biometricHelper.isBiometricAvailable() && !sessionManager.isBiometricEnabled()) {
                            // Primera vez con biometría disponible → preguntar al usuario
                            showBiometricDialog = true
                        } else {
                            // Ya tiene biometría habilitada o no disponible → continuar directo
                            navController.navigate(Screen.Household.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    },
                    showBiometric = offerBiometric,
                    onBiometricLogin = {
                        val activity = context as? androidx.fragment.app.FragmentActivity
                        if (activity != null) {
                            biometricHelper.showBiometricPrompt(
                                activity = activity,
                                onSuccess = {
                                    // Biometric OK → navigate to household selection
                                    navController.navigate(Screen.Household.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                },
                                onError = { errorMsg ->
                                    android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
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
                        // Navegar al Hub tras seleccionar hogar (la bottom bar aparecerá al elegir módulo)
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

            // ── Hub (now "Más opciones") ──────────────────────────────────────
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
                    overdueCount = overdueCount,
                    budgetAlertCount = budgetAlertCount,
                    lowStockCount = lowStockCount,
                    wishlistHighCount = wishlistHighCount,
                    hubAlertCount = hubAlertCount,
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
                    onNavigateToPersonal = { navController.navigate(Screen.PersonalDashboard.route) },
                    onAddExpense = { navController.navigate(Screen.AddTransaction.createRoute("expense")) },
                    onAddIncome = { navController.navigate(Screen.AddTransaction.createRoute("income")) },
                    onNavigateBack = { navController.popBackStack() }
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
                    onAddTransactionClick = { navController.navigate(Screen.AddTransaction.createRoute("expense")) },
                    isSuperUser = tenantContext.isSuperUser(),
                    onEditTransaction = { t -> navController.navigate(Screen.TransactionDetail.createRoute(t.id, edit = true)) },
                    onNavigateBack = { navController.popBackStack() }
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
                route = "transaction_detail/{transactionId}?edit={edit}",
                arguments = listOf(
                    navArgument("transactionId") { type = NavType.StringType },
                    navArgument("edit") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
                val openInEditMode = backStackEntry.arguments?.getBoolean("edit") ?: false
                val vm: TransactionDetailViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return TransactionDetailViewModel(transactionDetailRepository, tenantContext) as T
                        }
                    }
                )
                TransactionDetailScreen(
                    transactionId = transactionId,
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    openInEditMode = openInEditMode
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
                    availableBalance = availableBalance,
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
        } // end NavHost
    } // end Scaffold
}
