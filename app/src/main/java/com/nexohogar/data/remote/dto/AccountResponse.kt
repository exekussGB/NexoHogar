package com.nexohogar.data.remote.dto

/**
 * DTO para la respuesta de la API de cuentas.
 */
data class AccountResponse(
    val id: String,
    val household_id: String,
    val name: String,
    val balance: Double,
    val created_at: String
)
