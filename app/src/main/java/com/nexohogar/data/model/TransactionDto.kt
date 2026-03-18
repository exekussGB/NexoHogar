package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.TransactionEntry

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
    val status: String?,
    @SerializedName("account_id")
    val accountId: String?
)

/**
 * Mapper extension to convert DTO to Domain model (TransactionEntry).
 * Se mapea la transacción como una entrada para mantener compatibilidad con la UI de detalles.
 */
fun TransactionDto.toTransactionEntry(): TransactionEntry {
    return TransactionEntry(
        entryId = id ?: "",
        accountName = "Cuenta ID: ${accountId ?: "N/A"}", // Idealmente se obtendría el nombre mediante un join
        accountType = type ?: "N/A",
        entryType = if ((amountClp ?: 0.0) >= 0) "Ingreso" else "Egreso",
        amountClp = amountClp ?: 0.0
    )
}

fun List<TransactionDto>.toDomain(): List<TransactionEntry> {
    return this.map { it.toTransactionEntry() }
}
