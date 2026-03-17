package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para la creación de transacciones (INCOME y EXPENSE) vía RPC.
 */
data class CreateTransactionRequest(
    @SerializedName("p_household_id") val pHouseholdId: String,
    @SerializedName("p_type") val pType: String,
    @SerializedName("p_account_id") val pAccountId: String,
    @SerializedName("p_to_account_id") val pToAccountId: String? = null,
    @SerializedName("p_amount_clp") val pAmountClp: Long,
    @SerializedName("p_category_id") val pCategoryId: String? = null,
    @SerializedName("p_description") val pDescription: String? = null,
    @SerializedName("p_transaction_date") val pTransactionDate: String
)
