package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.PersonalDashboardSummary

/**
 * DTO para la respuesta de rpc_personal_dashboard_summary.
 */
data class PersonalDashboardDto(
    @SerializedName("total_balance")      val totalBalance: Double,
    @SerializedName("total_income")       val totalIncome: Double,
    @SerializedName("total_expense")      val totalExpense: Double,
    @SerializedName("accounts_count")     val accountsCount: Int,
    @SerializedName("transactions_count") val transactionsCount: Int
)

fun PersonalDashboardDto.toDomain() = PersonalDashboardSummary(
    totalBalance      = totalBalance,
    totalIncome       = totalIncome,
    totalExpense      = totalExpense,
    accountsCount     = accountsCount,
    transactionsCount = transactionsCount
)
