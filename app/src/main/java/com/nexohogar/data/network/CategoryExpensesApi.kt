package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.CategoryExpenseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CategoryExpensesApi {
    @POST("rest/v1/rpc/rpc_category_expenses")
    suspend fun getCategoryExpenses(
        @Body body: Map<String, String>
    ): Response<List<CategoryExpenseDto>>
}
