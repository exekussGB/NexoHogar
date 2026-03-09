package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request body for creating a transaction via Supabase RPC.
 * Refactored to support double-entry accounting parameters.
 */
data class CreateTransactionRequest(
    @SerializedName("p_household_id")
    val householdId: String,
    
    @SerializedName("p_type")
    val type: String,
    
    @SerializedName("p_account_id")
    val accountId: String,
    
    @SerializedName("p_to_account_id")
    val toAccountId: String? = null,
    
    @SerializedName("p_amount_clp")
    val amountClp: Double,
    
    @SerializedName("p_category_id")
    val categoryId: String? = null,
    
    @SerializedName("p_description")
    val description: String?,
    
    @SerializedName("p_transaction_date")
    val transactionDate: String
)
