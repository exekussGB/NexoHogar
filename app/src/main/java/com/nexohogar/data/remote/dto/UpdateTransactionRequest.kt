package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request DTO para editar una transacción (solo super_user).
 * Llama a la RPC rpc_update_transaction(p_transaction_id, p_household_id,
 * p_amount_clp, p_description, p_transaction_date) en Supabase.
 *
 * IMPORTANTE: todos los campos son obligatorios en la RPC — siempre enviar
 * los valores actuales de la transacción (nunca null).
 */
data class UpdateTransactionRequest(
    @SerializedName("p_transaction_id")   val transactionId: String,
    @SerializedName("p_household_id")     val householdId: String,
    @SerializedName("p_amount_clp")       val amountClp: Long,
    @SerializedName("p_description")      val description: String,
    @SerializedName("p_transaction_date") val transactionDate: String
)
