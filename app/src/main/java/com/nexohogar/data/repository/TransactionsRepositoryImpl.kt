package com.nexohogar.data.repository

import android.util.Log
import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.AccountsApi
import com.nexohogar.data.network.TransactionsApi
import com.nexohogar.data.remote.dto.CreateTransactionRequest
import com.nexohogar.data.remote.dto.CreateTransferRequest
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.Transaction
import com.nexohogar.domain.repository.TransactionsRepository

/**
 * Implementación del repositorio de transacciones.
 */
class TransactionsRepositoryImpl(
    private val api: TransactionsApi,
    private val accountsApi: AccountsApi,
    private val sessionManager: SessionManager
) : TransactionsRepository {

    override suspend fun getTransactions(
        householdId: String
    ): AppResult<List<Transaction>> {
        return try {
            val response = api.getTransactions("eq.$householdId")

            if (!response.isSuccessful) {
                val error = response.errorBody()?.string()
                Log.e("TRANSACTIONS_API", "HTTP ${response.code()} -> ${error ?: "unknown error"}")
                return AppResult.Error("Error cargando transacciones")
            }

            val dtoList = response.body() ?: emptyList()

            val result = dtoList
                .map { dto ->
                    Transaction(
                        id          = dto.id,
                        accountId   = dto.accountId,
                        amount      = dto.amountClp,
                        description = dto.description,
                        createdAt   = dto.createdAt,
                        type        = dto.type
                    )
                }
                // ── Ordenar de más reciente a más antiguo ──────────────────────
                // createdAt es ISO-8601 ("2024-01-15T10:30:00"), el orden lexicográfico
                // descendente es equivalente al orden cronológico descendente.
                .sortedByDescending { it.createdAt }

            AppResult.Success(result)

        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun createTransaction(request: CreateTransactionRequest): AppResult<Unit> {
        return try {
            val response = api.createTransaction(request)
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("TransactionsRepository", "Error creating transaction: $errorBody")
                AppResult.Error("Error al crear transacción")
            }
        } catch (e: Exception) {
            Log.e("TransactionsRepository", "Error creating transaction", e)
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun createTransfer(request: CreateTransferRequest): AppResult<Unit> {
        return try {
            val response = api.createTransfer(request)
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("TransactionsRepository", "Error creating transfer: $errorBody")
                AppResult.Error("Error al crear transferencia")
            }
        } catch (e: Exception) {
            Log.e("TransactionsRepository", "Error creating transfer", e)
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun getAccounts(householdId: String): AppResult<List<Account>> {
        return try {
            Log.d("ACCOUNTS_DEBUG", "Fetching accounts for householdId=$householdId")

            val body = accountsApi.getAccounts(householdId = "eq.$householdId")

            Log.d("ACCOUNTS_DEBUG", "Accounts returned from API: ${body.size}")

            val domainAccounts = body.map { dto ->
                Account(
                    id          = dto.id,
                    name        = dto.name,
                    type        = dto.accountType ?: "ASSET",
                    balance     = (dto.balance ?: 0.0).toLong(),
                    householdId = dto.householdId
                )
            }
            AppResult.Success(domainAccounts)

        } catch (e: Exception) {
            Log.e("ACCOUNTS_FETCH", "Error de red", e)
            AppResult.Error(e.message ?: "Error de red")
        }
    }
}
