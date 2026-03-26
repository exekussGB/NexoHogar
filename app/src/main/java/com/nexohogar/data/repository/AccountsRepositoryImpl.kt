package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.AccountsApi
import com.nexohogar.data.model.CreateAccountRequest
import com.nexohogar.data.remote.dto.toDomain
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.repository.AccountsRepository

class AccountsRepositoryImpl(
    private val accountsApi   : AccountsApi,
    private val sessionManager: SessionManager
) : AccountsRepository {

    /**
     * Obtiene los saldos reales desde la VISTA account_balances.
     * balance_clp = initial_balance_clp + suma de transaction_entries.
     * Filtra cuentas del sistema.
     */
    override suspend fun getAccountBalances(
        householdId: String
    ): AppResult<List<AccountBalance>> {
        return try {
            val balances = accountsApi.getBalances(
                householdId = "eq.$householdId"
            )
            val domain = balances
                .filter { dto ->
                    !dto.accountName.lowercase().contains("system") &&
                            !dto.accountName.startsWith("_")
                }
                .map { it.toDomain() }
            AppResult.Success(domain)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al obtener cuentas")
        }
    }

    /**
     * Crea una nueva cuenta.
     * account_type debe ir en MAYÚSCULAS — CHECK constraint: ASSET, LIABILITY, EXPENSE, INCOME
     * currency_code es NOT NULL → se envía "CLP"
     */
    override suspend fun createAccount(
        householdId  : String,
        name         : String,
        accountType  : String,
        accountSubtype: String
    ): AppResult<Account> {
        return try {
            val request = CreateAccountRequest(
                name         = name,
                accountType  = accountType.uppercase(),
                householdId  = householdId,
                currencyCode = "CLP"
            )
            val response = accountsApi.createAccount(request)
            val created  = response.firstOrNull()
                ?: return AppResult.Error("No se recibió respuesta del servidor")

            AppResult.Success(
                Account(
                    id          = created.id,
                    name        = created.name,
                    type        = created.accountType ?: accountType,
                    balance     = created.balance?.toLong() ?: 0L,
                    householdId = householdId
                )
            )
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al crear cuenta")
        }
    }
}
