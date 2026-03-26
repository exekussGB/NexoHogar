package com.nexohogar.domain.model

data class TransactionDetail(
    val id: String,
    val type: String,           // "income" | "expense" | "transfer"
    val description: String?,
    val transactionDate: String?,
    val amountClp: Long,
    val status: String?,
    val fromAccountId: String?,
    val toAccountId: String?,
    val fromAccountName: String?,
    val toAccountName: String?,
    val createdByName: String? = null
)
