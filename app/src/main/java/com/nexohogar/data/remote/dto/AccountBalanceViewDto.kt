package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.AccountBalance

/**
 * DTO para la VISTA account_balances de Supabase.
 * balance_clp = initial_balance_clp + movimientos de transaction_entries (double-entry).
 * La vista ahora incluye is_shared, owner_user_id, is_savings e icon.
 *
 * Select: account_id,name,account_type,balance_clp,is_shared,owner_user_id,is_savings,icon
 */
data class AccountBalanceViewDto(
    @SerializedName("account_id")    val accountId:    String,
    @SerializedName("name")          val accountName:  String,
    @SerializedName("account_type")  val accountType:  String,
    @SerializedName("balance_clp")   val balanceClp:   Double,
    @SerializedName("is_shared")     val isShared:     Boolean = true,
    @SerializedName("owner_user_id") val ownerUserId:  String? = null,
    @SerializedName("is_savings")    val isSavings:    Boolean = false,
    @SerializedName("is_liability")  val isLiability:  Boolean = false,
    @SerializedName("icon")          val icon:         String? = null,
    @SerializedName("credit_limit")  val creditLimit:  Double? = null
)

fun AccountBalanceViewDto.toDomain() = AccountBalance(
    accountId       = accountId,
    accountName     = accountName,
    accountType     = accountType,
    movementBalance = balanceClp.toLong(),
    isShared        = isShared,
    ownerUserId     = ownerUserId,
    isSavings       = isSavings,
    isLiability     = isLiability,
    icon            = icon,
    creditLimit     = creditLimit?.toLong()
)

fun List<AccountBalanceViewDto>.toDomain() = map { it.toDomain() }
