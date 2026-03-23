package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName

data class CreateAccountRequest(
    @SerializedName("name") val name: String,
    @SerializedName("account_type") val accountType: String,   // ASSET o LIABILITY (MAYÚSCULAS)
    @SerializedName("household_id") val householdId: String,
    @SerializedName("currency_code") val currencyCode: String = "CLP"  // Requerido, default CLP
)
