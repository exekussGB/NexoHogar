package com.nexohogar.core.di

import android.annotation.SuppressLint
import android.content.Context
import com.nexohogar.core.network.AuthInterceptor
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.*
import com.nexohogar.data.repository.*
import com.nexohogar.domain.repository.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

    // ── Core & Local ──────────────────────────────────────────────────────────

    val sessionManager: SessionManager by lazy {
        SessionManager(context)
    }

    val tenantContext: TenantContext by lazy {
        TenantContext(sessionManager)
    }

    // ── Network ───────────────────────────────────────────────────────────────

    private val authInterceptor: AuthInterceptor by lazy {
        AuthInterceptor(sessionManager)
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

    // ── API interfaces ────────────────────────────────────────────────────────

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

    val categoriesApi: CategoriesApi by lazy {
        retrofit.create(CategoriesApi::class.java)
    }

    val recurringBillsApi: RecurringBillsApi by lazy {
        retrofit.create(RecurringBillsApi::class.java)
    }

    val inventoryApi: InventoryApi by lazy {
        retrofit.create(InventoryApi::class.java)
    }

    val budgetsApi: BudgetsApi by lazy {
        retrofit.create(BudgetsApi::class.java)
    }

    // ── Repositories ──────────────────────────────────────────────────────────

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(authApi, sessionManager)
    }

    val householdRepository: HouseholdRepository by lazy {
        HouseholdRepositoryImpl(authApi)
    }

    val dashboardRepository: DashboardRepository by lazy {
        DashboardRepositoryImpl(dashboardApi)
    }

    val accountsRepository: AccountsRepository by lazy {
        AccountsRepositoryImpl(accountsApi, sessionManager)
    }

    val transactionsRepository: TransactionsRepository by lazy {
        TransactionsRepositoryImpl(
            api            = transactionsApi,
            accountsApi    = accountsApi,
            sessionManager = sessionManager
        )
    }

    val transactionDetailRepository: TransactionDetailRepository by lazy {
        TransactionDetailRepositoryImpl(transactionDetailApi)
    }

    val categoriesRepository: CategoriesRepository by lazy {
        CategoriesRepositoryImpl(categoriesApi)
    }

    val recurringBillsRepository: RecurringBillsRepository by lazy {
        RecurringBillsRepositoryImpl(recurringBillsApi)
    }

    val inventoryRepository: InventoryRepository by lazy {
        InventoryRepositoryImpl(inventoryApi)
    }

    val budgetsRepository: BudgetsRepository by lazy {
        BudgetsRepositoryImpl(budgetsApi, categoriesApi, sessionManager)
    }
}
