package com.nexohogar.domain.model

data class Budget(
    val id: String,
    val householdId: String,
    val categoryId: String,
    val categoryName: String,
    val amountClp: Long,
    val spentClp: Long = 0,
    val period: String = "monthly",
    val isActive: Boolean = true
) {
    val percentage: Double
        get() = if (amountClp > 0) (spentClp.toDouble() / amountClp.toDouble()) * 100.0 else 0.0

    val remaining: Long
        get() = amountClp - spentClp
}
