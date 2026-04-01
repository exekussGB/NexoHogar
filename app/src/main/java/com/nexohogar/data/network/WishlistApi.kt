package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.CreateWishlistItemRequest
import com.nexohogar.data.remote.dto.UpdateWishlistItemRequest
import com.nexohogar.data.remote.dto.WishlistItemDto
import retrofit2.Response
import retrofit2.http.*

interface WishlistApi {

    @GET("rest/v1/wishlist_items")
    suspend fun getWishlistItems(
        @Query("household_id") householdIdFilter: String,
        @Query("select")       select: String = "*",
        @Query("order")        order: String = "priority.asc,created_at.desc"
    ): Response<List<WishlistItemDto>>

    @POST("rest/v1/wishlist_items")
    suspend fun createWishlistItem(
        @Header("Prefer") prefer: String = "return=representation",
        @Body request: CreateWishlistItemRequest
    ): Response<List<WishlistItemDto>>

    @PATCH("rest/v1/wishlist_items")
    suspend fun updateWishlistItem(
        @Query("id")      idFilter: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body request: UpdateWishlistItemRequest
    ): Response<List<WishlistItemDto>>

    @DELETE("rest/v1/wishlist_items")
    suspend fun deleteWishlistItem(
        @Query("id") idFilter: String
    ): Response<Unit>
}
