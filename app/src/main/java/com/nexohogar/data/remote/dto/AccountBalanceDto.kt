package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.AccountBalance

/**
 * DTO para la vista de saldos por cuenta (v_account_balances o similar).
 */
data class AccountBalanceDto(
    @SerializedName("account_id") val accountId: String,
    @SerializedName("account_name") val accountName: String,
    @SerializedName("account_type") val accountType: String,
    @SerializedName("balance_clp") val balanceClp: Double, // 👈 Usar el que usa la API real
    @SerializedName("is_shared") val isShared: Boolean? = true,
    @SerializedName("is_savings") val isSavings: Boolean? = false,
    @SerializedName("is_liability") val isLiability: Boolean? = false,
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("credit_limit") val creditLimit: Double? = null
)

fun AccountBalanceDto.toDomain(): AccountBalance {
    return AccountBalance(
        accountId = accountId,
        accountName = accountName,
        accountType = accountType,
        movementBalance = balanceClp.toLong(),
        isShared = isShared ?: true,
        isSavings = isSavings ?: false,
        isLiability = isLiability ?: false,
        icon = icon,
        creditLimit = creditLimit?.toLong()
    )
}

fun List<AccountBalanceDto>.toDomain(): List<AccountBalance> = map { it.toDomain() }
