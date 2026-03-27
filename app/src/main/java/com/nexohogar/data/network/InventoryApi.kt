package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.CreateInventoryCategoryRequest
import com.nexohogar.data.remote.dto.CreateInventoryMovementRequest
import com.nexohogar.data.remote.dto.CreateProductRequest
import com.nexohogar.data.remote.dto.InventoryCategoryDto
import com.nexohogar.data.remote.dto.InventoryMovementDto
import com.nexohogar.data.remote.dto.ProductDto
import com.nexohogar.data.remote.dto.ImportReceiptRequest
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

    @PATCH("rest/v1/inventory_items")
    @Headers("Prefer: return=representation")
    suspend fun updateProduct(
        @Query("id") id: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
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

    // ---------- inventory_categories ----------
    @GET("rest/v1/inventory_categories")
    suspend fun getCategories(
        @Query("household_id") householdId: String,
        @Query("order") order: String = "sort_order.asc,name.asc"
    ): Response<List<InventoryCategoryDto>>

    @POST("rest/v1/inventory_categories")
    @Headers("Prefer: return=representation")
    suspend fun createCategory(
        @Body request: CreateInventoryCategoryRequest
    ): Response<List<InventoryCategoryDto>>

    @DELETE("rest/v1/inventory_categories")
    suspend fun deleteCategory(
        @Query("id") id: String
    ): Response<Unit>

    @POST("rest/v1/rpc/rpc_import_receipt")
    suspend fun importReceipt(
        @Body request: ImportReceiptRequest
    ): Response<Map<String, Any>>
}
