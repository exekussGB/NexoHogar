package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para la respuesta de la API de cuentas.
 */
data class AccountResponse(
    @SerializedName("id") val id: String,
    @SerializedName("household_id") val householdId: String,
    @SerializedName("name") val name: String,
    @SerializedName("account_type") val accountType: String?,
    @SerializedName("balance") val balance: Double?,
    @SerializedName("created_at") val createdAt: String
)
