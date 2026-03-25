package com.nexohogar.domain.model

data class Budget(
    val id: String,
    val householdId: String,
    val categoryId: String,
    val categoryName: String,
    val amountClp: Double,
    val periodType: String,
    val yearNum: Int,
    val monthNum: Int?,
    val weekNum: Int?,
    val memberId: String?,
    val createdAt: String?
)