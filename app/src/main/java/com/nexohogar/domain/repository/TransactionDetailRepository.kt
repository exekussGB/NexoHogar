package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.TransactionDetail

interface TransactionDetailRepository {
    suspend fun getTransactionDetail(transactionId: String): AppResult<TransactionDetail>
}