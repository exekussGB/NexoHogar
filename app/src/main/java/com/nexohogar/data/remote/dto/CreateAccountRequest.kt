package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateAccountRequest(
    @SerializedName("name") val name: String,
    @SerializedName("account_type") val accountType: String,
    @SerializedName("household_id") val householdId: String,
    @SerializedName("currency_code") val currencyCode: String = "CLP",
    @SerializedName("account_subtype") val accountSubtype: String? = null,
    @SerializedName("is_shared") val isShared: Boolean = true,
    @SerializedName("owner_user_id") val ownerUserId: String? = null,
    @SerializedName("initial_balance_clp") val initialBalanceCLP: Double? = null,
    @SerializedName("is_savings") val isSavings: Boolean = false    // 🆕 Feature 2
)
