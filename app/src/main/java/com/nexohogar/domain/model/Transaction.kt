package com.nexohogar.domain.model

/**
 * Modelo de dominio puro para una Transacción.
 * Mantiene un contrato fuerte: los campos críticos no son nullables para garantizar
 * la integridad de la lógica financiera en la capa superior.
 */
data class Transaction(
    val id: String,
    val type: String,
    val description: String,
    val transactionDate: String,
    val amountClp: Double,
    val status: String
)
