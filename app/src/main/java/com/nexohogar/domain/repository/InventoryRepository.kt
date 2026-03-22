package com.nexohogar.domain.repository

import com.nexohogar.domain.model.InventoryMovement
import com.nexohogar.domain.model.Product
import com.nexohogar.domain.model.PurchaseSuggestion

interface InventoryRepository {
    suspend fun getProducts(householdId: String): List<Product>
    suspend fun createProduct(
        householdId: String,
        name: String,
        unit: String,
        brand: String? = null,
        category: String? = null
    ): Product
    suspend fun getMovements(householdId: String, itemId: String? = null): List<InventoryMovement>
    suspend fun addPurchase(
        householdId: String,
        itemId: String,
        quantity: Double,
        movementDate: String,
        pricePerUnit: Double?,
        priceTotal: Double?,
        brand: String?,
        store: String?
    ): InventoryMovement
    suspend fun addConsumption(
        householdId: String,
        itemId: String,
        quantity: Double,
        movementDate: String
    ): InventoryMovement
    suspend fun getSuggestions(householdId: String): List<PurchaseSuggestion>
}
