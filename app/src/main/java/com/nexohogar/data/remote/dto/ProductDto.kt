package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.Product

data class ProductDto(
    @SerializedName("id")           val id: String,
    @SerializedName("household_id") val householdId: String,
    @SerializedName("name")         val name: String,
    @SerializedName("unit")         val unit: String,
    @SerializedName("brand")        val brand: String? = null,
    @SerializedName("category")     val category: String? = null,
    @SerializedName("notes")        val notes: String? = null,
    @SerializedName("created_at")   val createdAt: String? = null,
    @SerializedName("min_stock")    val minStock: Int? = null
) {
    fun toDomain(currentStock: Double = 0.0) = Product(
        id = id,
        householdId = householdId,
        name = name,
        unit = unit,
        brand = brand,
        category = category,
        notes = notes,
        createdAt = createdAt,
        currentStock = currentStock,
        minStock = minStock
    )
}
