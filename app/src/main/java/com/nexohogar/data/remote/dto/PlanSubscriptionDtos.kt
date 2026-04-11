package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PlanDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("price_monthly")
    val priceMonthly: Double,
    @SerializedName("currency")
    val currency: String = "CLP",
    @SerializedName("max_households")
    val maxHouseholds: Int? = null,
    @SerializedName("max_members_per_household")
    val maxMembersPerHousehold: Int? = null,
    @SerializedName("max_products")
    val maxProducts: Int? = null,
    @SerializedName("max_inventory_items")
    val maxInventoryItems: Int? = null,
    @SerializedName("max_wishlist_items")
    val maxWishlistItems: Int? = null,
    @SerializedName("max_future_purchases")
    val maxFuturePurchases: Int? = null,
    @SerializedName("max_recurring")
    val maxRecurring: Int? = null,
    @SerializedName("max_accounts")
    val maxAccounts: Int? = null,
    @SerializedName("features")
    val features: List<String> = emptyList(),
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class SubscriptionDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("plan_id")
    val planId: String,
    @SerializedName("plan")
    val plan: PlanDto? = null,
    @SerializedName("status")
    val status: String,
    @SerializedName("current_period_start")
    val currentPeriodStart: String,
    @SerializedName("current_period_end")
    val currentPeriodEnd: String? = null,
    @SerializedName("trial_ends_at")
    val trialEndsAt: String? = null,
    @SerializedName("external_customer_id")
    val externalCustomerId: String? = null,
    @SerializedName("external_subscription_id")
    val externalSubscriptionId: String? = null,
    @SerializedName("payment_provider")
    val paymentProvider: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)
