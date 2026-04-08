package com.nexohogar.domain.model

data class FuturePurchase(
    val id: String,
    val householdId: String,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val estimatedPrice: Double? = null,
    val priority: String = "medium",
    val isPurchased: Boolean = false,
    val purchasedAt: String? = null,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)
