package com.nexohogar.domain.model

data class DashboardSection(
    val totalBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val accountsCount: Int
)

data class DualDashboard(
    val shared: DashboardSection,
    val personal: DashboardSection?
)
