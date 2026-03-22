package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.Household
import com.nexohogar.domain.model.HouseholdMember

interface HouseholdRepository {
    suspend fun getHouseholds(): AppResult<List<Household>>
    suspend fun createHousehold(name: String): AppResult<Household>

    /**
     * Obtiene o crea el código de invitación del hogar.
     * @param householdId UUID del hogar
     * @return código de 8 caracteres
     */
    suspend fun getOrCreateInviteCode(householdId: String): AppResult<String>

    /**
     * Une al usuario autenticado al hogar identificado por el código.
     * @param inviteCode código de 8 caracteres
     * @return true si el join fue exitoso
     */
    suspend fun joinHouseholdByCode(inviteCode: String): AppResult<Boolean>

    /**
     * Obtiene la lista de miembros del hogar.
     * @param householdId UUID del hogar
     */
    suspend fun getHouseholdMembers(householdId: String): AppResult<List<HouseholdMember>>
}
