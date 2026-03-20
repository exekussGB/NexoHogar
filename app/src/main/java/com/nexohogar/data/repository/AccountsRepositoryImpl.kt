package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.model.toDomain
import com.nexohogar.data.network.AccountsApi
import com.nexohogar.data.remote.dto.CreateAccountRequest
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.repository.AccountsRepository

/**
 * Implementación del repositorio de cuentas.
 * No necesita SessionManager porque AuthInterceptor inyecta el token en cada request.
 */
class AccountsRepositoryImpl(
    private val accountsApi: AccountsApi
) : AccountsRepository {

    override suspend fun getAccountBalances(
        householdId: String
    ): AppResult<List<AccountBalance>> {
        return try {
            val dtos = accountsApi.getAccountBalances(householdId)
            AppResult.Success(dtos.toDomain())
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al obtener balances")
        }
    }

    override suspend fun createAccount(
        householdId: String,
        name: String,
        accountType: String,
        accountSubtype: String
    ): AppResult<Account> {
        return try {
            val request = CreateAccountRequest(
                name = name,
                accountType = accountType,
                accountSubtype = accountSubtype,
                householdId = householdId,
                isActive = true
            )
            val response = accountsApi.createAccount(request)
            val created = response.firstOrNull()
                ?: return AppResult.Error("No se recibió respuesta del servidor")

            AppResult.Success(
                Account(
                    id = created.id,
                    name = created.name,
                    type = created.accountType ?: accountType,
                    balance = (created.balance ?: 0.0).toLong(),
                    householdId = householdId
                )
            )
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al crear cuenta")
        }
    }
}