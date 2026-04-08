package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.FuturePurchase

data class FuturePurchaseDto(
    @SerializedName("id")               val id: String,
    @SerializedName("household_id")     val householdId: String,
    @SerializedName("name")             val name: String,
    @SerializedName("description")      val description: String? = null,
    @SerializedName("category")         val category: String? = null,
    @SerializedName("estimated_price")  val estimatedPrice: Double? = null,
    @SerializedName("priority")         val priority: String = "medium",
    @SerializedName("is_purchased")     val isPurchased: Boolean = false,
    @SerializedName("purchased_at")     val purchasedAt: String? = null,
    @SerializedName("created_by")       val createdBy: String,
    @SerializedName("created_at")       val createdAt: String = "",
    @SerializedName("updated_at")       val updatedAt: String = ""
) {
    fun toDomain() = FuturePurchase(
        id              = id,
        householdId     = householdId,
        name            = name,
        description     = description,
        category        = category,
        estimatedPrice  = estimatedPrice,
        priority        = priority,
        isPurchased     = isPurchased,
        purchasedAt     = purchasedAt,
        createdBy       = createdBy,
        createdAt       = createdAt,
        updatedAt       = updatedAt
    )
}

data class CreateFuturePurchaseRequest(
    @SerializedName("household_id")     val householdId: String,
    @SerializedName("name")             val name: String,
    @SerializedName("description")      val description: String? = null,
    @SerializedName("category")         val category: String? = null,
    @SerializedName("estimated_price")  val estimatedPrice: Double? = null,
    @SerializedName("priority")         val priority: String = "medium",
    @SerializedName("created_by")       val createdBy: String
)

data class UpdateFuturePurchaseRequest(
    @SerializedName("name")             val name: String? = null,
    @SerializedName("description")      val description: String? = null,
    @SerializedName("category")         val category: String? = null,
    @SerializedName("estimated_price")  val estimatedPrice: Double? = null,
    @SerializedName("priority")         val priority: String? = null,
    @SerializedName("is_purchased")     val isPurchased: Boolean? = null,
    @SerializedName("purchased_at")     val purchasedAt: String? = null,
    @SerializedName("updated_at")       val updatedAt: String? = null
)
