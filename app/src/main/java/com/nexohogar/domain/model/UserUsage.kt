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
    data class LimitUsage(val used: Int, val limit: Int) {
        val isUnlimited: Boolean get() = limit == Int.MAX_VALUE
        val isAtLimit: Boolean get() = !isUnlimited && used >= limit
        val percentage: Float get() = if (isUnlimited || limit == 0) 0f else used.toFloat() / limit
        val displayLimit: String get() = if (isUnlimited) "∞" else limit.toString()
    }
}
