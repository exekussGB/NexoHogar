package com.nexohogar.data.mapper

import com.nexohogar.data.remote.dto.PlanDto
import com.nexohogar.data.remote.dto.SubscriptionDto
import com.nexohogar.domain.model.Plan
import com.nexohogar.domain.model.Subscription
import com.nexohogar.domain.model.SubscriptionStatus
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

fun PlanDto.toDomain(): Plan = Plan(
    id = this.id,
    name = this.name,
    displayName = this.displayName,
    priceMonthly = this.priceMonthly,
    currency = this.currency,
    maxHouseholds = this.maxHouseholds,
    maxMembersPerHousehold = this.maxMembersPerHousehold,
    maxProducts = this.maxProducts,
    maxInventoryItems = this.maxInventoryItems,
    maxWishlistItems = this.maxWishlistItems,
    maxFuturePurchases = this.maxFuturePurchases,
    maxRecurring = this.maxRecurring,
    maxAccounts = this.maxAccounts,
    features = this.features,
    isActive = this.isActive
)

fun SubscriptionDto.toDomain(): Subscription = Subscription(
    id = this.id,
    userId = this.userId,
    planId = this.planId,
    plan = this.plan?.toDomain() ?: throw IllegalArgumentException("Plan not found in subscription"),
    status = SubscriptionStatus.fromValue(this.status),
    currentPeriodStart = parseDateTime(this.currentPeriodStart),
    currentPeriodEnd = this.currentPeriodEnd?.let { parseDateTime(it) },
    trialEndsAt = this.trialEndsAt?.let { parseDateTime(it) }
)

private fun parseDateTime(dateString: String?): OffsetDateTime {
    if (dateString == null) return OffsetDateTime.now()
    return try {
        OffsetDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    } catch (e: Exception) {
        OffsetDateTime.now()
    }
}
