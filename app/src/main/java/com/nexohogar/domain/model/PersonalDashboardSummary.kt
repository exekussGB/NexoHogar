package com.nexohogar.domain.model

/**
 * Modelo de dominio para el resumen del dashboard personal.
 * Solo incluye cuentas con is_shared = false y owner_user_id = usuario actual.
 */
data class PersonalDashboardSummary(
    val totalBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val accountsCount: Int,
    val transactionsCount: Int
)
