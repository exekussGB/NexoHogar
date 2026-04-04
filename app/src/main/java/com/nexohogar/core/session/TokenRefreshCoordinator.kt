package com.nexohogar.core.session

import android.util.Log
import com.google.gson.Gson
import com.nexohogar.core.network.SupabaseConfig
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.mapper.toDomain
import com.nexohogar.data.remote.dto.LoginResponse
import com.nexohogar.domain.model.UserSession
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object TokenRefreshCoordinator {

    private const val TAG = "TokenRefreshCoordinator"
    private val lock = Any()

    @Volatile
    private var lastSuccessMs = 0L
    private const val DEBOUNCE_MS = 5_000L
    private const val MAX_RETRIES = 3
    private val RETRY_DELAYS = longArrayOf(0, 1_500, 3_000)

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val gson: Gson by lazy { Gson() }

    // HTTP codes that mean the refresh token is PERMANENTLY invalid
    private val PERMANENT_REJECTION_CODES = setOf(400, 401, 403)

    fun refresh(sessionManager: SessionManager): RefreshResult = synchronized(lock) {
        if (!sessionManager.isTokenExpired()) {
            Log.d(TAG, "✅ Token still valid — no refresh needed")
            return@synchronized RefreshResult.AlreadyFresh
        }

        val now = System.currentTimeMillis()
        if (now - lastSuccessMs < DEBOUNCE_MS && !sessionManager.isTokenExpired()) {
            Log.d(TAG, "⏭️ Debounce — refresh succeeded ${(now - lastSuccessMs) / 1000}s ago")
            return@synchronized RefreshResult.AlreadyFresh
        }

        // Cache values BEFORE making HTTP calls — EncryptedSharedPreferences might fail later
        val cachedRefreshToken = sessionManager.fetchRefreshToken()
        val cachedSession = sessionManager.fetchSession()

        if (cachedRefreshToken.isNullOrBlank()) {
            Log.e(TAG, "❌ No refresh_token available (prefs AND backup empty)")
            return@synchronized RefreshResult.ServerRejected
        }

        Log.d(TAG, "🔑 Starting refresh with token ${cachedRefreshToken.take(8)}...")

        var lastResult: RefreshResult = RefreshResult.NetworkError("No attempt made")

        for (attempt in 0 until MAX_RETRIES) {
            if (attempt > 0) {
                val delayMs = RETRY_DELAYS.getOrElse(attempt) { 3_000L }
                Log.d(TAG, "🔄 Retry #$attempt after ${delayMs}ms")
                try { Thread.sleep(delayMs) } catch (_: InterruptedException) { break }
            }

            lastResult = doSingleRefresh(
                sessionManager = sessionManager,
                refreshToken = cachedRefreshToken,
                fallbackUserId = cachedSession?.userId,
                fallbackEmail = cachedSession?.email
            )

            when (lastResult) {
                is RefreshResult.Success -> {
                    lastSuccessMs = System.currentTimeMillis()
                    Log.d(TAG, "✅ Token refreshed on attempt #$attempt")
                    return@synchronized lastResult
                }
                is RefreshResult.ServerRejected -> {
                    Log.e(TAG, "❌ Server permanently rejected refresh — not retrying")
                    return@synchronized lastResult
                }
                is RefreshResult.NetworkError -> {
                    Log.w(TAG, "⚠️ Transient error on attempt #$attempt: ${lastResult.message}")
                    continue
                }
                is RefreshResult.AlreadyFresh -> {
                    return@synchronized lastResult
                }
            }
        }

        Log.e(TAG, "❌ Refresh failed after $MAX_RETRIES attempts: $lastResult")
        lastResult
    }

    private fun doSingleRefresh(
        sessionManager: SessionManager,
        refreshToken: String,
        fallbackUserId: String?,
        fallbackEmail: String?
    ): RefreshResult {
        return try {
            val bodyJson = gson.toJson(mapOf("refresh_token" to refreshToken))
            val body = bodyJson.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${SupabaseConfig.BASE_URL}auth/v1/token?grant_type=refresh_token")
                .post(body)
                .header("apikey", SupabaseConfig.API_KEY)
                .header("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseCode = response.code
            val responseBody = response.body?.string()

            Log.d(TAG, "📡 Supabase responded HTTP $responseCode")

            if (response.isSuccessful) {
                if (responseBody.isNullOrBlank()) {
                    Log.e(TAG, "❌ Empty response body on 200")
                    return RefreshResult.NetworkError("Empty response body")
                }

                val loginResponse = try {
                    gson.fromJson(responseBody, LoginResponse::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ JSON parse error: ${e.message}")
                    return RefreshResult.NetworkError("JSON parse error: ${e.message}")
                }

                // Try standard mapping first
                var newSession = loginResponse.toDomain()

                // Fallback: build session manually if toDomain() returned null
                if (newSession == null &&
                    loginResponse.accessToken != null &&
                    loginResponse.refreshToken != null
                ) {
                    val expSec = loginResponse.expiresIn ?: 3600L
                    val userId = loginResponse.user?.id ?: fallbackUserId ?: ""
                    val email = loginResponse.user?.email ?: fallbackEmail ?: ""

                    if (userId.isNotBlank()) {
                        newSession = UserSession(
                            accessToken = loginResponse.accessToken,
                            refreshToken = loginResponse.refreshToken,
                            userId = userId,
                            email = email,
                            expiresAt = System.currentTimeMillis() + (expSec * 1000L)
                        )
                        Log.d(TAG, "🔄 Built session with fallback userId/email")
                    }
                }

                if (newSession == null) {
                    Log.e(TAG, "❌ Could not build UserSession from response. Body: ${responseBody.take(200)}")
                    return RefreshResult.NetworkError("Could not parse session from response")
                }

                sessionManager.saveSession(newSession)
                Log.d(TAG, "✅ Token refreshed — new token ${newSession.accessToken.take(8)}..., expires in ${loginResponse.expiresIn}s")
                RefreshResult.Success(newSession.accessToken)

            } else {
                // CRITICAL FIX: Only permanent rejection for 400/401/403
                return if (responseCode in PERMANENT_REJECTION_CODES) {
                    Log.e(TAG, "❌ Permanent rejection: HTTP $responseCode — ${responseBody?.take(200)}")
                    RefreshResult.ServerRejected
                } else {
                    // 5xx, 429, etc — transient, retryable
                    Log.w(TAG, "⚠️ Transient server error: HTTP $responseCode — ${responseBody?.take(200)}")
                    RefreshResult.NetworkError("HTTP $responseCode")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Network exception: ${e.javaClass.simpleName}: ${e.message}")
            RefreshResult.NetworkError(e.message ?: "Unknown network error")
        }
    }
}
