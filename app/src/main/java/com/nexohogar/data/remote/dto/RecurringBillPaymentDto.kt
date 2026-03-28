package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para el historial de pagos de una bill.
 * Viene de rpc_recurring_bill_history.
 */
data class RecurringBillPaymentDto(
    val id: String,
    @SerializedName("paid_at")    val paidAt: String,
    @SerializedName("amount_clp") val amountClp: Long,
    val notes: String?,
    @SerializedName("paid_by")    val paidBy: String?
)
