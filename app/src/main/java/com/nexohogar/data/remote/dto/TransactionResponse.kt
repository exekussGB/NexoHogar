package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Datos de cuenta embebidos via PostgREST join (select=*,accounts(name,type))
 */
data class AccountInfo(
    @SerializedName("name") val name: String?,
    @SerializedName("type") val type: String?
)

/**
 * DTO unificado para respuestas de transacciones desde Supabase.
 */
data class TransactionResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("amount_clp")
    val amountClp: Long,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("account_id")
    val accountId: String,

    @SerializedName("household_id")
    val householdId: String,

    @SerializedName("type")
    val type: String,

    // Join con tabla accounts — null si la query no incluye el join
    @SerializedName("accounts")
    val accounts: AccountInfo? = null,

    @SerializedName("created_by")
    val createdBy: String? = null,

    @SerializedName("created_by_name")
    val createdByName: String? = null,

    @SerializedName("to_account_id")
    val toAccountId: String? = null,

    @SerializedName("transaction_date")
    val transactionDate: String? = null,

    @SerializedName("category_id")
    val categoryId: String? = null
)
