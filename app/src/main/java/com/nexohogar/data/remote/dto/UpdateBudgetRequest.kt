package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UpdateBudgetRequest(
    @SerializedName("amount_clp")
    val amountClp: Double,

    @SerializedName("updated_at")
    val updatedAt: String
)