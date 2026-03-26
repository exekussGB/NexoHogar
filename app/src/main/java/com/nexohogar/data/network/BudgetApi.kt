package com.nexohogar.data.network

import com.nexohogar.data.model.BudgetDto
import com.nexohogar.data.model.CreateBudgetRequest
import com.nexohogar.domain.model.BudgetConsumption
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface BudgetApi {

    @GET("rest/v1/budgets")
    suspend fun getBudgets(
        @Query("household_id") householdId: String,
        @Query("year_num") yearNum: String,
        @Query("month_num") monthNum: String,
        @Query("select") select: String = "*,categories(name)",
        @Query("order") order: String = "created_at.asc"
    ): Response<List<BudgetDto>>

    @POST("rest/v1/budgets")
    suspend fun createBudget(
        @Body request: CreateBudgetRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<BudgetDto>>

    @PATCH("rest/v1/budgets")
    suspend fun updateBudget(
        @Query("id") id: String,
        @Body request: BudgetDto,
        @Header("Prefer") prefer: String = "return=minimal"
    ): Response<Unit>

    @DELETE("rest/v1/budgets")
    suspend fun deleteBudget(
        @Query("id") id: String
    ): Response<Unit>

    @POST("rest/v1/rpc/rpc_get_budget_consumption")
    suspend fun getBudgetConsumption(
        @Body params: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<BudgetConsumption>>
}