package com.nexohogar.domain.model

/**
 * Modelo de dominio para una Transacción.
 * Representa la entidad de negocio simplificada.
 */
data class Transaction(
    val id: String,
    val description: String?,
    val amount: Double,
    val accountId: String,
    val createdAt: String,
    val type: String
)
