package com.nexohogar.domain.model

data class AccountBalance(
    val accountId: String,
    val accountName: String,
    val accountType: String,
    val movementBalance: Long,
    val isShared: Boolean = true,
    val ownerUserId: String? = null,
    val isSavings: Boolean = false,
    val isLiability: Boolean = false,
    val icon: String? = null,
    val creditLimit: Long? = null
)
