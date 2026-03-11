package com.nexohogar.presentation.dashboard

import com.nexohogar.data.model.MonthlyTrendDto
import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.domain.model.Transaction

data class DashboardUiState(
    val summary: DashboardSummary? = null,
    val trends: List<MonthlyTrendDto> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
