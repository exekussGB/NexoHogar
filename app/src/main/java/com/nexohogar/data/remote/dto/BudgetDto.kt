package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.Budget

data class BudgetDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("household_id")
    val householdId: String,

    @SerializedName("category_id")
    val categoryId: String,

    @SerializedName("categories")
    val categories: CategoryEmbedded?,

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

    @SerializedName("member_id")
    val memberId: String?,

    @SerializedName("created_by")
    val createdBy: String?,

    @SerializedName("created_at")
    val createdAt: String?,

    @SerializedName("updated_at")
    val updatedAt: String?
) {
    data class CategoryEmbedded(
        @SerializedName("name")
        val name: String
    )

    fun toDomain(): Budget {
        return Budget(
            id = id,
            householdId = householdId,
            categoryId = categoryId,
            categoryName = categories?.name ?: "",
            amountClp = amountClp,
            periodType = periodType,
            yearNum = yearNum,
            monthNum = monthNum,
            weekNum = weekNum,
            memberId = memberId,
            createdAt = createdAt
        )
    }
}