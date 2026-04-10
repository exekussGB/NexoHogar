package com.nexohogar.domain.model

data class UserUsage(
    val isPremium: Boolean,
    val planName: String,
    val products: LimitUsage,
    val inventory: LimitUsage,
    val wishlist: LimitUsage,
    val suggestions: LimitUsage,
    val recurring: LimitUsage,
    val accounts: LimitUsage,
    val members: LimitUsage
) {
    data class LimitUsage(
        val used: Int,
        val limit: Int
    ) {
        val isAtLimit: Boolean
            get() = limit > 0 && used >= limit

        val percentage: Float
            get() = if (limit > 0) used.toFloat() / limit else 0f
    }
}
