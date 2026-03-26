package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.BudgetItem

/**
 * DTO para rpc_get_budget_consumption.
 * Columnas devueltas:
 *   budget_id, category_id, category_name,
 *   budgeted_amount, consumed_amount, remaining_amount,
 *   consumption_pct, member_id
 */
data class BudgetConsumptionDto(
    @SerializedName("budget_id")        val budgetId        : String,
    @SerializedName("category_id")      val categoryId      : String,
    @SerializedName("category_name")    val categoryName    : String,
    @SerializedName("budgeted_amount")  val budgetedAmount  : Long,
    @SerializedName("consumed_amount")  val consumedAmount  : Long,
    @SerializedName("remaining_amount") val remainingAmount : Long,
    @SerializedName("consumption_pct")  val consumptionPct  : Double
)

fun BudgetConsumptionDto.toDomain() = BudgetItem(
    budgetId        = budgetId,
    categoryId      = categoryId,
    categoryName    = categoryName,
    budgetedAmount  = budgetedAmount,
    consumedAmount  = consumedAmount,
    remainingAmount = remainingAmount,
    consumptionPct  = consumptionPct
)

fun List<BudgetConsumptionDto>.toDomain() = map { it.toDomain() }
