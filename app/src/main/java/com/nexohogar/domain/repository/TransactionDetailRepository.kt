package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.TransactionDetail

interface TransactionDetailRepository {
    suspend fun getTransactionDetail(transactionId: String): AppResult<TransactionDetail>

    // 🆕 Feature 1: Editar transacción
    suspend fun updateTransaction(
        transactionId: String,
        amountClp: Long? = null,
        description: String? = null,
        transactionDate: String? = null,
        categoryId: String? = null
    ): AppResult<Unit>
}
