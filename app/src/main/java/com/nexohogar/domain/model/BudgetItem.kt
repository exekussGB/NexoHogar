package com.nexohogar.domain.model

/**
 * Modelo de dominio para un ítem de presupuesto con consumo.
 * budgetedAmount y spentAmount en Long (CLP sin decimales).
 */
data class BudgetItem(
    val budgetId        : String,
    val categoryId      : String,
    val categoryName    : String,
    val budgetedAmount  : Long,
    val spentAmount     : Long,
    val remainingAmount : Long,
    val consumptionPct  : Double
)
