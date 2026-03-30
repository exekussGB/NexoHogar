package com.nexohogar.data.remote.dto

/**
 * COH-07: Extraído de PersonalDashboardApi.kt al paquete dto.
 */
data class PersonalTransactionsRequest(
    val p_household_id: String,
    val p_user_id: String,
    val p_limit: Int = 5
)
