package com.nexohogar.presentation.dashboard

import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.domain.model.MonthlyBalance
import com.nexohogar.domain.model.Transaction
import com.nexohogar.domain.model.RecurringBill

data class DashboardUiState(
    val isLoading: Boolean = false,
    val summary: DashboardSummary? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val monthlyBalance: List<MonthlyBalance> = emptyList(),
    val hasPersonalAccounts: Boolean = false,
    val totalSavings: Long = 0L,
    val totalLiabilities: Long = 0L,                   // 🆕 Feature 3: Deudas totales
    val totalCreditLimit: Long = 0L,                   // 🆕 Feature 4: Cupo total de TCs
    val creditCards: List<com.nexohogar.domain.model.AccountBalance> = emptyList(),
    val upcomingBills: List<RecurringBill> = emptyList(), // 🆕 Integración con Cuentas Recurrentes
    val pendingBillsTotal: Long = 0L,                   // 🆕 Monto total comprometido este mes
    val actualLiquidity: Double? = null,                // 🆕 Balance - Presupuesto Comprometido
    val computedTotalBalance: Double? = null,
    val error: String? = null
)
