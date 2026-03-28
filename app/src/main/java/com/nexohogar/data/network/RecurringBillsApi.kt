package com.nexohogar.data.network

import com.google.gson.JsonObject
import com.nexohogar.data.remote.dto.CreateRecurringBillRequest
import com.nexohogar.data.remote.dto.PayRecurringBillRequest
import com.nexohogar.data.remote.dto.RecurringBillDto
import com.nexohogar.data.remote.dto.RecurringBillPaymentDto
import com.nexohogar.data.remote.dto.RecurringBillWithStatusDto
import com.nexohogar.data.remote.dto.RecurringSummaryDto
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

    // ── NUEVOS: endpoints RPC con integración contable ──────────────────

    @POST("rest/v1/rpc/rpc_pay_recurring_bill")
    suspend fun payRecurringBill(
        @Body request: PayRecurringBillRequest
    ): Response<JsonObject>

    @POST("rest/v1/rpc/rpc_recurring_summary")
    suspend fun getRecurringSummary(
        @Body request: Map<String, String>
    ): Response<RecurringSummaryDto>

    @POST("rest/v1/rpc/rpc_recurring_bills_with_status")
    suspend fun getRecurringBillsWithStatus(
        @Body request: Map<String, String>
    ): Response<List<RecurringBillWithStatusDto>>

    @POST("rest/v1/rpc/rpc_recurring_bill_history")
    suspend fun getRecurringBillHistory(
        @Body request: Map<String, String>
    ): Response<List<RecurringBillPaymentDto>>
}
