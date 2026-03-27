package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ImportReceiptRequest(
    @SerializedName("p_household_id")
    val householdId: String,

    @SerializedName("p_user_id")
    val userId: String,

    @SerializedName("p_account_id")
    val accountId: String,

    @SerializedName("p_category_id")
    val categoryId: String? = null,

    @SerializedName("p_store")
    val store: String? = null,

    @SerializedName("p_receipt_date")
    val receiptDate: String,

    @SerializedName("p_items")
    val items: List<ImportReceiptItemDto>
)