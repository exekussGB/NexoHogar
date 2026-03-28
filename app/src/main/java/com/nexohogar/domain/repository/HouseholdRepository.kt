package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.Household
import com.nexohogar.domain.model.HouseholdMember

interface HouseholdRepository {
    suspend fun getHouseholds(): AppResult<List<Household>>
    suspend fun createHousehold(name: String): AppResult<Household>
    suspend fun getOrCreateInviteCode(householdId: String): AppResult<String>
    suspend fun regenerateInviteCode(householdId: String): AppResult<String>
    suspend fun joinHouseholdByCode(inviteCode: String): AppResult<Boolean>
    suspend fun getHouseholdMembers(householdId: String): AppResult<List<HouseholdMember>>
    suspend fun acceptMember(memberId: String): AppResult<Boolean>
    suspend fun rejectMember(memberId: String): AppResult<Boolean>
}
