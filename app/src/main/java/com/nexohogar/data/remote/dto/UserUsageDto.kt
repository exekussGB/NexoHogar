package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.UserUsage

data class UserUsageDto(
    @SerializedName("plan_name")          val planName: String   = "free",
    @SerializedName("plan_display_name")  val planDisplayName: String = "Free",
    @SerializedName("is_premium")         val isPremium: Boolean = false,
    @SerializedName("price_monthly")      val priceMonthly: String = "0",
    @SerializedName("current_period_end") val currentPeriodEnd: String? = null,
    @SerializedName("products_used")      val productsUsed: Int  = 0,
    @SerializedName("products_limit")     val productsLimit: Int? = null,
    @SerializedName("inventory_used")     val inventoryUsed: Int  = 0,
    @SerializedName("inventory_limit")    val inventoryLimit: Int? = null,
    @SerializedName("wishlist_used")      val wishlistUsed: Int  = 0,
    @SerializedName("wishlist_limit")     val wishlistLimit: Int? = null,
    @SerializedName("suggestions_used")   val suggestionsUsed: Int  = 0,
    @SerializedName("suggestions_limit")  val suggestionsLimit: Int? = null,
    @SerializedName("recurring_used")     val recurringUsed: Int  = 0,
    @SerializedName("recurring_limit")    val recurringLimit: Int? = null,
    @SerializedName("accounts_used")      val accountsUsed: Int  = 0,
    @SerializedName("accounts_limit")     val accountsLimit: Int? = null,
) {
    // null en limit = ilimitado → usamos Int.MAX_VALUE como "sin límite"
    private fun limitOrUnlimited(limit: Int?) = limit ?: Int.MAX_VALUE

    fun toDomain() = UserUsage(
        isPremium   = isPremium,
        planName    = planName,
        products    = UserUsage.LimitUsage(productsUsed,    limitOrUnlimited(productsLimit)),
        inventory   = UserUsage.LimitUsage(inventoryUsed,   limitOrUnlimited(inventoryLimit)),
        wishlist    = UserUsage.LimitUsage(wishlistUsed,    limitOrUnlimited(wishlistLimit)),
        suggestions = UserUsage.LimitUsage(suggestionsUsed, limitOrUnlimited(suggestionsLimit)),
        recurring   = UserUsage.LimitUsage(recurringUsed,   limitOrUnlimited(recurringLimit)),
        accounts    = UserUsage.LimitUsage(accountsUsed,    limitOrUnlimited(accountsLimit)),
        members     = UserUsage.LimitUsage(0,               Int.MAX_VALUE),
    )
}