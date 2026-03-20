package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateAccountRequest(
    @SerializedName("name")            val name: String,
    @SerializedName("account_type")    val accountType: String,
    @SerializedName("account_subtype") val accountSubtype: String = "other",
    @SerializedName("household_id")    val householdId: String,
    @SerializedName("is_active")       val isActive: Boolean = true
)