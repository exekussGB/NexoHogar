package com.nexohogar.domain.model

data class InventoryMovement(
    val id: String,
    val householdId: String,
    val itemId: String,           // FK → inventory_items.id
    val transactionId: String? = null,
    val movementType: String,     // "in" (compra) o "out" (consumo)
    val quantity: Double,
    val movementDate: String,     // "yyyy-MM-dd"
    val pricePerUnit: Double? = null,
    val priceTotal: Double? = null,
    val brand: String? = null,
    val store: String? = null,
    val notes: String? = null,
    val createdAt: String? = null
)
