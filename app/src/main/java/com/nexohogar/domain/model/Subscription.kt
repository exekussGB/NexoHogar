package com.nexohogar.domain.model

import java.time.OffsetDateTime

data class Subscription(
    val id: String,
    val userId: String,
    val planId: String,
    val plan: Plan,
    val status: SubscriptionStatus,
    val currentPeriodStart: OffsetDateTime,
    val currentPeriodEnd: OffsetDateTime? = null,
    val trialEndsAt: OffsetDateTime? = null
)

enum class SubscriptionStatus(val value: String) {
    ACTIVE("active"),
    TRIALING("trialing"),
    CANCELED("canceled"),
    EXPIRED("expired"),
    PAST_DUE("past_due");

    companion object {
        fun fromValue(value: String?): SubscriptionStatus {
            return values().find { it.value == value } ?: ACTIVE
        }
    }
}
