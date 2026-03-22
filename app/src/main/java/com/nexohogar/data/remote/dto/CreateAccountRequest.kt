package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para crear una nueva cuenta en Supabase.
 * Solo se envían los campos mínimos requeridos para evitar errores 400
 * por columnas con DEFAULT o restricciones en la tabla.
 */
data class CreateAccountRequest(
    @SerializedName("name")         val name: String,
    @SerializedName("account_type") val accountType: String,   // lowercase: "asset","liability"
    @SerializedName("household_id") val householdId: String
)
