package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TransactionDetailDto(
    @SerializedName("id")               val id: String,
    @SerializedName("type")             val type: String,
    @SerializedName("description")      val description: String?,
    @SerializedName("transaction_date") val transactionDate: String?,
    @SerializedName("amount_clp")       val amountClp: Long?,
    @SerializedName("status")           val status: String?,
    // ✅ FIX: era "from_account_id" pero la columna real es "account_id"
    @SerializedName("account_id")       val fromAccountId: String?,
    @SerializedName("to_account_id")    val toAccountId: String?,
    @SerializedName("household_id")     val householdId: String?,
    // ✅ NUEVO: timestamp completo para mostrar hora
    @SerializedName("created_at")       val createdAt: String?,
    // ✅ NUEVO: nombre del usuario que creó (viene de v_transactions_with_user)
    @SerializedName("created_by_name")  val createdByName: String?
)
