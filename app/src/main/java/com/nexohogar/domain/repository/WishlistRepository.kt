package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.WishlistItem

interface WishlistRepository {

    suspend fun getWishlistItems(householdId: String): AppResult<List<WishlistItem>>

    suspend fun createWishlistItem(
        householdId: String,
        name: String,
        estimatedCost: Long,
        notes: String?,
        priority: Int,
        createdBy: String
    ): AppResult<WishlistItem>

    suspend fun updateWishlistItem(
        itemId: String,
        name: String,
        estimatedCost: Long,
        notes: String?,
        priority: Int
    ): AppResult<WishlistItem>

    suspend fun markAsPurchased(
        itemId: String,
        purchasedBy: String
    ): AppResult<WishlistItem>

    suspend fun deleteWishlistItem(itemId: String): AppResult<Unit>
}
