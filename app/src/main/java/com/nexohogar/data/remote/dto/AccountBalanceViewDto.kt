package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.AccountBalance

/**
 * DTO para la VISTA account_balances de Supabase.
 * balance_clp = initial_balance_clp + movimientos de transaction_entries (double-entry).
 */
data class AccountBalanceViewDto(
    @SerializedName("account_id")   val accountId:   String,
    @SerializedName("name")         val accountName: String,
    @SerializedName("account_type") val accountType: String,
    @SerializedName("balance_clp")  val balanceClp:  Double
)

fun AccountBalanceViewDto.toDomain() = AccountBalance(
    accountId       = accountId,
    accountName     = accountName,
    accountType     = accountType,
    movementBalance = balanceClp.toLong()
)

fun List<AccountBalanceViewDto>.toDomain() = map { it.toDomain() }
