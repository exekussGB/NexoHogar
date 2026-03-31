package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.WishlistItem

interface WishlistRepository {

    suspend fun getWishlistItems(householdId: String): AppResult<List<WishlistItem>>

    suspend fun createWishlistItem(
        householdId: String,
        name: String,
        description: String?,
        price: Double?,
        priority: String,
        createdBy: String
    ): AppResult<WishlistItem>

    suspend fun updateWishlistItem(
        itemId: String,
        name: String,
        description: String?,
        price: Double?,
        priority: String
    ): AppResult<WishlistItem>

    suspend fun markAsPurchased(itemId: String): AppResult<WishlistItem>

    suspend fun deleteWishlistItem(itemId: String): AppResult<Unit>
}
