package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ImportReceiptItemDto(
    @SerializedName("name")
    val name: String,

    @SerializedName("quantity")
    val quantity: Double,

    @SerializedName("price_per_unit")
    val pricePerUnit: Double? = null,

    @SerializedName("price_total")
    val priceTotal: Double? = null,

    @SerializedName("brand")
    val brand: String? = null,

    @SerializedName("unit")
    val unit: String = "unidad",

    @SerializedName("category")
    val category: String? = null
)