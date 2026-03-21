package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TransactionDetailDto(
    @SerializedName("id")               val id: String,
    @SerializedName("type")             val type: String,
    @SerializedName("description")      val description: String?,
    @SerializedName("transaction_date") val transactionDate: String?,
    @SerializedName("amount_clp")       val amountClp: Long?,
    @SerializedName("status")           val status: String?,
    @SerializedName("from_account_id")  val fromAccountId: String?,
    @SerializedName("to_account_id")    val toAccountId: String?,
    @SerializedName("household_id")     val householdId: String?
)