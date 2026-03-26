package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.Account

data class AccountDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("account_type") val accountType: String?,
    @SerializedName("balance") val balance: Double?,
    @SerializedName("household_id") val householdId: String,
    @SerializedName("is_shared") val isShared: Boolean? = true,
    @SerializedName("owner_user_id") val ownerUserId: String? = null
)

fun AccountDto.toDomain(): Account {
    return Account(
        id = id,
        name = name,
        type = accountType ?: "ASSET",
        balance = (balance ?: 0.0).toLong(),
        householdId = householdId,
        isShared = isShared ?: true,
        ownerUserId = ownerUserId
    )
}

fun List<AccountDto>.toDomain(): List<Account> = map { it.toDomain() }
