package com.nexohogar.domain.model

data class Household(
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUri: String? = null,
    val gradientIndex: Int = 0
)
