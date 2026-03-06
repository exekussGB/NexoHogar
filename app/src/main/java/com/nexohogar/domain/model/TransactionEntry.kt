package com.nexohogar.domain.model

/**
 * Domain model representing an entry (debit or credit) within a transaction.
 */
data class TransactionEntry(
    val entryId: String,
    val accountName: String,
    val accountType: String,
    val entryType: String, // "debit" or "credit"
    val amountClp: Double
)
