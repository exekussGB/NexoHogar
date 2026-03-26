package com.nexohogar.domain.model

data class InventoryCategory(
    val id: String,
    val householdId: String,
    val name: String,
    val icon: String? = null,
    val sortOrder: Int = 0
)
