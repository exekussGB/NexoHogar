package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.model.CreateAccountRequest
import com.nexohogar.data.model.toDomain
import com.nexohogar.data.network.AccountsApi
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.repository.AccountsRepository

class AccountsRepositoryImpl(
    private val accountsApi: AccountsApi,
    private val sessionManager: SessionManager
) : AccountsRepository {

    /**
     * Obtiene balances usando el campo estático (compatibilidad).
     * Filtra cuentas del sistema y cuentas eliminadas (filtro en API).
     */
    override suspend fun getAccountBalances(
        householdId: String
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
                .map { dto ->
                    AccountBalance(
                        accountId       = dto.id,
                        accountName     = dto.name,
                        accountType     = dto.accountType ?: "ASSET",
                        movementBalance = dto.balance?.toLong() ?: 0L
                    )
                }
            AppResult.Success(balances)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al obtener cuentas")
        }
    }

    /**
     * Obtiene saldos CALCULADOS desde transacciones reales vía RPC.
     */
    override suspend fun getCalculatedBalances(
        householdId: String
    ): AppResult<List<AccountBalance>> {
        return try {
            val dtos = accountsApi.getCalculatedBalances(
                body = mapOf("p_household_id" to householdId)
            )
            AppResult.Success(dtos.toDomain())
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al obtener saldos calculados")
        }
    }

    /**
     * Crea una nueva cuenta para el hogar.
     */
    override suspend fun createAccount(
        householdId: String,
        name: String,
        accountType: String,
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

    /**
     * Elimina una cuenta (soft delete vía RPC).
     */
    override suspend fun deleteAccount(accountId: String): AppResult<Unit> {
        return try {
            val response = accountsApi.deleteAccount(
                body = mapOf("p_account_id" to accountId)
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