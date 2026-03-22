package com.nexohogar.data.network

import com.nexohogar.data.model.AccountDto
import com.nexohogar.data.remote.dto.AccountResponse
import com.nexohogar.data.remote.dto.CreateAccountRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * API de cuentas para Supabase.
 * AuthInterceptor inyecta el token automáticamente en todas las requests.
 *
 * CORRECCIÓN v5: eliminada @Query("is_active") — esa columna no existe
 * en la tabla accounts del usuario, causando HTTP 400 en todas las llamadas.
 */
interface AccountsApi {

    /**
     * Obtiene las cuentas de un hogar.
     * IMPORTANTE: householdId debe pasarse con prefijo "eq." → "eq.{uuid}"
     */
    @GET("rest/v1/accounts")
    suspend fun getAccounts(
        @Query("household_id") householdId: String,
        @Query("select")       select: String = "*",
        @Query("order")        order: String = "name.asc"
    ): List<AccountDto>

    /**
     * Crea una nueva cuenta.
     * account_type debe ser LOWERCASE: "asset", "liability", "income", "expense"
     */
    @Headers("Prefer: return=representation")
    @POST("rest/v1/accounts")
    suspend fun createAccount(
        @Body request: CreateAccountRequest
    ): List<AccountResponse>
}
