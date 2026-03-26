package com.nexohogar.domain.model

data class CategoryExpenseByUser(
    val categoryName: String,
    val userId: String?,
    val userName: String,
    val totalAmount: Long,
    val percentage: Double
)

data class CategoryExpenseGroup(
    val categoryName: String,
    val totalAmount: Long,
    val percentage: Double,
    val users: List<CategoryExpenseByUser>
)
