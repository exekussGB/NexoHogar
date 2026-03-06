package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.TransactionEntry

/**
 * Data Transfer Object for Transaction Entry from Supabase view.
 */
data class TransactionEntryDto(
    @SerializedName("entry_id")
    val entryId: String,
    @SerializedName("account_name")
    val accountName: String,
    @SerializedName("account_type")
    val accountType: String,
    @SerializedName("entry_type")
    val entryType: String,
    @SerializedName("amount_clp")
    val amountClp: Double
)

/**
 * Mapper extension to convert DTO to Domain model.
 */
fun TransactionEntryDto.toDomain(): TransactionEntry {
    return TransactionEntry(
        entryId = entryId,
        accountName = accountName,
        accountType = accountType,
        entryType = entryType,
        amountClp = amountClp
    )
}

fun List<TransactionEntryDto>.toDomain(): List<TransactionEntry> {
    return this.map { it.toDomain() }
}
