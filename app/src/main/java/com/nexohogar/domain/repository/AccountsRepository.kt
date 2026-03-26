package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.AccountBalance

interface AccountsRepository {
    suspend fun getAccountBalances(householdId: String, userId: String): AppResult<List<AccountBalance>>
    suspend fun createAccount(
        householdId: String,
        name: String,
        accountType: String,
        accountSubtype: String = "other",
        isShared: Boolean = true,
        ownerUserId: String? = null
    ): AppResult<Account>
}
