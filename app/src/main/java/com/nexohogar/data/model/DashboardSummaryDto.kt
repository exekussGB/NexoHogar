package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.DashboardSummary

/**
 * Data Transfer Object for dashboard summary from Supabase.
 */
data class DashboardSummaryDto(
    @SerializedName("household_id")
    val householdId: String,
    @SerializedName("total_balance")
    val totalBalance: Double,
    @SerializedName("total_income")
    val totalIncome: Double,
    @SerializedName("total_expense")
    val totalExpense: Double,
    @SerializedName("accounts_count")
    val accountsCount: Int,
    @SerializedName("transactions_count")
    val transactionsCount: Int
)

/**
 * Mapper extension to convert DTO to Domain model.
 */
fun DashboardSummaryDto.toDomain(): DashboardSummary {
    return DashboardSummary(
        householdId = householdId,
        totalBalance = totalBalance,
        totalIncome = totalIncome,
        totalExpense = totalExpense,
        accountsCount = accountsCount,
        transactionsCount = transactionsCount
    )
}
