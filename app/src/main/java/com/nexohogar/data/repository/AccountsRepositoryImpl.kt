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

    // -------------------------------------------------------------------------
    // getAccountBalances
    // Antes usaba la RPC get_account_balances (que no existe → HTTP 404).
    // Ahora consulta directamente la tabla accounts con el filtro PostgREST
    // correcto: "eq.{householdId}".
    // -------------------------------------------------------------------------
    override suspend fun getAccountBalances(
        householdId: String
    ): AppResult<List<AccountBalance>> {
        return try {
            val accounts = accountsApi.getAccounts(
                householdId = "eq.$householdId"
            )
            val balances = accounts.map { dto ->
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

    // -------------------------------------------------------------------------
    // createAccount
    // account_type debe ser LOWERCASE para cumplir el CHECK constraint de Supabase.
    // El repositorio normaliza el valor por si acaso llega en mayúsculas.
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
                accountType    = accountType.lowercase(),   // Garantizar lowercase
                accountSubtype = accountSubtype.lowercase(),
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
                    balance     = created.balance?.toLong() ?: 0L,
                    householdId = householdId
                )
            )
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al crear cuenta")
        }
    }
}
