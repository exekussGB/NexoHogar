package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.CategoryResponse
import com.nexohogar.data.remote.dto.CreateCategoryRequest
import retrofit2.Response
import retrofit2.http.*

interface CategoriesApi {

    @GET("categories")
    suspend fun getCategories(
        @Query("household_id") householdFilter: String,
        @Query("select") select: String = "id,name,type,household_id,icon",
        @Query("order") order: String = "name.asc"
    ): Response<List<CategoryResponse>>

    @POST("categories")
    suspend fun createCategory(
        @Header("Prefer") prefer: String = "return=representation",
        @Body request: CreateCategoryRequest
    ): Response<List<CategoryResponse>>
}
