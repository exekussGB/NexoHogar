package com.nexohogar.presentation.personaldashboard

import com.nexohogar.domain.model.MonthlyBalance
import com.nexohogar.domain.model.PersonalDashboardSummary
import com.nexohogar.domain.model.Transaction

data class PersonalDashboardUiState(
    val isLoading: Boolean = false,
    val summary: PersonalDashboardSummary? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val monthlyBalance: List<MonthlyBalance> = emptyList(),
    val hasPersonalAccounts: Boolean = true,
    val error: String? = null
)
