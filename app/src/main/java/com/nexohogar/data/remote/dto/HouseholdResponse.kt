package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HouseholdResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("gradient_index")
    val gradientIndex: Int? = null
)
