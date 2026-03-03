package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.Household

/**
 * Interfaz del repositorio de hogares en la capa de dominio.
 */
interface HouseholdRepository {
    suspend fun getHouseholds(): AppResult<List<Household>>
    fun selectHousehold(id: String)
}
