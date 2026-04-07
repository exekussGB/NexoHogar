package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateProductRequest(
    @SerializedName("household_id")  val householdId: String,
    @SerializedName("name")          val name: String,
    @SerializedName("unit")          val unit: String,
    @SerializedName("brand")         val brand: String? = null,
    @SerializedName("category")      val category: String? = null,
    @SerializedName("notes")         val notes: String? = null,
    @SerializedName("min_stock")     val minStock: Int? = null
)
