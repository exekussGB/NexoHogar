package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.Account

data class AccountDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("account_type")
    val accountType: String?,

    @SerializedName("account_subtype")
    val accountSubtype: String?,

    @SerializedName("balance")
    val balance: Double?,

    @SerializedName("household_id")
    val householdId: String,

    @SerializedName("is_shared")
    val isShared: Boolean?,

    @SerializedName("owner_user_id")
    val ownerUserId: String?,

    @SerializedName("is_system")
    val isSystem: Boolean?,

    @SerializedName("is_savings")
    val isSavings: Boolean? = false,    // 🆕 Feature 2

    @SerializedName("is_liability")
    val isLiability: Boolean? = false,  // 🆕 Feature 3: Deudas

    @SerializedName("icon")
    val icon: String? = null,           // 🆕 Custom icon

    @SerializedName("credit_limit")
    val creditLimit: Double? = null     // 🆕 Feature 4: Cupo
)

fun AccountDto.toDomain(): Account {
    return Account(
        id = id,
        name = name,
        type = accountType ?: "ASSET",
        subtype = accountSubtype ?: "other",
        balance = (balance ?: 0.0).toLong(),
        householdId = householdId,
        isShared = isShared ?: true,
        ownerUserId = ownerUserId,
        isSavings = isSavings ?: false,    // 🆕 Feature 2
        isLiability = isLiability ?: false, // 🆕 Feature 3
        icon = icon,                       // 🆕 Custom icon
        creditLimit = creditLimit?.toLong()
    )
}

fun List<AccountDto>.toDomain(): List<Account> {
    return this.map { it.toDomain() }
}
