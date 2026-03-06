package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.AccountBalance

/**
 * DTO para el balance de cuenta proveniente de Supabase.
 */
data class AccountBalanceDto(
    @SerializedName("account_id")
    val accountId: String,
    @SerializedName("account_name")
    val accountName: String,
    @SerializedName("account_type")
    val accountType: String,
    @SerializedName("movement_balance")
    val movementBalance: Double
)

/**
 * Mapper para convertir DTO a modelo de Dominio.
 */
fun AccountBalanceDto.toDomain(): AccountBalance {
    return AccountBalance(
        accountId = accountId,
        accountName = accountName,
        accountType = accountType,
        movementBalance = movementBalance
    )
}

fun List<AccountBalanceDto>.toDomain(): List<AccountBalance> {
    return this.map { it.toDomain() }
}
