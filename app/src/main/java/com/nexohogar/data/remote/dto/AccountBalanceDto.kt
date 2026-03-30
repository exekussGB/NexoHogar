package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.AccountBalance

data class AccountBalanceDto(
    @SerializedName("account_id") val accountId: String,
    @SerializedName("account_name") val accountName: String,
    @SerializedName("account_type") val accountType: String,
    @SerializedName("movement_balance") val movementBalance: Double,
    @SerializedName("is_shared") val isShared: Boolean? = true
)

fun AccountBalanceDto.toDomain(): AccountBalance {
    return AccountBalance(
        accountId = accountId,
        accountName = accountName,
        accountType = accountType,
        movementBalance = movementBalance.toLong(),
        isShared = isShared ?: true
    )
}

fun List<AccountBalanceDto>.toDomain(): List<AccountBalance> = map { it.toDomain() }
