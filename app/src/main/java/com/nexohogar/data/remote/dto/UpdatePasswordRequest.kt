package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UpdatePasswordRequest(
    @SerializedName("password")
    val password: String
)
