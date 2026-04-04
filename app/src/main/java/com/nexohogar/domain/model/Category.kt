package com.nexohogar.domain.model

/**
 * Modelo de dominio para una categoría de transacción.
 */
data class Category(
    val id: String,
    val name: String,
    val type: String,
    val icon: String? = null
)
