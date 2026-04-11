package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.Plan
import com.nexohogar.domain.model.Subscription

interface SubscriptionsRepository {
    suspend fun getCurrentUserSubscription(): AppResult<Subscription>
    suspend fun getUserSubscription(userId: String): AppResult<Subscription>
    suspend fun getCurrentUserPlan(): AppResult<Plan>
}
