package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.FuturePurchase

interface FuturePurchasesRepository {
    suspend fun getFuturePurchases(householdId: String): AppResult<List<FuturePurchase>>

    suspend fun createFuturePurchase(
        householdId: String,
        name: String,
        description: String? = null,
        category: String? = null,
        estimatedPrice: Double? = null,
        priority: String = "medium",
        createdBy: String
    ): AppResult<FuturePurchase>

    suspend fun updateFuturePurchase(
        itemId: String,
        name: String? = null,
        description: String? = null,
        category: String? = null,
        estimatedPrice: Double? = null,
        priority: String? = null
    ): AppResult<FuturePurchase>

    suspend fun markAsPurchased(itemId: String): AppResult<FuturePurchase>

    suspend fun deleteFuturePurchase(itemId: String): AppResult<Unit>
}
