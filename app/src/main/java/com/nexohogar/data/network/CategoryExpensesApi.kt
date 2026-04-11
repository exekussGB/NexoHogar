package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.CategoryExpenseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CategoryExpensesApi {

    @POST("rpc/rpc_category_expenses_v2")
    suspend fun getCategoryExpenses(
        @Body body: HashMap<String, Any>
    ): Response<List<CategoryExpenseDto>>
}
