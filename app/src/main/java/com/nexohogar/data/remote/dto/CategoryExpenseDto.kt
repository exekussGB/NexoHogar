package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CategoryExpenseDto(
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("user_id")       val userId: String?,
    @SerializedName("user_name")     val userName: String?,
    @SerializedName("total_amount")  val totalAmount: Long,
    @SerializedName("percentage")    val percentage: Double
)
