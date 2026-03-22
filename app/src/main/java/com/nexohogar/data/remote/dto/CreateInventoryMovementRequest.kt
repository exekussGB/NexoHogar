package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateInventoryMovementRequest(
    @SerializedName("household_id")   val householdId: String,
    @SerializedName("item_id")        val itemId: String,
    @SerializedName("movement_type")  val movementType: String,  // "in" o "out"
    @SerializedName("quantity")       val quantity: Double,
    @SerializedName("movement_date")  val movementDate: String,  // "yyyy-MM-dd"
    @SerializedName("price_per_unit") val pricePerUnit: Double? = null,
    @SerializedName("price_total")    val priceTotal: Double? = null,
    @SerializedName("brand")          val brand: String? = null,
    @SerializedName("store")          val store: String? = null,
    @SerializedName("notes")          val notes: String? = null
)
