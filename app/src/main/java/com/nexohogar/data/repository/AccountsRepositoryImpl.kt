package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.model.toDomain
import com.nexohogar.data.network.AccountsApi
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.repository.AccountsRepository

/**
 * Implementación del repositorio de cuentas que consume datos de Supabase.
 */
class AccountsRepositoryImpl(
    private val api: AccountsApi
) : AccountsRepository {

    override suspend fun getAccountBalances(householdId: String): AppResult<List<AccountBalance>> {
        return try {
            val filter = "eq.$householdId"
            val response = api.getAccountBalances(filter)
            
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                AppResult.Success(body.toDomain())
            } else {
                AppResult.Error("Error al obtener balances: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}")
        }
    }
}
