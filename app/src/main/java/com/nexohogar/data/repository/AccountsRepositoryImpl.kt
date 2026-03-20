package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.model.AccountDto
import com.nexohogar.data.network.AccountsApi
import com.nexohogar.data.remote.dto.CreateAccountRequest
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.repository.AccountsRepository

class AccountsRepositoryImpl(
    private val accountsApi: AccountsApi,
    private val sessionManager: SessionManager,
    private val tenantContext: TenantContext
) : AccountsRepository {

    override suspend fun getAccountBalances(): AppResult<List<Account>> {
        return try {
            val token = "Bearer ${sessionManager.getToken() ?: return AppResult.Error("No token")}"
            val householdId = tenantContext.requireHouseholdId()
            val balances = accountsApi.getAccountBalances(token, householdId)
            val accounts = balances.map { dto ->
                Account(
                    id = dto.id,
                    name = dto.name,
                    accountType = dto.accountType,
                    balance = dto.balance,
                    householdId = householdId
                )
            }
            AppResult.Success(accounts)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun createAccount(
        name: String,
        accountType: String,
        accountSubtype: String
    ): AppResult<Account> {
        return try {
            val token = "Bearer ${sessionManager.getToken() ?: return AppResult.Error("No token")}"
            val householdId = tenantContext.requireHouseholdId() // ← fix: usa requireHouseholdId() en lugar de getCurrentHouseholdId()
            val request = CreateAccountRequest(
                name = name,
                accountType = accountType,
                accountSubtype = accountSubtype,
                householdId = householdId,
                isActive = true
            )
            val response = accountsApi.createAccount(token, request)
            val created = response.firstOrNull()
                ?: return AppResult.Error("No se recibió respuesta del servidor")
            AppResult.Success(
                Account(
                    id = created.id,
                    name = created.name,
                    accountType = created.accountType,
                    balance = created.balance?.toLong() ?: 0L,
                    householdId = householdId
                )
            )
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al crear cuenta")
        }
    }
}