package com.nexohogar.domain.model

data class Product(
    val id: String,
    val householdId: String,
    val name: String,
    val unit: String,           // "kg", "g", "unidades", "litros"
    val brand: String? = null,
    val category: String? = null,  // "Carnes", "Lácteos", "Cereales", etc.
    val notes: String? = null,
    val createdAt: String? = null,
    // Calculado al cargar (suma de movimientos)
    val currentStock: Double = 0.0
)
