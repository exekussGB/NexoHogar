package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.TransactionEntry

/**
 * Interface for the transaction detail repository.
 */
interface TransactionDetailRepository {
    suspend fun getTransactionEntries(transactionId: String): AppResult<List<TransactionEntry>>
}
