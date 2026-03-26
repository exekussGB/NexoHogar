package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName

data class ExpenseByCategoryDto(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("total_amount") val totalAmount: Long,
    @SerializedName("transaction_count") val transactionCount: Long,
    @SerializedName("user_display_name") val userDisplayName: String?
)
