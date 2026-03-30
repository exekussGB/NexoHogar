package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.domain.model.DashboardSection

data class DashboardDto(
    @SerializedName("household_id") val householdId: String,
    @SerializedName("total_balance") val totalBalance: Double,
    @SerializedName("total_income") val totalIncome: Double,
    @SerializedName("total_expense") val totalExpense: Double,
    @SerializedName("accounts_count") val accountsCount: Int,
    @SerializedName("transactions_count") val transactionsCount: Int
)

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

data class MonthlyTrendDto(
    @SerializedName("month_name") val month: String,
    @SerializedName("total_income") val income: Double,
    @SerializedName("total_expense") val expense: Double
)

data class DualDashboardDto(
    @SerializedName("section") val section: String,
    @SerializedName("total_balance") val totalBalance: Double,
    @SerializedName("total_income") val totalIncome: Double,
    @SerializedName("total_expense") val totalExpense: Double,
    @SerializedName("accounts_count") val accountsCount: Int
)

fun DualDashboardDto.toSection(): DashboardSection {
    return DashboardSection(
        totalBalance = totalBalance,
        totalIncome = totalIncome,
        totalExpense = totalExpense,
        accountsCount = accountsCount
    )
}
