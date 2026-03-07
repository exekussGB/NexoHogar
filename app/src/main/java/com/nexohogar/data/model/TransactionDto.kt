package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.Transaction

/**
 * DTO para la respuesta de Supabase.
 * Permite nulos para manejar de forma segura datos incompletos de la base de datos.
 */
data class TransactionDto(
    @SerializedName("id")
    val id: String?,
    @SerializedName("type")
    val type: String?,
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
 * Mapper defensivo: Convierte el DTO (inseguro) al modelo de Dominio (seguro).
 * Garantiza que el dominio nunca reciba un nulo en campos críticos.
 */
fun TransactionDto.toDomain(): Transaction {
    return Transaction(
        id = id ?: "",
        type = type ?: "expense",
        description = description ?: "",
        transactionDate = transactionDate ?: "",
        amountClp = amountClp ?: 0.0,
        status = status ?: "pending"
    )
}

fun List<TransactionDto>.toDomain(): List<Transaction> {
    return this.map { it.toDomain() }
}
