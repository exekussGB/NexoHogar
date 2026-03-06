package com.nexohogar.domain.model

/**
 * Modelo de dominio puro para el balance de una cuenta.
 */
data class AccountBalance(
    val accountId: String,
    val accountName: String,
    val accountType: String,
    val movementBalance: Double
)
