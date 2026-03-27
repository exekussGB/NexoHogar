package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.AccountsApi
import com.nexohogar.data.model.CreateAccountRequest
import com.nexohogar.data.remote.dto.toDomain
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.repository.AccountsRepository
import com.nexohogar.data.model.SoftDeleteAccountRequest

class AccountsRepositoryImpl(
    private val accountsApi   : AccountsApi,
    private val sessionManager: SessionManager
) : AccountsRepository {

    /**
     * Obtiene los saldos reales desde la VISTA account_balances.
     * balance_clp = initial_balance_clp + suma de transaction_entries.
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
     * Retorna true si el usuario tiene al menos una cuenta personal (is_shared = false).
     */
    override suspend fun hasPersonalAccounts(
        householdId: String,
        userId: String
    ): AppResult<Boolean> {
        return try {
            val balances = accountsApi.getBalances(
                householdId = "eq.$householdId"
            )
            val hasPersonal = balances.any { it.isShared == false && it.ownerUserId == userId }
            AppResult.Success(hasPersonal)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al verificar cuentas personales")
        }
    }

    /**
     * Retorna los saldos de cuentas personales del usuario (is_shared = false).
     */
    override suspend fun getPersonalAccountBalances(
        householdId: String,
        userId: String
    ): AppResult<List<AccountBalance>> {
        return try {
            val balances = accountsApi.getBalances(
                householdId = "eq.$householdId"
            )
            val personal = balances
                .filter { it.isShared == false && it.ownerUserId == userId }
                .map { it.toDomain() }
            AppResult.Success(personal)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al obtener cuentas personales")
        }
    }

    /**
     * Crea una nueva cuenta.
     * account_type debe ir en MAYÚSCULAS — CHECK constraint: ASSET, LIABILITY, EXPENSE, INCOME
     */
    override suspend fun createAccount(
        householdId   : String,
        name          : String,
        accountType   : String,
        accountSubtype: String,
        isShared      : Boolean,
        ownerUserId   : String?
    ): AppResult<Account> {
        return try {
            val request = CreateAccountRequest(
                name          = name,
                accountType   = accountType.uppercase(),
                householdId   = householdId,
                currencyCode  = "CLP",
                accountSubtype = accountSubtype,
                isShared      = isShared,
                ownerUserId   = ownerUserId
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

    /**
     * Elimina una cuenta por ID (DELETE en Supabase).
     */
    override suspend fun deleteAccount(accountId: String): AppResult<Unit> {
        return try {
            val response = accountsApi.deleteAccount(
                id = "eq.$accountId",
                body = SoftDeleteAccountRequest()
            )
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error("Error al eliminar cuenta: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al eliminar cuenta")
        }
    }
}
