package com.nexohogar.data.remote.dto

/**
 * The unified DTO for transaction responses.
 * This class matches the updated 'select' query parameters used in TransactionsApi.
 */
data class TransactionResponse(
    val id: String,
    val description: String?,
    val amount_clp: Double,
    val created_at: String,
    val account_id: String,
    val household_id: String,
    val type: String
)
