package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class JoinHouseholdRequest(
    @SerializedName("p_invite_code")
    val inviteCode: String
)
