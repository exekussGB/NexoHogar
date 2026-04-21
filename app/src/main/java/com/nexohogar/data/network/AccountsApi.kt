package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.AccountDto
import com.nexohogar.data.remote.dto.CreateAccountRequest
import com.nexohogar.data.remote.dto.AccountBalanceViewDto
import com.nexohogar.data.remote.dto.AccountResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.PATCH
import com.nexohogar.data.remote.dto.SoftDeleteAccountRequest
import com.nexohogar.data.remote.dto.UpdateAccountRequest
import retrofit2.Response

interface AccountsApi {

    @GET("account_balances")
    suspend fun getBalances(
        @Query("household_id") householdId: String,
        @Query("select")       select: String = "account_id,name,account_type,account_subtype,balance_clp,is_shared,owner_user_id,is_savings,is_liability,icon,credit_limit",
        @Query("order")        order: String  = "name.asc"
    ): List<AccountBalanceViewDto>

    @GET("accounts")
    suspend fun getAccounts(
        @Query("household_id") householdId: String,
        @Query("select")       select: String = "*",
        @Query("order")        order: String  = "name.asc",
        @Query("is_deleted")   isDeleted: String = "eq.false"
    ): List<AccountDto>

    @Headers("Prefer: return=representation")
    @POST("accounts")
    suspend fun createAccount(
        @Body request: CreateAccountRequest
    ): List<AccountResponse>

    @Headers("Prefer: return=minimal")
    @PATCH("accounts")
    suspend fun deleteAccount(
        @Query("id") id: String,
        @Body body: SoftDeleteAccountRequest
    ): Response<Unit>

    @Headers("Prefer: return=minimal")
    @PATCH("accounts")
    suspend fun updateAccount(
        @Query("id") id: String,
        @Body body: UpdateAccountRequest
    ): Response<Unit>
}
