package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateBudgetRequest(
    @SerializedName("household_id")
    val householdId: String,

    @SerializedName("category_id")
    val categoryId: String,

    @SerializedName("amount_clp")
    val amountClp: Double,

    @SerializedName("period_type")
    val periodType: String,

    @SerializedName("year_num")
    val yearNum: Int,

    @SerializedName("month_num")
    val monthNum: Int?,

    @SerializedName("week_num")
    val weekNum: Int?,

    @SerializedName("created_by")
    val createdBy: String?,

    @SerializedName("member_id")
    val memberId: String?
)