package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.AccountsApi
import com.nexohogar.data.remote.dto.CreateAccountRequest
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.repository.AccountsRepository

class AccountsRepositoryImpl(
    private val accountsApi: AccountsApi,
    private val sessionManager: SessionManager
) : AccountsRepository {

    override suspend fun getAccountBalances(householdId: String): AppResult<List<AccountBalance>> {
        return try {
            val token = "Bearer ${sessionManager.fetchAuthToken()
                ?: return AppResult.Error("No hay sesión activa")}"

            val dtos = accountsApi.getAccountBalances(token, householdId)

            val balances = dtos.map { dto ->
                AccountBalance(
                    accountId       = dto.accountId,
                    accountName     = dto.accountName,
                    accountType     = dto.accountType,
                    movementBalance = dto.movementBalance
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
        accountSubtype: String
    ): AppResult<Account> {
        return try {
            val token = "Bearer ${sessionManager.fetchAuthToken()
                ?: return AppResult.Error("No hay sesión activa")}"

            val request = CreateAccountRequest(
                name          = name,
                accountType   = accountType,
                accountSubtype = accountSubtype,
                householdId   = householdId,
                isActive      = true
            )
            val response = accountsApi.createAccount(token, request)
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