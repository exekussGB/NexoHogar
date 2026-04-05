package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 🆕 Feature 1: Request DTO para editar una transacción (solo super_user).
 * Llama a la RPC rpc_update_transaction en Supabase.
 * Solo permite editar: monto, descripción, fecha y categoría.
 * NO permite cambiar tipo ni cuentas (integridad contable).
 */
data class UpdateTransactionRequest(
    @SerializedName("p_transaction_id") val transactionId: String,
    @SerializedName("p_amount_clp") val amountClp: Long? = null,
    @SerializedName("p_description") val description: String? = null,
    @SerializedName("p_transaction_date") val transactionDate: String? = null,
    @SerializedName("p_category_id") val categoryId: String? = null
)
