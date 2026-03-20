package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.Household

interface HouseholdRepository {
    suspend fun getHouseholds(): AppResult<List<Household>>
    suspend fun createHousehold(name: String): AppResult<Household>
}