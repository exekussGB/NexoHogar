package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.AccountsApi
import com.nexohogar.data.model.CreateAccountRequest
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.repository.AccountsRepository

class AccountsRepositoryImpl(
    private val accountsApi: AccountsApi,
    private val sessionManager: SessionManager
) : AccountsRepository {

    override suspend fun getAccountBalances(
        householdId: String
    ): AppResult<List<AccountBalance>> {
        return try {
            val accounts = accountsApi.getAccounts(householdId = "eq.$householdId")
            val balances = accounts
                .filter { dto ->
                    val nameLower = dto.name.lowercase()
                    !nameLower.contains("system")
                }
                .map { dto ->
                    AccountBalance(
                        accountId       = dto.id,
                        accountName     = dto.name,
                        accountType     = dto.accountType ?: "ASSET",
                        movementBalance = dto.balance?.toLong() ?: 0L,
                        isShared        = dto.isShared ?: true,
                        ownerUserId     = dto.ownerUserId,
                        createdBy       = dto.createdBy
                    )
                }
            AppResult.Success(balances)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al obtener cuentas")
        }
    }

    override suspend fun createAccount(
        householdId: String,
        name: String,
        accountType: String,
        accountSubtype: String,
        isShared: Boolean,
        ownerUserId: String?
    ): AppResult<Account> {
        return try {
            val userId = sessionManager.fetchSession()?.userId
            val request = CreateAccountRequest(
                name         = name,
                accountType  = accountType.uppercase(),
                householdId  = householdId,
                currencyCode = "CLP",
                accountSubtype = accountSubtype,
                isShared     = isShared,
                ownerUserId  = if (!isShared) (ownerUserId ?: userId) else null,
                createdBy    = userId
            )

            val response = accountsApi.createAccount(request)
            val created = response.firstOrNull()
                ?: return AppResult.Error("No se recibió respuesta del servidor")

            AppResult.Success(
                Account(
                    id          = created.id,
                    name        = created.name,
                    type        = created.accountType ?: accountType,
                    balance     = created.balance?.toLong() ?: 0L,
                    householdId = householdId,
                    isShared    = isShared,
                    ownerUserId = ownerUserId ?: userId,
                    createdBy   = userId
                )
            )
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al crear cuenta")
        }
    }

    override suspend fun deleteAccount(accountId: String): AppResult<Unit> {
        return try {
            val response = accountsApi.deleteAccount("eq.$accountId")
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error("Error al eliminar cuenta: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al eliminar cuenta")
        }
    }

    override suspend fun hasPersonalAccounts(householdId: String, userId: String): AppResult<Boolean> {
        return try {
            val accounts = accountsApi.getAccounts(householdId = "eq.$householdId")
            val hasPersonal = accounts.any { dto ->
                dto.isShared == false && dto.ownerUserId == userId && dto.isSystem != true
            }
            AppResult.Success(hasPersonal)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al verificar cuentas personales")
        }
    }

    override suspend fun getPersonalAccountBalances(householdId: String, userId: String): AppResult<List<AccountBalance>> {
        return try {
            val accounts = accountsApi.getAccounts(householdId = "eq.$householdId")
            val balances = accounts
                .filter { dto ->
                    dto.isShared == false && dto.ownerUserId == userId && !dto.name.lowercase().contains("system")
                }
                .map { dto ->
                    AccountBalance(
                        accountId       = dto.id,
                        accountName     = dto.name,
                        accountType     = dto.accountType ?: "ASSET",
                        movementBalance = dto.balance?.toLong() ?: 0L,
                        isShared        = false,
                        ownerUserId     = dto.ownerUserId,
                        createdBy       = dto.createdBy
                    )
                }
            AppResult.Success(balances)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al obtener cuentas personales")
        }
    }
}
