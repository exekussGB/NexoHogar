package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.InventoryCategory

data class InventoryCategoryDto(
    @SerializedName("id")           val id: String,
    @SerializedName("household_id") val householdId: String,
    @SerializedName("name")         val name: String,
    @SerializedName("icon")         val icon: String? = null,
    @SerializedName("sort_order")   val sortOrder: Int = 0,
    @SerializedName("created_at")   val createdAt: String? = null
) {
    fun toDomain() = InventoryCategory(
        id = id,
        householdId = householdId,
        name = name,
        icon = icon,
        sortOrder = sortOrder
    )
}
