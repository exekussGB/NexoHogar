package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para la respuesta de la API de categorías.
 */
data class CategoryResponse(
    val id: String,
    val name: String,
    val type: String,
    @SerializedName("household_id")
    val householdId: String
)
