package com.nexohogar.domain.model

data class ScannedReceiptItem(
    val name: String,
    val quantity: Double = 1.0,
    val pricePerUnit: Double? = null,
    val priceTotal: Double? = null,
    val brand: String? = null,
    val unit: String = "unidad",
    val category: String? = null,
    val isSelected: Boolean = true
)