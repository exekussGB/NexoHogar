package com.nexohogar.core.di

import android.annotation.SuppressLint
import android.content.Context
import com.nexohogar.core.network.SupabaseConfig
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.*
import com.nexohogar.data.repository.*
import com.nexohogar.domain.repository.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Service Locator manual.
 * Centraliza la creación de dependencias y expone solo las interfaces de Dominio.
 */
@SuppressLint("StaticFieldLeak")
object ServiceLocator {

    private var databaseContext: Context? = null

    fun init(context: Context) {
        if (databaseContext == null) {
            databaseContext = context.applicationContext
        }
    }

    private val context: Context
        get() = databaseContext ?: throw IllegalStateException("ServiceLocator must be initialized with context")

    // --- Core & Local ---
    
    val sessionManager: SessionManager by lazy {
        SessionManager(context)
    }

    val tenantContext: TenantContext by lazy {
        TenantContext(sessionManager)
    }

    // --- Network ---

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()
            .addHeader("apikey", SupabaseConfig.API_KEY)
            .addHeader("Content-Type", "application/json")

        sessionManager.fetchAuthToken()?.let { token ->
            builder.addHeader("Authorization", "Bearer $token")
        }

        chain.proceed(builder.build())
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://REMOVED.supabase.co/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val dashboardApi: DashboardApi by lazy {
        retrofit.create(DashboardApi::class.java)
    }

    val accountsApi: AccountsApi by lazy {
        retrofit.create(AccountsApi::class.java)
    }

    val transactionsApi: TransactionsApi by lazy {
        retrofit.create(TransactionsApi::class.java)
    }

    val transactionDetailApi: TransactionDetailApi by lazy {
        retrofit.create(TransactionDetailApi::class.java)
    }

    // --- Repositories (Exponiendo interfaces de Dominio) ---
    
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(authApi, sessionManager)
    }

    val householdRepository: HouseholdRepository by lazy {
        HouseholdRepositoryImpl(authApi, tenantContext)
    }

    val dashboardRepository: DashboardRepository by lazy {
        DashboardRepositoryImpl(dashboardApi)
    }

    val accountsRepository: AccountsRepository by lazy {
        AccountsRepositoryImpl(accountsApi)
    }

    val transactionsRepository: TransactionsRepository by lazy {
        TransactionsRepositoryImpl(transactionsApi)
    }

    val transactionDetailRepository: TransactionDetailRepository by lazy {
        TransactionDetailRepositoryImpl(transactionDetailApi)
    }
}
