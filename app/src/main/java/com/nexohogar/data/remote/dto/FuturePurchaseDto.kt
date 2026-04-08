package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import java.time.Instant

data class FuturePurchaseDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("household_id")
    val householdId: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("category")
    val category: String? = null,

    @SerializedName("priority")
    val priority: String = "medium",

    @SerializedName("estimated_price")
    val estimatedPrice: Double? = null,

    @SerializedName("is_purchased")
    val isPurchased: Boolean = false,

    @SerializedName("purchased_at")
    val purchasedAt: String? = null,

    @SerializedName("created_by")
    val createdBy: String,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null
)
