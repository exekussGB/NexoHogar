package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para la creación de transferencias entre cuentas vía RPC.
 */
data class CreateTransferRequest(
    @SerializedName("household_id") val householdId: String,
    @SerializedName("from_account_id") val fromAccountId: String,
    @SerializedName("to_account_id") val toAccountId: String,
    @SerializedName("amount_clp") val amountClp: Long
)
