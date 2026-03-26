package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para las transacciones personales devueltas por rpc_personal_recent_transactions.
 */
data class PersonalTransactionDto(
    @SerializedName("id")          val id: String,
    @SerializedName("account_id")  val accountId: String,
    @SerializedName("type")        val type: String,
    @SerializedName("description") val description: String?,
    @SerializedName("amount_clp")  val amountClp: Long,
    @SerializedName("created_at")  val createdAt: String
)
