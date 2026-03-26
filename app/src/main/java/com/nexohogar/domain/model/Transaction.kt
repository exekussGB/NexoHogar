package com.nexohogar.domain.model

/**
 * Modelo de dominio para una Transacción.
 * amount en Long porque CLP no tiene decimales.
 */
data class Transaction(
    val id: String,
    val description: String?,
    val amount: Long,           // Long: CLP no tiene decimales
    val accountId: String,
    val createdAt: String,
    val type: String,
    val createdByName: String? = null
)
