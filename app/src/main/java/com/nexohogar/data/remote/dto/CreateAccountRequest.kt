package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para crear una nueva cuenta en Supabase.
 * Solo incluye los campos que existen en la tabla accounts.
 * NOTA: account_subtype fue eliminado — no existe como columna en la tabla.
 */
data class CreateAccountRequest(
    @SerializedName("name")         val name: String,
    @SerializedName("account_type") val accountType: String,   // lowercase: "asset","liability","income","expense"
    @SerializedName("household_id") val householdId: String,
    @SerializedName("balance")      val balance: Double = 0.0,
    @SerializedName("is_active")    val isActive: Boolean = true
)
