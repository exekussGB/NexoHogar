package com.nexohogar.domain.model

/**
 * Domain model representing a financial transaction.
 * Updated to allow nulls for fields that may be empty in Supabase.
 */
data class Transaction(
    val id: String,
    val type: String,
    val description: String?,
    val transactionDate: String?,
    val amountClp: Double?,
    val status: String
)
