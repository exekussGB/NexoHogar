package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateInventoryCategoryRequest(
    @SerializedName("household_id") val householdId: String,
    @SerializedName("name")         val name: String,
    @SerializedName("icon")         val icon: String? = null
)
