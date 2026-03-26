package com.nexohogar.presentation.dashboard

import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.domain.model.MonthlyBalance
import com.nexohogar.domain.model.Transaction

data class DashboardUiState(
    val isLoading: Boolean = false,
    val summary: DashboardSummary? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val monthlyBalance: List<MonthlyBalance> = emptyList(),
    val hasPersonalAccounts: Boolean = false,
    val error: String? = null
)
