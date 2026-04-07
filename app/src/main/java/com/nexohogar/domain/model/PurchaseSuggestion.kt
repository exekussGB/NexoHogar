package com.nexohogar.domain.model

/**
 * Sugerencia de compra generada a partir del historial de consumo.
 * Refactor v1.2.8 → HEAD: campos aplanados para evitar dependencia directa con Product.
 */
data class PurchaseSuggestion(
    val productId: String,
    val productName: String,
    val unit: String,
    val category: String?,
    val currentStock: Double,
    val suggestedQuantity: Double,
    val estimatedCost: Double?,
    val reason: String
)
