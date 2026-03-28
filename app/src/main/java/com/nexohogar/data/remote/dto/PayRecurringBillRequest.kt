package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request para marcar una cuenta recurrente como pagada.
 * El usuario puede confirmar o modificar el monto antes de pagar.
 */
data class PayRecurringBillRequest(
    @SerializedName("p_bill_id")      val billId: String,
    @SerializedName("p_household_id") val householdId: String,
    @SerializedName("p_amount_clp")   val amountClp: Long,
    @SerializedName("p_account_id")   val accountId: String? = null,
    @SerializedName("p_notes")        val notes: String? = null
)
