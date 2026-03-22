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

    /**
     * Obtiene los balances de las cuentas del hogar.
     * - Filtra las cuentas del sistema (_system_expense_, _system_income_) para no
     *   mostrarlas al usuario.
     */
    override suspend fun getAccountBalances(
        householdId: String
    ): AppResult<List<AccountBalance>> {
        return try {
            val accounts = accountsApi.getAccounts(
                householdId = "eq.$householdId"
            )
            val balances = accounts
                // Excluir cuentas internas del sistema
                .filter { dto ->
                    val nameLower = dto.name.lowercase()
                    !nameLower.startsWith("_system_") && nameLower != "_system_expense_" && nameLower != "_system_income_"
                }
                .map { dto ->
                    AccountBalance(
                        accountId       = dto.id,
                        accountName     = dto.name,
                        accountType     = dto.accountType ?: "asset",
                        movementBalance = dto.balance?.toLong() ?: 0L
                    )
                }
            AppResult.Success(balances)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al obtener cuentas")
        }
    }

    /**
     * Crea una nueva cuenta para el hogar.
     * account_type debe ser lowercase para cumplir el CHECK constraint de Supabase.
     */
    override suspend fun createAccount(
        householdId: String,
        name: String,
        accountType: String,
        accountSubtype: String
    ): AppResult<Account> {
        return try {
            val request = CreateAccountRequest(
                name        = name,
                accountType = accountType.lowercase(),
                householdId = householdId,
                balance     = 0.0,
                isActive    = true
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
                    householdId = householdId
                )
            )
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al crear cuenta")
        }
    }
}
