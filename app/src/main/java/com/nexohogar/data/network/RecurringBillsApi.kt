package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.CreateRecurringBillRequest
import com.nexohogar.data.remote.dto.RecurringBillDto
import com.nexohogar.data.remote.dto.ToggleActiveRequest
import com.nexohogar.data.remote.dto.UpdateLastPaidRequest
import retrofit2.Response
import retrofit2.http.*

interface RecurringBillsApi {

    @GET("rest/v1/recurring_bills")
    suspend fun getRecurringBills(
        @Query("household_id") householdIdFilter: String,
        @Query("select")       select: String = "*",
        @Query("order")        order: String = "due_day.asc"
    ): Response<List<RecurringBillDto>>

    @POST("rest/v1/recurring_bills")
    suspend fun createRecurringBill(
        @Header("Prefer") prefer: String = "return=representation",
        @Body request: CreateRecurringBillRequest
    ): Response<List<RecurringBillDto>>

    @PATCH("rest/v1/recurring_bills")
    suspend fun markAsPaid(
        @Query("id")       idFilter: String,
        @Header("Prefer")  prefer: String = "return=representation",
        @Body request: UpdateLastPaidRequest
    ): Response<List<RecurringBillDto>>

    @PATCH("rest/v1/recurring_bills")
    suspend fun toggleActive(
        @Query("id")       idFilter: String,
        @Header("Prefer")  prefer: String = "return=representation",
        @Body request: ToggleActiveRequest
    ): Response<List<RecurringBillDto>>

    @DELETE("rest/v1/recurring_bills")
    suspend fun deleteRecurringBill(
        @Query("id") idFilter: String
    ): Response<Unit>
}
