package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.model.toDomain
import com.nexohogar.data.network.TransactionDetailApi
import com.nexohogar.domain.model.TransactionEntry
import com.nexohogar.domain.repository.TransactionDetailRepository

/**
 * Implementation of TransactionDetailRepository that fetches data from Supabase.
 */
class TransactionDetailRepositoryImpl(
    private val api: TransactionDetailApi
) : TransactionDetailRepository {

    override suspend fun getTransactionEntries(transactionId: String): AppResult<List<TransactionEntry>> {
        return try {
            val filter = "eq.$transactionId"
            val response = api.getTransactionEntries(filter)
            
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                AppResult.Success(body.toDomain())
            } else {
                AppResult.Error("Error al obtener detalles de la transacción: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}")
        }
    }
}
