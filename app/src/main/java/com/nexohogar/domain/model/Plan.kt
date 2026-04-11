package com.nexohogar.domain.model

data class Plan(
    val id: String,
    val name: String,
    val displayName: String,
    val priceMonthly: Double,
    val currency: String = "CLP",
    val maxHouseholds: Int? = null,
    val maxMembersPerHousehold: Int? = null,
    val maxProducts: Int? = null,
    val maxInventoryItems: Int? = null,
    val maxWishlistItems: Int? = null,
    val maxFuturePurchases: Int? = null,
    val maxRecurring: Int? = null,
    val maxAccounts: Int? = null,
    val features: List<String> = emptyList(),
    val isActive: Boolean = true
) {
    // Helper methods para verificar si un límite existe
    fun hasLimit(limitType: LimitType): Boolean = when (limitType) {
        LimitType.PRODUCTS -> maxProducts != null
        LimitType.INVENTORY_ITEMS -> maxInventoryItems != null
        LimitType.WISHLIST_ITEMS -> maxWishlistItems != null
        LimitType.FUTURE_PURCHASES -> maxFuturePurchases != null
        LimitType.RECURRING -> maxRecurring != null
        LimitType.ACCOUNTS -> maxAccounts != null
        LimitType.MEMBERS -> maxMembersPerHousehold != null
    }

    fun getLimit(limitType: LimitType): Int? = when (limitType) {
        LimitType.PRODUCTS -> maxProducts
        LimitType.INVENTORY_ITEMS -> maxInventoryItems
        LimitType.WISHLIST_ITEMS -> maxWishlistItems
        LimitType.FUTURE_PURCHASES -> maxFuturePurchases
        LimitType.RECURRING -> maxRecurring
        LimitType.ACCOUNTS -> maxAccounts
        LimitType.MEMBERS -> maxMembersPerHousehold
    }

    fun isPremium(): Boolean = maxProducts == null && maxAccounts == null && maxRecurring == null
}

enum class LimitType {
    PRODUCTS,
    INVENTORY_ITEMS,
    WISHLIST_ITEMS,
    FUTURE_PURCHASES,
    RECURRING,
    ACCOUNTS,
    MEMBERS
}
