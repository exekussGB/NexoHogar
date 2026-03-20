package com.nexohogar.data.remote.dto
import com.google.gson.annotations.SerializedName
/**
 * Parámetro para el RPC create_household_with_defaults.
 * El backend crea el hogar y genera automáticamente su cuenta base.
 */


data class CreateHouseholdRequest(
    @SerializedName("p_name")
    val name: String,
    @SerializedName("description")
    val description: String? = null
)

/**
 * Respuesta del RPC create_household_with_defaults.
 * Devuelve el hogar recién creado.
 */
