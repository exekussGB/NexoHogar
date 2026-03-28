package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para bills con estado de pago del mes actual.
 * Viene de rpc_recurring_bills_with_status.
 */
data class RecurringBillWithStatusDto(
    val id: String,
    val name: String,
    @SerializedName("amount_clp")          val amountClp: Long,
    @SerializedName("due_day")             val dueDay: Int,
    @SerializedName("is_active")           val isActive: Boolean,
    val notes: String?,
    @SerializedName("account_id")          val accountId: String?,
    @SerializedName("category_id")         val categoryId: String?,
    @SerializedName("is_paid_this_month")  val isPaidThisMonth: Boolean,
    @SerializedName("last_payment_amount") val lastPaymentAmount: Long?,
    @SerializedName("last_payment_date")   val lastPaymentDate: String?,
    @SerializedName("is_overdue")          val isOverdue: Boolean,
    @SerializedName("days_until_due")      val daysUntilDue: Int?
)
