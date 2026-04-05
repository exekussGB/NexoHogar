package com.nexohogar.core.di

import android.annotation.SuppressLint
import android.content.Context
import com.nexohogar.BuildConfig
import com.nexohogar.core.network.AuthInterceptor
import com.nexohogar.core.network.SupabaseConfig
import com.nexohogar.core.session.SessionRefresher
import com.nexohogar.core.session.SupabaseDataStoreSessionManager
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.*
import com.nexohogar.data.network.WishlistApi
import com.nexohogar.data.repository.WishlistRepositoryImpl
import com.nexohogar.domain.repository.WishlistRepository
import com.nexohogar.data.repository.*
import com.nexohogar.domain.repository.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.nexohogar.core.tutorial.TutorialManager
import com.nexohogar.data.local.ThemePreferences
import com.nexohogar.data.local.NotificationPreferences
import com.nexohogar.core.biometric.BiometricHelper

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
        TenantContext(sessionManager, supabaseClient)
    }

    /**
     * SDK de Supabase con gestión de sesión persistente en DataStore.
     * - [autoLoadFromStorage = true]: restaura la sesión al iniciar la app.
     * - [alwaysAutoRefresh = true]: renueva el access_token automáticamente
     *   antes de que expire → el usuario nunca es expulsado por expiración.
     */
    val supabaseClient: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.BASE_URL,
            supabaseKey = SupabaseConfig.API_KEY
        ) {
            install(Auth) {
                sessionManager = SupabaseDataStoreSessionManager(context)
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
            }
        }
    }

    // ── Legacy: mantenido por compatibilidad pero ya no gestiona tokens ──────
    val sessionRefresher: SessionRefresher by lazy {
        SessionRefresher(sessionManager)
    }

    // ── Tutorial ──────────────────────────────────────────────────────────
    val tutorialManager: TutorialManager by lazy {
        TutorialManager(context)
    }

    // ── Theme & Notifications ─────────────────────────────────────────
    val themePreferences: ThemePreferences by lazy {
        ThemePreferences(context)
    }

    val notificationPreferences: NotificationPreferences by lazy {
        NotificationPreferences(context)
    }

    // ── Biometric ─────────────────────────────────────────────────────
    val biometricHelper: BiometricHelper by lazy {
        BiometricHelper(context)
    }

    // ── Network ───────────────────────────────────────────────────────────────

    private val authInterceptor: AuthInterceptor by lazy {
        AuthInterceptor(sessionManager, supabaseClient)
    }

    /**
     * SEC-07: Certificate pinning para Supabase.
     */
    private val certificatePinner: CertificatePinner by lazy {
        val supabaseHost = SupabaseConfig.BASE_URL
            .removePrefix("https://")
            .removeSuffix("/")
        CertificatePinner.Builder()
            .add(supabaseHost, "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=")
            .add(supabaseHost, "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=")
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .certificatePinner(certificatePinner)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(SupabaseConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ── API interfaces ────────────────────────────────────────────────────────

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val dashboardApi: DashboardApi by lazy { retrofit.create(DashboardApi::class.java) }
    val accountsApi: AccountsApi by lazy { retrofit.create(AccountsApi::class.java) }
    val transactionsApi: TransactionsApi by lazy { retrofit.create(TransactionsApi::class.java) }
    val transactionDetailApi: TransactionDetailApi by lazy { retrofit.create(TransactionDetailApi::class.java) }
    val categoriesApi: CategoriesApi by lazy { retrofit.create(CategoriesApi::class.java) }
    val recurringBillsApi: RecurringBillsApi by lazy { retrofit.create(RecurringBillsApi::class.java) }
    val inventoryApi: InventoryApi by lazy { retrofit.create(InventoryApi::class.java) }
    val fcmApi: FcmApi by lazy { retrofit.create(FcmApi::class.java) }
    val budgetApi: BudgetApi by lazy { retrofit.create(BudgetApi::class.java) }
    val wishlistApi: WishlistApi by lazy { retrofit.create(WishlistApi::class.java) }
    val categoryExpensesApi: CategoryExpensesApi by lazy { retrofit.create(CategoryExpensesApi::class.java) }
    val personalDashboardApi: PersonalDashboardApi by lazy { retrofit.create(PersonalDashboardApi::class.java) }

    // ── Repositories ──────────────────────────────────────────────────────────

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(authApi, sessionManager, supabaseClient)
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

    val budgetRepository: BudgetRepository by lazy {
        BudgetRepositoryImpl(budgetApi)
    }

    val categoryExpensesRepository: CategoryExpensesRepository by lazy {
        CategoryExpensesRepositoryImpl(categoryExpensesApi)
    }

    val personalDashboardRepository: PersonalDashboardRepository by lazy {
        PersonalDashboardRepositoryImpl(personalDashboardApi)
    }

    val wishlistRepository: WishlistRepository by lazy {
        WishlistRepositoryImpl(wishlistApi)
    }
}
