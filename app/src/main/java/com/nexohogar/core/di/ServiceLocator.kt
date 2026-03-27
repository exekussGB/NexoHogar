package com.nexohogar.core.di

import android.content.Context
import com.nexohogar.core.network.AuthInterceptor
import com.nexohogar.core.network.SupabaseConfig
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.AccountsApi
import com.nexohogar.data.network.AuthApi
import com.nexohogar.data.network.BudgetApi
import com.nexohogar.data.network.CategoriesApi
import com.nexohogar.data.network.CategoryExpensesApi
import com.nexohogar.data.network.DashboardApi
import com.nexohogar.data.network.InventoryApi
import com.nexohogar.data.network.PersonalDashboardApi
import com.nexohogar.data.network.RecurringBillsApi
import com.nexohogar.data.network.TransactionDetailApi
import com.nexohogar.data.network.TransactionsApi
import com.nexohogar.data.repository.AccountsRepositoryImpl
import com.nexohogar.data.repository.AuthRepositoryImpl
import com.nexohogar.data.repository.BudgetRepositoryImpl
import com.nexohogar.data.repository.CategoriesRepositoryImpl
import com.nexohogar.data.repository.CategoryExpensesRepositoryImpl
import com.nexohogar.data.repository.DashboardRepositoryImpl
import com.nexohogar.data.repository.HouseholdRepositoryImpl
import com.nexohogar.data.repository.InventoryRepositoryImpl
import com.nexohogar.data.repository.PersonalDashboardRepositoryImpl
import com.nexohogar.data.repository.RecurringBillsRepositoryImpl
import com.nexohogar.data.repository.TransactionDetailRepositoryImpl
import com.nexohogar.data.repository.TransactionsRepositoryImpl
import com.nexohogar.domain.repository.AccountsRepository
import com.nexohogar.domain.repository.AuthRepository
import com.nexohogar.domain.repository.BudgetRepository
import com.nexohogar.domain.repository.CategoriesRepository
import com.nexohogar.domain.repository.CategoryExpensesRepository
import com.nexohogar.domain.repository.DashboardRepository
import com.nexohogar.domain.repository.HouseholdRepository
import com.nexohogar.domain.repository.InventoryRepository
import com.nexohogar.domain.repository.PersonalDashboardRepository
import com.nexohogar.domain.repository.RecurringBillsRepository
import com.nexohogar.domain.repository.TransactionDetailRepository
import com.nexohogar.domain.repository.TransactionsRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.nexohogar.presentation.scanner.ReceiptScannerViewModel
import com.nexohogar.data.network.FcmApi

/**
 * Contenedor de dependencias manual (Service Locator).
 * Proporciona instancias únicas (lazy) de todos los repositorios, APIs y servicios.
 * Se inicializa una vez en NexoHogarApp.onCreate().
 */
object ServiceLocator {

    private lateinit var appContext: Context

    /** Llamar desde Application.onCreate() o Activity.onCreate() antes de usar cualquier propiedad. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // ── Sesión y Tenant ───────────────────────────────────────────────────────

    val sessionManager: SessionManager by lazy {
        SessionManager(appContext)
    }

    val tenantContext: TenantContext by lazy {
        TenantContext(sessionManager)
    }

    // ── Red ───────────────────────────────────────────────────────────────────

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(SupabaseConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ── APIs ──────────────────────────────────────────────────────────────────

    private val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    private val accountsApi: AccountsApi by lazy {
        retrofit.create(AccountsApi::class.java)
    }

    private val dashboardApi: DashboardApi by lazy {
        retrofit.create(DashboardApi::class.java)
    }

    private val transactionsApi: TransactionsApi by lazy {
        retrofit.create(TransactionsApi::class.java)
    }

    private val categoriesApi: CategoriesApi by lazy {
        retrofit.create(CategoriesApi::class.java)
    }

    private val transactionDetailApi: TransactionDetailApi by lazy {
        retrofit.create(TransactionDetailApi::class.java)
    }

    private val recurringBillsApi: RecurringBillsApi by lazy {
        retrofit.create(RecurringBillsApi::class.java)
    }

    private val inventoryApi: InventoryApi by lazy {
        retrofit.create(InventoryApi::class.java)
    }

    private val budgetApi: BudgetApi by lazy {
        retrofit.create(BudgetApi::class.java)
    }

    private val categoryExpensesApi: CategoryExpensesApi by lazy {
        retrofit.create(CategoryExpensesApi::class.java)
    }

    private val personalDashboardApi: PersonalDashboardApi by lazy {
        retrofit.create(PersonalDashboardApi::class.java)
    }

    // ── Repositorios ──────────────────────────────────────────────────────────

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
        TransactionsRepositoryImpl(transactionsApi, accountsApi, sessionManager)
    }

    val categoriesRepository: CategoriesRepository by lazy {
        CategoriesRepositoryImpl(categoriesApi)
    }

    val transactionDetailRepository: TransactionDetailRepository by lazy {
        TransactionDetailRepositoryImpl(transactionDetailApi)
    }

    val recurringBillsRepository: RecurringBillsRepository by lazy {
        RecurringBillsRepositoryImpl(recurringBillsApi)
    }

    val inventoryRepository: InventoryRepository by lazy {
        InventoryRepositoryImpl(inventoryApi)
    }

    val budgetRepository: BudgetRepository by lazy {
        BudgetRepositoryImpl(budgetApi)
    }

    val categoryExpensesRepository: CategoryExpensesRepository by lazy {
        CategoryExpensesRepositoryImpl(categoryExpensesApi)
    }

    val personalDashboardRepository: PersonalDashboardRepository by lazy {
        PersonalDashboardRepositoryImpl(personalDashboardApi)
    }

    val fcmApi: FcmApi by lazy {
        retrofit.create(FcmApi::class.java)

    fun provideReceiptScannerViewModel(): ReceiptScannerViewModel {
        return ReceiptScannerViewModel(
            inventoryRepository = inventoryRepository,
            accountsRepository = accountsRepository,
            tenantContext = tenantContext
        )
    }
}
