package com.nexohogar.domain.model

/**
 * Modelo de dominio para un ítem de presupuesto con consumo.
 * budgetedAmount y consumedAmount en Long (CLP sin decimales).
 */
data class BudgetItem(
    val budgetId        : String,
    val categoryId      : String,
    val categoryName    : String,
    val budgetedAmount  : Long,
    val consumedAmount  : Long,
    val remainingAmount : Long,
    val consumptionPct  : Double
)
