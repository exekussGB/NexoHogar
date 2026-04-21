package com.nexohogar.presentation.personaldashboard

import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.model.MonthlyBalance
import com.nexohogar.domain.model.PersonalDashboardSummary
import com.nexohogar.domain.model.Transaction
import com.nexohogar.domain.model.RecurringBill

data class PersonalDashboardUiState(
    val isLoading: Boolean = false,
    val summary: PersonalDashboardSummary? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val monthlyBalance: List<MonthlyBalance> = emptyList(),
    val hasPersonalAccounts: Boolean = true,
    val totalSavings: Long = 0L,
    val totalLiabilities: Long = 0L,
    val totalCreditLimit: Long = 0L,
    val creditCards: List<AccountBalance> = emptyList(),
    val computedTotalBalance: Double? = null,
    val error: String? = null
)