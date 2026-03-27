package com.nexohogar.domain.model

data class Product(
    val id: String,
    val householdId: String,
    val name: String,
    val unit: String,           // "kg", "g", "unidades", "litros"
    val brand: String? = null,
    val category: String? = null,
    val notes: String? = null,
    val createdAt: String? = null,
    val currentStock: Double = 0.0,
    val minStock: Int? = null   // Stock mínimo configurable para alertas
)
