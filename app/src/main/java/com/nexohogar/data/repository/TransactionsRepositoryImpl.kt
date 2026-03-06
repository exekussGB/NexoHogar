package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.model.toDomain
import com.nexohogar.data.network.TransactionsApi
import com.nexohogar.domain.model.Transaction
import com.nexohogar.domain.repository.TransactionsRepository

/**
 * Implementation of TransactionsRepository that fetches data from Supabase.
 */
class TransactionsRepositoryImpl(
    private val api: TransactionsApi
) : TransactionsRepository {

    override suspend fun getTransactions(householdId: String): AppResult<List<Transaction>> {
        return try {
            val filter = "eq.$householdId"
            val response = api.getTransactions(filter)
            
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                AppResult.Success(body.toDomain())
            } else {
                AppResult.Error("Error al obtener transacciones: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}")
        }
    }
}
