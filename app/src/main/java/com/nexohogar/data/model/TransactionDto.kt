package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.Transaction

/**
 * Data Transfer Object for Transaction from Supabase view.
 * Updated to handle nulls from the database.
 */
data class TransactionDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("transaction_date")
    val transactionDate: String?,
    @SerializedName("amount_clp")
    val amountClp: Double?,
    @SerializedName("status")
    val status: String?
)

/**
 * Mapper extension to convert DTO to Domain model.
 * Handles null values safely.
 */
fun TransactionDto.toDomain(): Transaction {
    return Transaction(
        id = id,
        type = type,
        description = description,
        transactionDate = transactionDate,
        amountClp = amountClp,
        status = status ?: "pending"
    )
}

fun List<TransactionDto>.toDomain(): List<Transaction> {
    return this.map { it.toDomain() }
}
