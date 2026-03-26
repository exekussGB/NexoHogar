package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.BudgetConsumptionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface BudgetApi {

    /**
     * Llama a rpc_get_budget_consumption para obtener presupuesto con consumo.
     * Params: p_household_id, p_year, p_month
     */
    @POST("rest/v1/rpc/rpc_get_budget_consumption")
    suspend fun getBudgetConsumption(
        @Body body: Map<String, Any>
    ): Response<List<BudgetConsumptionDto>>
}
