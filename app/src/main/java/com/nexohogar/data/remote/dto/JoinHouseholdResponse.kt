package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class JoinHouseholdResponse(
    @SerializedName("household_id") val householdId: String?,
    @SerializedName("household_name") val householdName: String?,
    @SerializedName("message") val message: String?
)