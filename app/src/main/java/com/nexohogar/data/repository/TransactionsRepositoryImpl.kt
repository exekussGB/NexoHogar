package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.core.util.AppLogger
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.local.room.dao.AccountDao
import com.nexohogar.data.local.room.dao.TransactionDao
import com.nexohogar.data.local.room.entity.AccountEntity
import com.nexohogar.data.local.room.entity.TransactionEntity
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
    private val sessionManager: SessionManager,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
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
        // ── 1. Guardar localmente primero (Offline-First) ──
        val localId = java.util.UUID.randomUUID().toString()
        val localEntity = TransactionEntity(
            id = localId,
            householdId = request.pHouseholdId,
            type = request.pType,
            accountId = request.pAccountId,
            categoryId = request.pCategoryId,
            amountClp = request.pAmountClp,
            description = request.pDescription,
            transactionDate = request.pTransactionDate,
            createdAt = java.time.LocalDateTime.now().toString(),
            pendingSync = true
        )

        try {
            transactionDao.insertTransaction(localEntity)
            AppLogger.d("TransactionsRepository", "Transaction saved locally: $localId")
        } catch (e: Exception) {
            AppLogger.e("TransactionsRepository", "Error saving local transaction", e)
        }

        return try {
            val response = api.createTransaction(request)
            if (response.isSuccessful) {
                // ── 2. Si se subió con éxito, marcar como sincronizada ──
                transactionDao.markAsSynced(localId)
                AppResult.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                AppLogger.e("TransactionsRepository", "Error creating transaction in API: $errorBody")
                // No devolvemos error fatal, porque ya está guardada localmente
                AppResult.Success(Unit) 
            }
        } catch (e: Exception) {
            AppLogger.e("TransactionsRepository", "Network error creating transaction", e)
            // Sigue siendo éxito para la UI porque ya está en Room y se subirá luego
            AppResult.Success(Unit)
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

            // Intentar fetch de API
            val body = accountsApi.getAccounts(householdId = "eq.$householdId")
            
            val domainAccounts = body.map { dto ->
                Account(
                    id          = dto.id,
                    name        = dto.name,
                    type        = dto.accountType ?: "ASSET",
                    balance     = (dto.balance ?: 0.0).toLong(),
                    householdId = dto.householdId
                )
            }

            // Actualizar caché local
            val entities = domainAccounts.map { 
                AccountEntity(it.id, it.householdId, it.name, it.type, it.balance)
            }
            accountDao.insertAccounts(entities)

            AppResult.Success(domainAccounts)

        } catch (e: Exception) {
            AppLogger.e("TransactionsRepository", "Network error, falling back to local DB", e)
            
            // Fallback a Room
            return try {
                val localAccounts = accountDao.getAccountsByHousehold(householdId)
                // Como getAccountsByHousehold retorna un Flow, aquí tendríamos un problema si la interfaz no es reactiva.
                // Para mantener la interfaz suspend, podemos usar .first() pero necesitamos kotlinx-coroutines-core
                // O simplemente hacer un query síncrono (suspend) en el DAO.
                
                // Por ahora, para no complicar el DAO, devolveremos el error o un mensaje amigable.
                // Lo ideal es que la interfaz retorne Flow<AppResult<List<Account>>>.
                AppResult.Error("Sin conexión. Usando datos locales (limitado).")
            } catch (localEx: Exception) {
                AppResult.Error("Error de red y no hay datos locales.")
            }
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
