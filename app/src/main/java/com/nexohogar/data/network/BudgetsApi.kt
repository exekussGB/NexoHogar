package com.nexohogar.data.network

import com.nexohogar.data.model.BudgetDto
import com.nexohogar.data.model.BudgetSpendingDto
import com.nexohogar.data.model.CreateBudgetRequest
import com.nexohogar.data.model.ExpenseByCategoryDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface BudgetsApi {

    @GET("rest/v1/budgets")
    suspend fun getBudgets(
        @Query("household_id") householdId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): List<BudgetDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/budgets")
    suspend fun createBudget(
        @Body request: CreateBudgetRequest
    ): List<BudgetDto>

    @DELETE("rest/v1/budgets")
    suspend fun deleteBudget(
        @Query("id") budgetId: String
    ): Response<Unit>

    @PATCH("rest/v1/budgets")
    suspend fun updateBudget(
        @Query("id") budgetId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @POST("rest/v1/rpc/get_budget_spending")
    suspend fun getBudgetSpending(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): List<BudgetSpendingDto>

    @POST("rest/v1/rpc/get_expenses_by_category")
    suspend fun getExpensesByCategory(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<ExpenseByCategoryDto>
}
