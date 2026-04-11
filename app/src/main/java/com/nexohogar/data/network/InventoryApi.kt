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

    @GET("inventory_items")
    suspend fun getProducts(
        @Query("household_id") householdId: String,
        @Query("order") order: String = "name.asc"
    ): Response<List<ProductDto>>

    @POST("inventory_items")
    @Headers("Prefer: return=representation")
    suspend fun createProduct(
        @Body request: CreateProductRequest
    ): Response<List<ProductDto>>

    @PATCH("inventory_items")
    @Headers("Prefer: return=representation")
    suspend fun updateProduct(
        @Query("id") id: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Response<List<ProductDto>>

    @GET("inventory_movements")
    suspend fun getMovements(
        @Query("household_id") householdId: String,
        @Query("item_id") itemId: String? = null,
        @Query("order") order: String = "movement_date.desc"
    ): Response<List<InventoryMovementDto>>

    @POST("inventory_movements")
    @Headers("Prefer: return=representation")
    suspend fun addMovement(
        @Body request: CreateInventoryMovementRequest
    ): Response<List<InventoryMovementDto>>

    @GET("inventory_categories")
    suspend fun getCategories(
        @Query("household_id") householdId: String,
        @Query("order") order: String = "sort_order.asc,name.asc"
    ): Response<List<InventoryCategoryDto>>

    @POST("inventory_categories")
    @Headers("Prefer: return=representation")
    suspend fun createCategory(
        @Body request: CreateInventoryCategoryRequest
    ): Response<List<InventoryCategoryDto>>

    @DELETE("inventory_categories")
    suspend fun deleteCategory(
        @Query("id") id: String
    ): Response<Unit>

    @POST("rpc/rpc_import_receipt")
    suspend fun importReceipt(
        @Body request: ImportReceiptRequest
    ): Response<Map<String, Any>>
}
