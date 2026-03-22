package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class InviteCodeRequest(
    @SerializedName("p_household_id")
    val householdId: String
)
