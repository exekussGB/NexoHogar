package com.nexohogar.domain.repository

import com.nexohogar.domain.model.InventoryCategory
import com.nexohogar.domain.model.InventoryMovement
import com.nexohogar.domain.model.Product
import com.nexohogar.domain.model.PurchaseSuggestion
import com.nexohogar.domain.model.ScannedReceiptItem

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

    // ─── Categorías ──────────────────────────────────────────────────────────────
    suspend fun getCategories(householdId: String): List<InventoryCategory>
    suspend fun createCategory(householdId: String, name: String, icon: String? = null): InventoryCategory
    suspend fun deleteCategory(categoryId: String)

    suspend fun importReceipt(
        householdId: String,
        userId: String,
        accountId: String,
        categoryId: String?,
        store: String?,
        receiptDate: String,
        items: List<ScannedReceiptItem>
    ): Map<String, Any>
}
