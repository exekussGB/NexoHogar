package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.CreateFuturePurchaseRequest
import com.nexohogar.data.remote.dto.FuturePurchaseDto
import com.nexohogar.data.remote.dto.UpdateFuturePurchaseRequest
import retrofit2.Response
import retrofit2.http.*

interface FuturePurchasesApi {

    @GET("rest/v1/future_purchases")
    suspend fun getFuturePurchases(
        @Query("household_id") householdIdFilter: String,
        @Query("select")       select: String = "*",
        @Query("order")        order: String = "priority.asc,created_at.desc"
    ): Response<List<FuturePurchaseDto>>

    @POST("rest/v1/future_purchases")
    suspend fun createFuturePurchase(
        @Header("Prefer") prefer: String = "return=representation",
        @Body request: CreateFuturePurchaseRequest
    ): Response<List<FuturePurchaseDto>>

    @PATCH("rest/v1/future_purchases")
    suspend fun updateFuturePurchase(
        @Query("id")      idFilter: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body request: UpdateFuturePurchaseRequest
    ): Response<List<FuturePurchaseDto>>

    @DELETE("rest/v1/future_purchases")
    suspend fun deleteFuturePurchase(
        @Query("id") idFilter: String
    ): Response<Unit>
}
