package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RemoveMemberResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("removed_user_id") val removedUserId: String?
)