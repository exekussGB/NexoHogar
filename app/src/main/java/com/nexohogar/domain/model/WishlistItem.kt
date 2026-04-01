package com.nexohogar.domain.model

data class WishlistItem(
    val id: String,
    val householdId: String,
    val name: String,
    val description: String?,
    val url: String?,
    val price: Double?,
    val priority: String,      // "HIGH", "MEDIUM", "LOW"
    val isPurchased: Boolean,
    val purchasedAt: String?,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
) {
    val priorityLabel: String get() = when (priority) {
        "HIGH"   -> "Alta"
        "MEDIUM" -> "Media"
        "LOW"    -> "Baja"
        else     -> "Media"
    }
}
