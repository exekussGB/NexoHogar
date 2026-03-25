package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.BudgetConsumption

data class BudgetConsumptionDto(
    @SerializedName("budget_id")
    val budgetId: String,

    @SerializedName("category_id")
    val categoryId: String,

    @SerializedName("category_name")
    val categoryName: String,

    @SerializedName("budgeted_amount")
    val budgetedAmount: Double,

    @SerializedName("consumed_amount")
    val consumedAmount: Double,

    @SerializedName("remaining_amount")
    val remainingAmount: Double,

    @SerializedName("consumption_pct")
    val consumptionPct: Double,

    @SerializedName("member_id")
    val memberId: String?
) {
    fun toDomain(): BudgetConsumption {
        return BudgetConsumption(
            budgetId = budgetId,
            categoryId = categoryId,
            categoryName = categoryName,
            budgetedAmount = budgetedAmount,
            consumedAmount = consumedAmount,
            remainingAmount = remainingAmount,
            consumptionPct = consumptionPct,
            memberId = memberId
        )
    }
}