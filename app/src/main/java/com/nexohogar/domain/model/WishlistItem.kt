package com.nexohogar.domain.model

data class WishlistItem(
    val id: String,
    val householdId: String,
    val name: String,
    val estimatedCost: Long,
    val notes: String?,
    val priority: Int,       // 1=alta, 2=media, 3=baja
    val isPurchased: Boolean,
    val purchasedAt: String?,
    val purchasedBy: String?,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
) {
    val priorityLabel: String get() = when (priority) {
        1    -> "Alta"
        2    -> "Media"
        3    -> "Baja"
        else -> "Media"
    }
}
