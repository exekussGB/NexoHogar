package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.InventoryCategory
import com.nexohogar.domain.model.InventoryMovement
import com.nexohogar.domain.model.Product
import com.nexohogar.domain.model.PurchaseSuggestion
import com.nexohogar.domain.model.ScannedReceiptItem

/**
 * COH-03: Migrado a AppResult para consistencia con los demás repositorios.
 */
interface InventoryRepository {
    suspend fun getProducts(householdId: String): AppResult<List<Product>>
    suspend fun createProduct(
        householdId: String,
        name: String,
        unit: String,
        brand: String? = null,
        category: String? = null
    ): AppResult<Product>
    suspend fun getMovements(householdId: String, itemId: String? = null): AppResult<List<InventoryMovement>>
    suspend fun addPurchase(
        householdId: String,
        itemId: String,
        quantity: Double,
        movementDate: String,
        pricePerUnit: Double?,
        priceTotal: Double?,
        brand: String?,
        store: String?
    ): AppResult<InventoryMovement>
    suspend fun addConsumption(
        householdId: String,
        itemId: String,
        quantity: Double,
        movementDate: String
    ): AppResult<InventoryMovement>
    suspend fun getSuggestions(householdId: String): AppResult<List<PurchaseSuggestion>>

    // ─── Categorías ──────────────────────────────────────────────────────────────
    suspend fun getCategories(householdId: String): AppResult<List<InventoryCategory>>
    suspend fun createCategory(householdId: String, name: String, icon: String? = null): AppResult<InventoryCategory>
    suspend fun deleteCategory(categoryId: String): AppResult<Unit>

    suspend fun importReceipt(
        householdId: String,
        userId: String,
        accountId: String,
        categoryId: String?,
        store: String?,
        receiptDate: String,
        items: List<ScannedReceiptItem>
    ): AppResult<Map<String, Any>>
}
