package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.InventoryMovement

data class InventoryMovementDto(
    @SerializedName("id")             val id: String,
    @SerializedName("household_id")   val householdId: String,
    @SerializedName("item_id")        val itemId: String,
    @SerializedName("transaction_id") val transactionId: String? = null,
    @SerializedName("movement_type")  val movementType: String,   // "in" o "out"
    @SerializedName("quantity")       val quantity: Double,
    @SerializedName("movement_date")  val movementDate: String,
    @SerializedName("price_per_unit") val pricePerUnit: Double? = null,
    @SerializedName("price_total")    val priceTotal: Double? = null,
    @SerializedName("brand")          val brand: String? = null,
    @SerializedName("store")          val store: String? = null,
    @SerializedName("notes")          val notes: String? = null,
    @SerializedName("created_at")     val createdAt: String? = null
) {
    fun toDomain() = InventoryMovement(
        id = id,
        householdId = householdId,
        itemId = itemId,
        transactionId = transactionId,
        movementType = movementType,
        quantity = quantity,
        movementDate = movementDate,
        pricePerUnit = pricePerUnit,
        priceTotal = priceTotal,
        brand = brand,
        store = store,
        notes = notes,
        createdAt = createdAt
    )
}
