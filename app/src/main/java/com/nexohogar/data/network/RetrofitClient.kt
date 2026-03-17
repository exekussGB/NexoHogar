package com.nexohogar.data.network

import android.util.Log
import com.google.gson.GsonBuilder
import com.nexohogar.core.network.SupabaseConfig
import com.nexohogar.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://fpsdkpurugviwygfuljp.supabase.co/"

    lateinit var sessionManager: SessionManager

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val supabaseInterceptor = Interceptor { chain ->

        val originalRequest = chain.request()

        val builder = originalRequest.newBuilder()
            .addHeader("apikey", SupabaseConfig.API_KEY)
            .addHeader("Content-Type", "application/json")

        val token = sessionManager.fetchAuthToken()
        Log.d("HF_DEBUG", "Token enviado: $token")

        if (!token.isNullOrEmpty()) {
            builder.addHeader("Authorization", "Bearer $token")
        }

        Log.d("HF_DEBUG", "Request URL: ${originalRequest.url}")
        chain.proceed(builder.build())
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(supabaseInterceptor)
        .addInterceptor(logging)
        .build()

    private val retrofit: Retrofit by lazy {
        val gson = GsonBuilder()
            .serializeNulls()
            .create()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val transactionsApi: TransactionsApi by lazy {
        retrofit.create(TransactionsApi::class.java)
    }
}
