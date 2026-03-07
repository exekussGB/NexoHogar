package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.model.CreateTransactionRequest
import com.nexohogar.domain.model.Transaction

/**
 * Interface for the transactions repository.
 */
interface TransactionsRepository {
    suspend fun getTransactions(householdId: String): AppResult<List<Transaction>>
    
    suspend fun createTransaction(
        request: CreateTransactionRequest
    ): AppResult<Unit>
}
