package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para el resumen mensual de cuentas recurrentes.
 * Viene de rpc_recurring_summary.
 */
data class RecurringSummaryDto(
    @SerializedName("total_bills")       val totalBills: Int,
    @SerializedName("active_bills")      val activeBills: Int,
    @SerializedName("total_monthly_clp") val totalMonthlyClp: Long,
    @SerializedName("paid_this_month")   val paidThisMonth: Int,
    @SerializedName("paid_amount_clp")   val paidAmountClp: Long,
    @SerializedName("pending_count")     val pendingCount: Int,
    @SerializedName("pending_amount_clp")val pendingAmountClp: Long,
    @SerializedName("overdue_count")     val overdueCount: Int
)
