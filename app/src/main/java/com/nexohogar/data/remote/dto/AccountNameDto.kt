package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AccountNameDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String
)