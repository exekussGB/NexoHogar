package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateCategoryRequest(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("household_id") val householdId: String,
    @SerializedName("icon") val icon: String? = null
)