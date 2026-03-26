package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName

data class BudgetDto(
    @SerializedName("id") val id: String,
    @SerializedName("household_id") val householdId: String,
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("amount_clp") val amountClp: Long,
    @SerializedName("period") val period: String?,
    @SerializedName("is_active") val isActive: Boolean?,
    @SerializedName("created_by") val createdBy: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class CreateBudgetRequest(
    @SerializedName("household_id") val householdId: String,
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("amount_clp") val amountClp: Long,
    @SerializedName("period") val period: String = "monthly",
    @SerializedName("created_by") val createdBy: String? = null
)

data class BudgetSpendingDto(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("spent") val spent: Long
)
