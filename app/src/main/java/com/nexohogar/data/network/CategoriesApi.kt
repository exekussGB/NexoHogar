package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.CategoryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface Retrofit para obtener categorías desde Supabase.
 */
interface CategoriesApi {

    @GET("rest/v1/categories")
    suspend fun getCategories(
        @Query("household_id") householdFilter: String,
        @Query("select") select: String = "id,name,type,household_id",
        @Query("order") order: String = "name.asc"
    ): Response<List<CategoryResponse>>
}
