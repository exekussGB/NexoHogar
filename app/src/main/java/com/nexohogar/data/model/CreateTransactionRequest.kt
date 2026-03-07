package com.nexohogar.data.model

/**
 * Request body for creating a transaction via Supabase RPC.
 */
data class CreateTransactionRequest(
    val p_household_id: String,
    val p_account_id: String,
    val p_amount: Double,
    val p_type: String,
    val p_description: String?,
    val p_transaction_date: String
)
