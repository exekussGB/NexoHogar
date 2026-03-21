package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.model.toDomain
import com.nexohogar.data.network.AccountsApi
import com.nexohogar.data.remote.dto.CreateAccountRequest
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.repository.AccountsRepository

class AccountsRepositoryImpl(
    private val accountsApi: AccountsApi,
    private val sessionManager: SessionManager   // mantenido por compatibilidad con ServiceLocator
) : AccountsRepository {

    // -------------------------------------------------------------------------
    // getAccountBalances
    // AccountsApi.getAccountBalances(householdId) → no lleva token (usa AuthInterceptor)
    // AccountBalanceDto.toDomain() → AccountBalance con movementBalance: Double
    // -------------------------------------------------------------------------
    override suspend fun getAccountBalances(
        householdId: String
    ): AppResult<List<AccountBalance>> {
        return try {
            val balances = accountsApi.getAccountBalances(householdId)
            AppResult.Success(balances.map { it.toDomain() })
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al obtener balances de cuentas")
        }
    }

    // -------------------------------------------------------------------------
    // createAccount
    // AccountsApi.createAccount(request) → no lleva token (usa AuthInterceptor)
    // Account.type (no accountType), Account.balance: Double
    // -------------------------------------------------------------------------
    override suspend fun createAccount(
        householdId: String,
        name: String,
        accountType: String,
        accountSubtype: String
    ): AppResult<Account> {
        return try {
            val request = CreateAccountRequest(
                name           = name,
                accountType    = accountType,
                accountSubtype = accountSubtype,
                householdId    = householdId,
                isActive       = true
            )

            val response = accountsApi.createAccount(request)

            val created = response.firstOrNull()
                ?: return AppResult.Error("No se recibió respuesta del servidor")

            AppResult.Success(
                Account(
                    id          = created.id,
                    name        = created.name,
                    type        = created.accountType ?: accountType,
                    balance     = created.balance ?: 0.0,
                    householdId = householdId
                )
            )
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al crear cuenta")
        }
    }
}
