package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.TransactionDetail

interface TransactionDetailRepository {
    suspend fun getTransactionDetail(transactionId: String): AppResult<TransactionDetail>

    // Feature 1: Editar transacción (solo super_user)
    // Todos los parámetros son obligatorios — la RPC los requiere no-nulos.
    suspend fun updateTransaction(
        transactionId  : String,
        householdId    : String,
        amountClp      : Long,
        description    : String,
        transactionDate: String
    ): AppResult<Unit>
}
