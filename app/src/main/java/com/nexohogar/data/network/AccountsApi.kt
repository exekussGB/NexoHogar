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
import retrofit2.Response
interface AccountsApi {


    /**
     * Consulta la VISTA account_balances para obtener saldos reales.
     * La vista incluye is_shared y owner_user_id desde la tabla accounts.
     * householdId debe pasarse con prefijo "eq." → "eq.{uuid}"
     */
    @GET("rest/v1/account_balances")
    suspend fun getBalances(
        @Query("household_id") householdId: String,
        @Query("select")       select: String = "account_id,name,account_type,balance_clp,is_shared,owner_user_id",
        @Query("order")        order: String  = "name.asc"
    ): List<AccountBalanceViewDto>

    /**
     * Obtiene cuentas del hogar (sin balance calculado, solo metadata).
     */
    @GET("rest/v1/accounts")
    suspend fun getAccounts(
        @Query("household_id") householdId: String,
        @Query("select")       select: String = "*",
        @Query("order")        order: String  = "name.asc",
        @Query("is_deleted") isDeleted: String = "eq.false"
    ): List<AccountDto>

    /**
     * Crea una nueva cuenta.
     * account_type debe ser UPPERCASE: ASSET, LIABILITY, INCOME, EXPENSE
     */
    @Headers("Prefer: return=representation")
    @POST("rest/v1/accounts")
    suspend fun createAccount(
        @Body request: CreateAccountRequest
    ): List<AccountResponse>

    /**
     * Elimina (soft delete) una cuenta por ID.
     */
    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/accounts")
    suspend fun deleteAccount(
        @Query("id") id: String,
        @Body body: SoftDeleteAccountRequest
    ): Response<Unit>

}
