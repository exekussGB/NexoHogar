package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.AccountBalance

/**
 * DTO para la VISTA account_balances de Supabase.
 * balance_clp = initial_balance_clp + movimientos de transaction_entries (double-entry).
 * La vista ahora incluye is_shared y owner_user_id.
 */
data class AccountBalanceViewDto(
    @SerializedName("account_id")    val accountId:    String,
    @SerializedName("name")          val accountName:  String,
    @SerializedName("account_type")  val accountType:  String,
    @SerializedName("balance_clp")   val balanceClp:   Double,
    @SerializedName("is_shared")     val isShared:     Boolean = true,
    @SerializedName("owner_user_id") val ownerUserId:  String? = null
)

fun AccountBalanceViewDto.toDomain() = AccountBalance(
    accountId       = accountId,
    accountName     = accountName,
    accountType     = accountType,
    movementBalance = balanceClp.toLong(),
    isShared        = isShared,
    ownerUserId     = ownerUserId
)

fun List<AccountBalanceViewDto>.toDomain() = map { it.toDomain() }
