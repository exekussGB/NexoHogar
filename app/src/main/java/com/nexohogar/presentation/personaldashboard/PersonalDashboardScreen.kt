package com.nexohogar.presentation.personaldashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.presentation.components.LoadingOverlay
import com.nexohogar.presentation.dashboard.BalanceCardWithInsights
import com.nexohogar.presentation.dashboard.CreditCardsCard
import com.nexohogar.presentation.dashboard.MonthlyChart
import com.nexohogar.presentation.dashboard.RecentTransactionsList
import com.nexohogar.presentation.dashboard.SavingsCard
import com.nexohogar.presentation.dashboard.DashboardUiState
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDashboardScreen(
    viewModel: PersonalDashboardViewModel,
    onTransactionClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState   by viewModel.uiState.collectAsState()
    val clpFormat  = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Resumen Personal",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading) {
                LoadingOverlay()
            } else if (!uiState.hasPersonalAccounts) {
                EmptyPersonalAccountsState()
            } else {
                LazyColumn(
                    modifier        = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding  = PaddingValues(vertical = 16.dp)
                ) {
                    // ── Chip de contexto ──────────────────────────────────
                    item { PersonalContextChip() }

                    // ── Balance personal ──────────────────────────────────
                    item {
                        uiState.summary?.let { summary ->
                            val dashboardSummary = DashboardSummary(
                                householdId       = "",
                                totalBalance      = uiState.computedTotalBalance ?: summary.totalBalance,
                                totalIncome       = summary.totalIncome,
                                totalExpense      = summary.totalExpense,
                                accountsCount     = summary.accountsCount,
                                transactionsCount = summary.transactionsCount
                            )
                            // Reutilizamos BalanceCardWithInsights con un DashboardUiState mínimo
                            BalanceCardWithInsights(
                                summary = dashboardSummary,
                                format  = clpFormat,
                                uiState = DashboardUiState(
                                    computedTotalBalance = uiState.computedTotalBalance,
                                    actualLiquidity      = null,
                                    pendingBillsTotal    = 0L
                                )
                            )
                        }
                    }

                    // ── Tarjetas de crédito personales ────────────────────
                    if (uiState.creditCards.isNotEmpty()) {
                        item {
                            CreditCardsCard(
                                cards  = uiState.creditCards,
                                format = clpFormat
                            )
                        }
                    }

                    // ── Gráfico de tendencia ──────────────────────────────
                    item { MonthlyChart(monthlyData = uiState.monthlyBalance) }

                    // ── Ahorro personal ───────────────────────────────────
                    if (uiState.totalSavings != 0L) {
                        item {
                            SavingsCard(
                                totalSavings = uiState.totalSavings,
                                format       = clpFormat
                            )
                        }
                    }

                    // ── Últimos movimientos personales ────────────────────
                    item {
                        RecentTransactionsList(
                            transactions       = uiState.recentTransactions,
                            format             = clpFormat,
                            onTransactionClick = onTransactionClick,
                            onSeeAllClick      = { }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PersonalContextChip() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.AccountCircle,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier           = Modifier.size(16.dp)
            )
            Text(
                text       = "Cuentas personales",
                style      = MaterialTheme.typography.labelMedium,
                color      = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptyPersonalAccountsState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier           = Modifier.size(72.dp),
                tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Text(
                text       = "Sin cuentas personales",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )
            Text(
                text      = "Aún no tienes cuentas marcadas como personales.\nCrea una cuenta con \"Solo yo\" para verla aquí.",
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}