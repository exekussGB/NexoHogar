package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.mapper.toDomain
import com.nexohogar.data.network.AuthApi
import com.nexohogar.domain.model.Household
import com.nexohogar.domain.repository.HouseholdRepository

/**
 * Implementación del repositorio de hogares.
 * Utiliza TenantContext para persistir la selección del usuario y mappers para devolver modelos de dominio.
 */
class HouseholdRepositoryImpl(
    private val authApi: AuthApi,
    private val tenantContext: TenantContext
) : HouseholdRepository {

    override suspend fun getHouseholds(): AppResult<List<Household>> {
        return try {
            val response = authApi.getHouseholds()
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                AppResult.Success(body.toDomain())
            } else {
                AppResult.Error("Error al obtener hogares")
            }
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}")
        }
    }

    override fun selectHousehold(id: String) {
        tenantContext.setHouseholdId(id)
    }
}
