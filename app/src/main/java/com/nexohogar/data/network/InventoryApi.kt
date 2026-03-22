package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.CreateInventoryMovementRequest
import com.nexohogar.data.remote.dto.CreateProductRequest
import com.nexohogar.data.remote.dto.InventoryMovementDto
import com.nexohogar.data.remote.dto.ProductDto
import retrofit2.Response
import retrofit2.http.*

interface InventoryApi {

    // ---------- inventory_items ----------
    @GET("rest/v1/inventory_items")
    suspend fun getProducts(
        @Query("household_id") householdId: String,
        @Query("order") order: String = "name.asc"
    ): Response<List<ProductDto>>

    @POST("rest/v1/inventory_items")
    @Headers("Prefer: return=representation")
    suspend fun createProduct(
        @Body request: CreateProductRequest
    ): Response<List<ProductDto>>

    // ---------- inventory_movements ----------
    @GET("rest/v1/inventory_movements")
    suspend fun getMovements(
        @Query("household_id") householdId: String,
        @Query("item_id") itemId: String? = null,
        @Query("order") order: String = "movement_date.desc"
    ): Response<List<InventoryMovementDto>>

    @POST("rest/v1/inventory_movements")
    @Headers("Prefer: return=representation")
    suspend fun addMovement(
        @Body request: CreateInventoryMovementRequest
    ): Response<List<InventoryMovementDto>>
    }