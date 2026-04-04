package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.remote.dto.CreateTransactionRequest
import com.nexohogar.data.remote.dto.CreateTransferRequest
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.Transaction

/**
 * Interface for the transactions repository.
 */
interface TransactionsRepository {
    suspend fun getTransactions(householdId: String, limit: Int = 30, offset: Int = 0): AppResult<List<Transaction>>
    
    suspend fun createTransaction(
        request: CreateTransactionRequest
    ): AppResult<Unit>

    suspend fun createTransfer(
        request: CreateTransferRequest
    ): AppResult<Unit>

    suspend fun getTransactionsByAccount(
        householdId: String,
        accountId: String,
        limit: Int = 10
    ): AppResult<List<Transaction>>

    suspend fun getAccounts(householdId: String): AppResult<List<Account>>
}
