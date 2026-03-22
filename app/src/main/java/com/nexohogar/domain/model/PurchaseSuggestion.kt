package com.nexohogar.domain.model

data class PurchaseSuggestion(
    val product: Product,
    val suggestedQuantity: Double,
    val estimatedCost: Double?,
    val reason: String           // e.g. "Consumo promedio: 2kg/mes, stock actual: 0.5kg"
)
