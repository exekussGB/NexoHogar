package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.model.CreateAccountRequest
import com.nexohogar.data.network.AccountsApi
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.repository.AccountsRepository

class AccountsRepositoryImpl(
    private val accountsApi: AccountsApi,
    private val sessionManager: SessionManager
) : AccountsRepository {

    override suspend fun getAccountBalances(
        householdId: String,
        userId: String
    ): AppResult<List<AccountBalance>> {
        return try {
            val accounts = accountsApi.getAccounts(
                householdId = "eq.$householdId"
            )
            val balances = accounts
                .filter { dto ->
                    val nameLower = dto.name.lowercase()
                    !nameLower.contains("system")
                }
                .filter { dto ->
                    // Show shared accounts + personal accounts owned by this user
                    (dto.isShared ?: true) || dto.ownerUserId == userId
                }
                .map { dto ->
                    AccountBalance(
                        accountId       = dto.id,
                        accountName     = dto.name,
                        accountType     = dto.accountType ?: "ASSET",
                        movementBalance = dto.balance?.toLong() ?: 0L,
                        isShared        = dto.isShared ?: true
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
            val request = CreateAccountRequest(
                name         = name,
                accountType  = accountType.uppercase(),
                householdId  = householdId,
                currencyCode = "CLP",
                isShared     = isShared,
                ownerUserId  = if (isShared) null else ownerUserId
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
                    ownerUserId = if (isShared) null else ownerUserId
                )
            )
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al crear cuenta")
        }
    }
}
