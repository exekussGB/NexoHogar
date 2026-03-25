package com.nexohogar.domain.model

data class BudgetConsumption(
    val budgetId: String,
    val categoryId: String,
    val categoryName: String,
    val budgetedAmount: Double,
    val consumedAmount: Double,
    val remainingAmount: Double,
    val consumptionPct: Double,
    val memberId: String?
)