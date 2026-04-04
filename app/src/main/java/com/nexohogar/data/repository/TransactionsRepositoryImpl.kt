package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.core.util.AppLogger
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.AccountsApi
import com.nexohogar.data.network.TransactionsApi
import com.nexohogar.data.remote.dto.CreateTransactionRequest
import com.nexohogar.data.remote.dto.CreateTransferRequest
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.Transaction
import com.nexohogar.domain.repository.TransactionsRepository

/**
 * SEC-04: Todos los Log.d/Log.e reemplazados por AppLogger.
 */
class TransactionsRepositoryImpl(
    private val api: TransactionsApi,
    private val accountsApi: AccountsApi,
    private val sessionManager: SessionManager
) : TransactionsRepository {

    override suspend fun getTransactions(
        householdId: String,
        limit: Int,
        offset: Int
    ): AppResult<List<Transaction>> {
        return try {
            val response = api.getTransactions(
                householdFilter = "eq.$householdId",
                limit = limit,
                offset = offset
            )

            if (!response.isSuccessful) {
                val error = response.errorBody()?.string()
                AppLogger.e("TRANSACTIONS_API", "HTTP ${response.code()} -> ${error ?: "unknown error"}")
                return AppResult.Error("Error cargando transacciones")
            }

            val dtoList = response.body() ?: emptyList()

            val result = dtoList
                .map { dto ->
                    Transaction(
                        id            = dto.id,
                        accountId     = dto.accountId,
                        amount        = dto.amountClp,
                        description   = dto.description,
                        createdAt     = dto.createdAt,
                        type          = dto.type,
                        createdByName = dto.createdByName
                    )
                }
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
                AppLogger.e("TransactionsRepository", "Error creating transaction: $errorBody")
                AppResult.Error("Error al crear transacción")
            }
        } catch (e: Exception) {
            AppLogger.e("TransactionsRepository", "Error creating transaction", e)
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun createTransfer(request: CreateTransferRequest): AppResult<Unit> {
        return try {
            AppLogger.d("TransactionsRepository", "Creating transfer")
            val response = api.createTransfer(request)
            if (response.isSuccessful) {
                AppLogger.d("TransactionsRepository", "Transfer created successfully")
                AppResult.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                AppLogger.e("TransactionsRepository", "Transfer error HTTP ${response.code()}: $errorBody")
                AppResult.Error("Error al crear transferencia")
            }
        } catch (e: Exception) {
            AppLogger.e("TransactionsRepository", "Transfer exception", e)
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun getAccounts(householdId: String): AppResult<List<Account>> {
        return try {
            AppLogger.d("TransactionsRepository", "Fetching accounts for householdId=$householdId")

            val body = accountsApi.getAccounts(householdId = "eq.$householdId")

            AppLogger.d("TransactionsRepository", "Accounts returned: ${body.size}")

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
            AppLogger.e("TransactionsRepository", "Error de red", e)
            AppResult.Error(e.message ?: "Error de red")
        }
    }

    override suspend fun getTransactionsByAccount(
        householdId: String,
        accountId: String,
        limit: Int
    ): AppResult<List<Transaction>> {
        return try {
            val response = api.getTransactions(
                householdFilter = "eq.$householdId",
                select = "*"
            )

            if (!response.isSuccessful) {
                val error = response.errorBody()?.string()
                AppLogger.e("TRANSACTIONS_API", "HTTP ${response.code()} -> ${error ?: "unknown error"}")
                return AppResult.Error("Error cargando transacciones de cuenta")
            }

            val dtoList = response.body() ?: emptyList()

            val result = dtoList
                .filter { it.accountId == accountId }
                .map { dto ->
                    Transaction(
                        id            = dto.id,
                        accountId     = dto.accountId,
                        amount        = dto.amountClp,
                        description   = dto.description,
                        createdAt     = dto.createdAt,
                        type          = dto.type,
                        createdByName = dto.createdByName
                    )
                }
                .sortedByDescending { it.createdAt }
                .take(limit)

            AppResult.Success(result)

        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }
}
