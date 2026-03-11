package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.DashboardSummary

/**
 * DTO para la vista v_dashboard de Supabase.
 */
data class DashboardDto(
    @SerializedName("household_id") val householdId: String,
    @SerializedName("total_balance") val totalBalance: Double,
    @SerializedName("total_income") val totalIncome: Double,
    @SerializedName("total_expense") val totalExpense: Double,
    @SerializedName("accounts_count") val accountsCount: Int,
    @SerializedName("transactions_count") val transactionsCount: Int
)

/**
 * Mapper extension to convert DTO to Domain model.
 */
fun DashboardDto.toDomain(): DashboardSummary {
    return DashboardSummary(
        householdId = householdId,
        totalBalance = totalBalance,
        totalIncome = totalIncome,
        totalExpense = totalExpense,
        accountsCount = accountsCount,
        transactionsCount = transactionsCount
    )
}

/**
 * DTO para la vista v_monthly_summary de Supabase.
 */
data class MonthlyTrendDto(
    @SerializedName("month_name") val month: String,
    @SerializedName("total_income") val income: Double,
    @SerializedName("total_expense") val expense: Double
)
