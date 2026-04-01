package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.WishlistItem

data class WishlistItemDto(
    @SerializedName("id")            val id: String,
    @SerializedName("household_id")  val householdId: String,
    @SerializedName("name")          val name: String,
    @SerializedName("description")   val description: String? = null,
    @SerializedName("url")           val url: String? = null,
    @SerializedName("price")         val price: Double? = null,
    @SerializedName("priority")      val priority: String = "MEDIUM",
    @SerializedName("is_purchased")  val isPurchased: Boolean = false,
    @SerializedName("purchased_at")  val purchasedAt: String? = null,
    @SerializedName("created_by")    val createdBy: String = "",
    @SerializedName("created_at")    val createdAt: String = "",
    @SerializedName("updated_at")    val updatedAt: String = ""
) {
    fun toDomain() = WishlistItem(
        id          = id,
        householdId = householdId,
        name        = name,
        description = description,
        url         = url,
        price       = price,
        priority    = priority,
        isPurchased = isPurchased,
        purchasedAt = purchasedAt,
        createdBy   = createdBy,
        createdAt   = createdAt,
        updatedAt   = updatedAt
    )
}

data class CreateWishlistItemRequest(
    @SerializedName("household_id") val householdId: String,
    @SerializedName("name")         val name: String,
    @SerializedName("description")  val description: String? = null,
    @SerializedName("url")          val url: String? = null,
    @SerializedName("price")        val price: Double? = null,
    @SerializedName("priority")     val priority: String = "MEDIUM",
    @SerializedName("created_by")   val createdBy: String
)

data class UpdateWishlistItemRequest(
    @SerializedName("name")          val name: String? = null,
    @SerializedName("description")   val description: String? = null,
    @SerializedName("url")           val url: String? = null,
    @SerializedName("price")         val price: Double? = null,
    @SerializedName("priority")      val priority: String? = null,
    @SerializedName("is_purchased")  val isPurchased: Boolean? = null,
    @SerializedName("purchased_at")  val purchasedAt: String? = null,
    @SerializedName("updated_at")    val updatedAt: String? = null
)
