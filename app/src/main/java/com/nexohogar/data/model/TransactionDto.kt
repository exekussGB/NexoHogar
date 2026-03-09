package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName

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
