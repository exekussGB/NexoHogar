package com.nexohogar.domain.model

/**
 * Modelo de dominio puro para un Hogar (Household).
 * Sin anotaciones de Gson o Retrofit.
 */
data class Household(
    val id: String,
    val name: String,
    val description: String
)
