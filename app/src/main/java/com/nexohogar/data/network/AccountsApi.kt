package com.nexohogar.data.network

import com.nexohogar.data.model.AccountDto
import com.nexohogar.data.model.CreateAccountRequest
import com.nexohogar.data.remote.dto.AccountResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface AccountsApi {
    @GET("rest/v1/accounts")
    suspend fun getAccounts(
        @Query("household_id") householdId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "name.asc"
    ): List<AccountDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/accounts")
    suspend fun createAccount(
        @Body request: CreateAccountRequest
    ): List<AccountResponse>
}
