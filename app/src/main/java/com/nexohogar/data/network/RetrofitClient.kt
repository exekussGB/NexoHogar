package com.nexohogar.data.network

import com.nexohogar.core.network.SupabaseConfig
import com.nexohogar.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://fpsdkpurugviwygfuljp.supabase.co/"

    // ⚠️ NECESITAMOS SESSION MANAGER
    lateinit var sessionManager: SessionManager

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val supabaseInterceptor = Interceptor { chain ->

        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()
            .addHeader("apikey", SupabaseConfig.API_KEY)

        // 🔥 AGREGAMOS TOKEN SI EXISTE
        val token = sessionManager.fetchAuthToken()
        if (!token.isNullOrEmpty()) {
            builder.addHeader("Authorization", "Bearer $token")
        }

        chain.proceed(builder.build())
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(supabaseInterceptor)
        .addInterceptor(logging)
        .build()

    val instance: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
}
