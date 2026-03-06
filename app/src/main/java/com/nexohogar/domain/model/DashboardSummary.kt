package com.nexohogar.domain.model

/**
 * Domain model representing the dashboard summary data.
 */
data class DashboardSummary(
    val householdId: String,
    val totalBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val accountsCount: Int,
    val transactionsCount: Int
)
