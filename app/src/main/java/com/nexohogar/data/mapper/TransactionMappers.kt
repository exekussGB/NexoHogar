package com.nexohogar.data.mapper

import com.nexohogar.data.remote.dto.TransactionResponse
import com.nexohogar.domain.model.Transaction

/**
 * Mappers para convertir DTOs de transacciones a modelos de dominio.
 */

fun TransactionResponse.toDomain(): Transaction {
    return Transaction(
        id = id,
        description = description,
        amount = amount_clp,
        accountId = account_id,
        createdAt = created_at
    )
}

fun List<TransactionResponse>.toDomain(): List<Transaction> {
    return this.map { it.toDomain() }
}
