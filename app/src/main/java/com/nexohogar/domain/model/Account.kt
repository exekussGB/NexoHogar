package com.nexohogar.domain.model

data class Account(
    val id: String,
    val name: String,
    val type: String,
    val balance: Long,
    val householdId: String,
    val isShared: Boolean = true,
    val ownerUserId: String? = null
)
