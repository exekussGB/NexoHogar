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

    @POST("rpc/rpc_get_budget_consumption")
    suspend fun getBudgetConsumption(
        @Body body: HashMap<String, Any>
    ): Response<List<BudgetConsumptionDto>>

    @Headers("Prefer: return=minimal")
    @POST("budgets")
    suspend fun createBudget(
        @Body body: HashMap<String, Any>
    ): Response<Unit>

    @Headers("Prefer: return=minimal")
    @PATCH("budgets")
    suspend fun updateBudget(
        @Query("id") id: String,
        @Body body: HashMap<String, Any>
    ): Response<Unit>

    @DELETE("budgets")
    suspend fun deleteBudget(
        @Query("id") id: String
    ): Response<Unit>

    @POST("rpc/rpc_create_budget")
    suspend fun createBudgetRpc(
        @Body body: HashMap<String, Any>
    ): Response<Unit>
}
