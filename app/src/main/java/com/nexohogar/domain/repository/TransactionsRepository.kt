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
    suspend fun getTransactions(householdId: String): AppResult<List<Transaction>>
    
    suspend fun createTransaction(
        request: CreateTransactionRequest
    ): AppResult<Unit>

    suspend fun createTransfer(
        request: CreateTransferRequest
    ): AppResult<Unit>

    suspend fun getAccounts(householdId: String): AppResult<List<Account>>
}
