package com.nexohogar.domain.model

/**
 * Modelo de dominio para una cuenta financiera.
 * Utilizado para representar activos, pasivos, ingresos y gastos en la lógica de negocio.
 */
data class Account(
    val id: String,
    val name: String,
    val type: String,
    val balance: Long,          // Long porque CLP no tiene decimales
    val householdId: String
)