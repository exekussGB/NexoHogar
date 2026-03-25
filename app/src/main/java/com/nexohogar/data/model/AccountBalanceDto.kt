package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.AccountBalance

/**
 * DTO para saldos calculados desde el RPC get_calculated_balances.
 */
data class AccountBalanceDto(
    @SerializedName("account_id")
    val accountId: String,
    @SerializedName("account_name")
    val accountName: String,
    @SerializedName("account_type")
    val accountType: String,
    @SerializedName("initial_balance")
    val initialBalance: Double? = null,
    @SerializedName("calculated_balance")
    val calculatedBalance: Double? = null,
    // Mantener compatibilidad con RPC antiguo get_account_balances
    @SerializedName("movement_balance")
    val movementBalance: Double? = null
)

/**
 * Mapper: usa calculated_balance si existe, sino movement_balance, sino 0.
 */
fun AccountBalanceDto.toDomain(): AccountBalance {
    val balance = calculatedBalance ?: movementBalance ?: 0.0
    return AccountBalance(
        accountId       = accountId,
        accountName     = accountName,
        accountType     = accountType,
        movementBalance = balance.toLong()
    )
}

fun List<AccountBalanceDto>.toDomain(): List<AccountBalance> {
    return this.map { it.toDomain() }
}