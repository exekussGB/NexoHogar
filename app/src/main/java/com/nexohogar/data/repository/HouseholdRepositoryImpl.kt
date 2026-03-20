package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.AuthApi
import com.nexohogar.data.remote.dto.CreateHouseholdRequest
import com.nexohogar.domain.model.Household
import com.nexohogar.domain.repository.HouseholdRepository

class HouseholdRepositoryImpl(
    private val authApi: AuthApi
) : HouseholdRepository {

    override suspend fun getHouseholds(): AppResult<List<Household>> {
        return try {
            val response = authApi.getHouseholds()
            if (response.isSuccessful) {
                val households = response.body()?.map { dto ->
                    Household(
                        id          = dto.id,
                        name        = dto.name,
                        description = dto.description
                    )
                } ?: emptyList()
                AppResult.Success(households)
            } else {
                AppResult.Error("Error al obtener hogares: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun createHousehold(name: String): AppResult<Household> {
        return try {
            val response = authApi.createHousehold(CreateHouseholdRequest(name))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    AppResult.Success(
                        Household(
                            id          = body.id,
                            name        = body.name,
                            description = body.description
                        )
                    )
                } else {
                    AppResult.Error("El servidor no devolvió datos del hogar")
                }
            } else {
                AppResult.Error("Error al crear hogar: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }
}