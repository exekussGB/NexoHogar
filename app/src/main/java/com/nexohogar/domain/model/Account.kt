package com.nexohogar.domain.model

/**
 * Modelo de dominio para una cuenta financiera.
 * Soporta cuentas compartidas y personales.
 */
data class Account(
    val id: String,
    val name: String,
    val type: String,
    val subtype: String = "other",
    val balance: Long,
    val householdId: String,
    val isShared: Boolean = true,
    val ownerUserId: String? = null,
    val createdBy: String? = null
)
