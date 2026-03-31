package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.WishlistItem

data class WishlistItemDto(
    @SerializedName("id")            val id: String,
    @SerializedName("household_id")  val householdId: String,
    @SerializedName("name")          val name: String,
    @SerializedName("estimated_cost") val estimatedCost: Long = 0L,
    @SerializedName("notes")         val notes: String? = null,
    @SerializedName("priority")      val priority: Int = 2,
    @SerializedName("is_purchased")  val isPurchased: Boolean = false,
    @SerializedName("purchased_at")  val purchasedAt: String? = null,
    @SerializedName("purchased_by")  val purchasedBy: String? = null,
    @SerializedName("created_by")    val createdBy: String = "",
    @SerializedName("created_at")    val createdAt: String = "",
    @SerializedName("updated_at")    val updatedAt: String = ""
) {
    fun toDomain() = WishlistItem(
        id            = id,
        householdId   = householdId,
        name          = name,
        estimatedCost = estimatedCost,
        notes         = notes,
        priority      = priority,
        isPurchased   = isPurchased,
        purchasedAt   = purchasedAt,
        purchasedBy   = purchasedBy,
        createdBy     = createdBy,
        createdAt     = createdAt,
        updatedAt     = updatedAt
    )
}

data class CreateWishlistItemRequest(
    @SerializedName("household_id")   val householdId: String,
    @SerializedName("name")           val name: String,
    @SerializedName("estimated_cost") val estimatedCost: Long,
    @SerializedName("notes")          val notes: String? = null,
    @SerializedName("priority")       val priority: Int = 2,
    @SerializedName("created_by")     val createdBy: String
)

data class UpdateWishlistItemRequest(
    @SerializedName("name")           val name: String? = null,
    @SerializedName("estimated_cost") val estimatedCost: Long? = null,
    @SerializedName("notes")          val notes: String? = null,
    @SerializedName("priority")       val priority: Int? = null,
    @SerializedName("is_purchased")   val isPurchased: Boolean? = null,
    @SerializedName("purchased_at")   val purchasedAt: String? = null,
    @SerializedName("purchased_by")   val purchasedBy: String? = null,
    @SerializedName("updated_at")     val updatedAt: String? = null
)
