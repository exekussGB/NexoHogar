package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.AccountBalance

interface AccountsRepository {
    suspend fun getAccountBalances(householdId: String): AppResult<List<AccountBalance>>
    suspend fun getAccounts(householdId: String): AppResult<List<Account>>
    suspend fun createAccount(
        householdId: String,
        name: String,
        accountType: String,
        accountSubtype: String = "other",
        isShared: Boolean = true,
        ownerUserId: String? = null,
        initialBalanceCLP: Double? = null,
        isSavings: Boolean = false,    // 🆕 Feature 2
        icon: String? = null           // 🆕 Custom icon
    ): AppResult<Account>
    suspend fun deleteAccount(accountId: String): AppResult<Unit>
    suspend fun updateAccount(
        accountId : String,
        name      : String,
        isSavings : Boolean,
        isShared  : Boolean,
        icon      : String? = null     // 🆕 Custom icon
    ): AppResult<Unit>
    suspend fun hasPersonalAccounts(householdId: String, userId: String): AppResult<Boolean>
    suspend fun getPersonalAccountBalances(householdId: String, userId: String): AppResult<List<AccountBalance>>
}
