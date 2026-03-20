package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName

data class HouseholdResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String? = null
)