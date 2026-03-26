package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.BudgetConsumptionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface BudgetApi {

    /**
     * Llama a rpc_get_budget_consumption para obtener presupuesto con consumo.
     * Params: p_household_id, p_year, p_month
     */
    @POST("rest/v1/rpc/rpc_get_budget_consumption")
    suspend fun getBudgetConsumption(
        @Body body: HashMap<String, Any>
    ): Response<List<BudgetConsumptionDto>>

    /** Crea un presupuesto en la tabla budgets. */
    @Headers("Prefer: return=minimal")
    @POST("rest/v1/budgets")
    suspend fun createBudget(
        @Body body: HashMap<String, Any>
    ): Response<Unit>

    /** Actualiza el monto de un presupuesto. */
    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/budgets")
    suspend fun updateBudget(
        @Query("id") id: String,
        @Body body: HashMap<String, Any>
    ): Response<Unit>

    /** Elimina un presupuesto por ID. */
    @DELETE("rest/v1/budgets")
    suspend fun deleteBudget(
        @Query("id") id: String
    ): Response<Unit>
}
