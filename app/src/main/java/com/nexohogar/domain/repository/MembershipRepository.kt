package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.UserUsage

interface MembershipRepository {

    suspend fun getUserUsage(householdId: String): AppResult<UserUsage>

    suspend fun isPremium(householdId: String): AppResult<Boolean>
}