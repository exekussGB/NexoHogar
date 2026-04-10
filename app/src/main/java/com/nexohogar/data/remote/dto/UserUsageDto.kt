package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.UserUsage

data class UserUsageDto(
    @SerializedName("plan_name")        val planName: String  = "free",
    @SerializedName("is_premium")       val isPremium: Boolean = false,
    @SerializedName("products_used")    val productsUsed: Int  = 0,
    @SerializedName("products_limit")   val productsLimit: Int = 10,
    @SerializedName("inventory_used")   val inventoryUsed: Int  = 0,
    @SerializedName("inventory_limit")  val inventoryLimit: Int = 50,
    @SerializedName("wishlist_used")    val wishlistUsed: Int  = 0,
    @SerializedName("wishlist_limit")   val wishlistLimit: Int = 10,
    @SerializedName("suggestions_used")  val suggestionsUsed: Int  = 0,
    @SerializedName("suggestions_limit") val suggestionsLimit: Int = 5,
    @SerializedName("recurring_used")   val recurringUsed: Int  = 0,
    @SerializedName("recurring_limit")  val recurringLimit: Int = 5,
    @SerializedName("accounts_used")    val accountsUsed: Int  = 0,
    @SerializedName("accounts_limit")   val accountsLimit: Int = 3,
    @SerializedName("members_used")     val membersUsed: Int  = 0,
    @SerializedName("members_limit")    val membersLimit: Int = 3,
) {
    fun toDomain() = UserUsage(
        isPremium   = isPremium,
        planName    = planName,
        products    = UserUsage.LimitUsage(productsUsed,    productsLimit),
        inventory   = UserUsage.LimitUsage(inventoryUsed,   inventoryLimit),
        wishlist    = UserUsage.LimitUsage(wishlistUsed,    wishlistLimit),
        suggestions = UserUsage.LimitUsage(suggestionsUsed, suggestionsLimit),
        recurring   = UserUsage.LimitUsage(recurringUsed,   recurringLimit),
        accounts    = UserUsage.LimitUsage(accountsUsed,    accountsLimit),
        members     = UserUsage.LimitUsage(membersUsed,     membersLimit),
    )
}
