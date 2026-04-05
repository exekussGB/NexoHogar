package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para editar una cuenta existente (PATCH /rest/v1/accounts).
 * Solo permite cambiar: nombre, is_savings, is_shared.
 */
data class UpdateAccountRequest(
    @SerializedName("name")       val name: String? = null,
    @SerializedName("is_savings") val isSavings: Boolean? = null,
    @SerializedName("is_shared")  val isShared: Boolean? = null
)
