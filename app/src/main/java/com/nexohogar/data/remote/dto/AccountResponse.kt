package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AccountResponse(
    @SerializedName("id") val id: String,
    @SerializedName("household_id") val householdId: String,
    @SerializedName("name") val name: String,
    @SerializedName("account_type") val accountType: String?,
    @SerializedName("account_subtype") val accountSubtype: String?,
    @SerializedName("balance") val balance: Double?,
    @SerializedName("is_shared") val isShared: Boolean?,
    @SerializedName("owner_user_id") val ownerUserId: String?,
    @SerializedName("created_by") val createdBy: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("is_savings") val isSavings: Boolean? = false,
    @SerializedName("icon") val icon: String? = null           // 🆕 Custom icon
)
