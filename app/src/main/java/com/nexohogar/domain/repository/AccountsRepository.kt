package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.AccountBalance

/**
 * Interfaz para el repositorio de cuentas.
 */
interface AccountsRepository {
    suspend fun getAccountBalances(householdId: String): AppResult<List<AccountBalance>>
}
