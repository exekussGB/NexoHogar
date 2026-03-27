package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName

data class UpdatePasswordRequest(
    @SerializedName("password")
    val password: String
)
