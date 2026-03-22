package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.RecurringBill

data class RecurringBillDto(
    @SerializedName("id")             val id: String,
    @SerializedName("household_id")   val householdId: String,
    @SerializedName("name")           val name: String,
    @SerializedName("amount_clp")     val amountClp: Long = 0L,
    @SerializedName("due_day")        val dueDayOfMonth: Int,
    @SerializedName("is_active")      val isActive: Boolean = true,
    @SerializedName("last_paid_date") val lastPaidDate: String? = null,
    @SerializedName("notes")          val notes: String? = null,
    @SerializedName("created_at")     val createdAt: String = ""
) {
    fun toDomain() = RecurringBill(
        id             = id,
        householdId    = householdId,
        name           = name,
        amountClp      = amountClp,
        dueDayOfMonth  = dueDayOfMonth,
        isActive       = isActive,
        lastPaidDate   = lastPaidDate,
        notes          = notes,
        createdAt      = createdAt
    )
}

data class CreateRecurringBillRequest(
    @SerializedName("household_id")   val householdId: String,
    @SerializedName("name")           val name: String,
    @SerializedName("amount_clp")     val amountClp: Long,
    @SerializedName("due_day")        val dueDayOfMonth: Int,
    @SerializedName("notes")          val notes: String? = null,
    @SerializedName("is_active")      val isActive: Boolean = true
)

data class UpdateLastPaidRequest(
    @SerializedName("last_paid_date") val lastPaidDate: String
)

data class ToggleActiveRequest(
    @SerializedName("is_active") val isActive: Boolean
)
