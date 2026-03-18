package com.nexohogar.presentation.dashboard

import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.domain.model.Transaction
import com.nexohogar.domain.model.MonthlyBalance

data class DashboardUiState(
    val isLoading: Boolean = false,
    val summary: DashboardSummary? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val monthlyBalance: List<MonthlyBalance> = emptyList(),
    val error: String? = null
)
