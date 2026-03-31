package com.nexohogar.core.di

import android.annotation.SuppressLint
import android.content.Context
import com.nexohogar.BuildConfig
import com.nexohogar.core.network.AuthInterceptor
import com.nexohogar.core.network.SupabaseConfig
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.*
import com.nexohogar.data.repository.*
import com.nexohogar.domain.repository.*
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.nexohogar.core.tutorial.TutorialManager
import com.nexohogar.data.local.ThemePreferences
import com.nexohogar.data.network.service.AiReceiptParserService
import com.nexohogar.data.local.NotificationPreferences

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

    // ── Network ───────────────────────────────────────────────────────────────

    private val authInterceptor: AuthInterceptor by lazy {
        AuthInterceptor(sessionManager)
    }

    /**
     * SEC-07: Certificate pinning para Supabase.
     * Se fija al certificado intermedio (Google Trust Services WE1) y al root (GTS Root R4)
     * para tolerar rotación del certificado leaf.
     *
     * ⚠️ Si Supabase cambia de CA, estos pins deberán actualizarse.
     * Revisar periódicamente con:
     *   openssl s_client -servername <host> -connect <host>:443 -showcerts
     */
    private val certificatePinner: CertificatePinner by lazy {
        val supabaseHost = SupabaseConfig.BASE_URL
            .removePrefix("https://")
            .removeSuffix("/")
        CertificatePinner.Builder()
            // Intermediate: Google Trust Services WE1
            .add(supabaseHost, "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=")
            // Root: GTS Root R4
            .add(supabaseHost, "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=")
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        // SEC-04: Solo loguear body HTTP en debug, NONE en release
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .certificatePinner(certificatePinner) // SEC-07
            .build()
    }

    private val retrofit: Retrofit by lazy {
        // SEC-01: Usar SupabaseConfig.BASE_URL en lugar de URL hardcodeada
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

    // ── AI Receipt Parser ─────────────────────────────────────────────────────
    val aiReceiptParserService: AiReceiptParserService by lazy {
        AiReceiptParserService(okHttpClient)
    }

    val fcmApi: FcmApi by lazy { retrofit.create(FcmApi::class.java) }
    val budgetApi: BudgetApi by lazy { retrofit.create(BudgetApi::class.java) }
    val categoryExpensesApi: CategoryExpensesApi by lazy { retrofit.create(CategoryExpensesApi::class.java) }
    val personalDashboardApi: PersonalDashboardApi by lazy { retrofit.create(PersonalDashboardApi::class.java) }

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

    val budgetRepository: BudgetRepository by lazy {
        BudgetRepositoryImpl(budgetApi)
    }

    val categoryExpensesRepository: CategoryExpensesRepository by lazy {
        CategoryExpensesRepositoryImpl(categoryExpensesApi)
    }

    val personalDashboardRepository: PersonalDashboardRepository by lazy {
        PersonalDashboardRepositoryImpl(personalDashboardApi)
    }
}
