package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.AccountsApi
import com.nexohogar.data.remote.dto.CreateAccountRequest
import com.nexohogar.data.remote.dto.toDomain
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.repository.AccountsRepository
import com.nexohogar.data.remote.dto.SoftDeleteAccountRequest
import com.nexohogar.data.remote.dto.UpdateAccountRequest

class AccountsRepositoryImpl(
    private val accountsApi   : AccountsApi
) : AccountsRepository {

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
                            !dto.accountName.startsWith("_") &&
                            dto.isShared // 🛡️ Solo cuentas compartidas en el dashboard general
                }
                .map { it.toDomain() }
            AppResult.Success(domain)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al obtener cuentas")
        }
    }

    override suspend fun getAccounts(
        householdId: String
    ): AppResult<List<Account>> {
        return try {
            val balances = accountsApi.getBalances(
                householdId = "eq.$householdId"
            )
            val accounts = balances
                .filter { dto ->
                    !dto.accountName.lowercase().contains("system") &&
                            !dto.accountName.startsWith("_")
                }
                .map { dto ->
                    Account(
                        id          = dto.accountId,
                        name        = dto.accountName,
                        type        = dto.accountType,
                        balance     = dto.balanceClp.toLong(),
                        householdId = householdId,
                        isShared    = dto.isShared,
                        ownerUserId = dto.ownerUserId,
                        isSavings   = dto.isSavings,
                        isLiability = dto.isLiability,
                        icon        = dto.icon,
                        creditLimit = dto.creditLimit?.toLong()
                    )
                }
            AppResult.Success(accounts)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al obtener cuentas")
        }
    }

    override suspend fun hasPersonalAccounts(
        householdId: String,
        userId: String
    ): AppResult<Boolean> {
        return try {
            val balances = accountsApi.getBalances(
                householdId = "eq.$householdId"
            )
            val hasPersonal = balances.any { !it.isShared && it.ownerUserId == userId }
            AppResult.Success(hasPersonal)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al verificar cuentas personales")
        }
    }

    override suspend fun getPersonalAccountBalances(
        householdId: String,
        userId: String
    ): AppResult<List<AccountBalance>> {
        return try {
            val balances = accountsApi.getBalances(
                householdId = "eq.$householdId"
            )
            val personal = balances
                .filter { !it.isShared && it.ownerUserId == userId }
                .map { it.toDomain() }
            AppResult.Success(personal)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al obtener cuentas personales")
        }
    }

    override suspend fun createAccount(
        householdId   : String,
        name          : String,
        accountType   : String,
        accountSubtype: String,
        isShared      : Boolean,
        ownerUserId   : String?,
        initialBalanceCLP: Double?,
        isSavings     : Boolean,    // 🆕 Feature 2
        icon          : String?,    // 🆕 Custom icon
        creditLimit   : Long?       // 🆕 Feature 4
    ): AppResult<Account> {
        return try {
            val request = CreateAccountRequest(
                name          = name,
                accountType   = accountType.uppercase(),
                householdId   = householdId,
                currencyCode  = "CLP",
                accountSubtype = accountSubtype,
                isShared      = isShared,
                ownerUserId   = ownerUserId,
                initialBalanceCLP = initialBalanceCLP,
                isSavings     = isSavings,    // 🆕 Feature 2
                icon          = icon,         // 🆕 Custom icon
                creditLimit   = creditLimit?.toDouble()
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
                    householdId = householdId,
                    isSavings   = created.isSavings ?: false,    // 🆕 Feature 2
                    isLiability = created.isLiability ?: false,  // 🆕 Feature 3
                    icon        = created.icon,                   // 🆕 Custom icon
                    creditLimit = created.creditLimit?.toLong()
                )
            )
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al crear cuenta")
        }
    }

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

    override suspend fun updateAccount(
        accountId : String,
        name      : String,
        isSavings : Boolean,
        isShared  : Boolean,
        icon      : String?,    // 🆕 Custom icon
        creditLimit: Long?      // 🆕 Feature 4
    ): AppResult<Unit> {
        return try {
            val response = accountsApi.updateAccount(
                id   = "eq.$accountId",
                body = UpdateAccountRequest(
                    name      = name,
                    isSavings = isSavings,
                    isShared  = isShared,
                    icon      = icon,        // 🆕 Custom icon
                    creditLimit = creditLimit?.toDouble()
                )
            )
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error("Error al actualizar cuenta: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al actualizar cuenta")
        }
    }
}
