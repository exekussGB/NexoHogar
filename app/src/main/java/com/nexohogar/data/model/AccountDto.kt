package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.Account

data class AccountDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("account_type")
    val accountType: String?,

    @SerializedName("balance")
    val balance: Double?,

    @SerializedName("household_id")
    val householdId: String
)

fun AccountDto.toDomain(): Account {
    return Account(
        id = id,
        name = name,
        type = accountType ?: "ASSET",
        balance = (balance ?: 0.0).toLong(),   // convertir a Long
        householdId = householdId
    )
}

fun List<AccountDto>.toDomain(): List<Account> {
    return this.map { it.toDomain() }
}