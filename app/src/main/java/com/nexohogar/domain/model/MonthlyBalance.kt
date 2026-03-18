package com.nexohogar.domain.model

data class MonthlyBalance(
    val yearNum: Int,
    val monthNum: Int,
    val income: Long,
    val expense: Long,
    val net: Long
)
