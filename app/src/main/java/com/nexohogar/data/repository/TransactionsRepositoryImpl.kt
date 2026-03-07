package com.nexohogar.data.repository

import android.util.Log
import com.nexohogar.core.result.AppResult
import com.nexohogar.data.model.CreateTransactionRequest
import com.nexohogar.data.model.toDomain
import com.nexohogar.data.network.TransactionsApi
import com.nexohogar.domain.model.Transaction
import com.nexohogar.domain.repository.TransactionsRepository

/**
 * Implementación del repositorio de transacciones.
 * Aplica defensive mapping para asegurar la integridad del dominio.
 */
class TransactionsRepositoryImpl(
    private val api: TransactionsApi
) : TransactionsRepository {

    override suspend fun getTransactions(householdId: String): AppResult<List<Transaction>> {
        return try {
            val filter = "eq.$householdId"
            val response = api.getTransactions(filter)
            
            if (response.isSuccessful) {
                val dtoList = response.body() ?: emptyList()
                
                // Logging para debug: Identificar nulos en la respuesta de Supabase
                Log.d("TRANSACTION_FETCH", "Recibidas ${dtoList.size} transacciones")
                dtoList.forEach { dto ->
                    Log.d("TRANSACTION_DTO", dto.toString())
                }

                val domainList = dtoList.map { it.toDomain() }
                AppResult.Success(domainList)
            } else {
                val errorMsg = "Error ${response.code()}: ${response.errorBody()?.string()}"
                Log.e("TRANSACTION_FETCH", errorMsg)
                AppResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("TRANSACTION_FETCH", "Fallo de red", e)
            AppResult.Error("Fallo de red: ${e.message}")
        }
    }

    override suspend fun createTransaction(request: CreateTransactionRequest): AppResult<Unit> {
        return try {
            val response = api.createTransaction(request)
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                val errorMsg = "Error ${response.code()}: ${response.errorBody()?.string()}"
                Log.e("TRANSACTION_CREATE", errorMsg)
                AppResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("TRANSACTION_CREATE", "Error creando transacción", e)
            AppResult.Error(e.message ?: "Error creando transacción")
        }
    }
}
