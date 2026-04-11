package com.nexohogar.core.network

import com.nexohogar.BuildConfig

/**
 * Configuración centralizada para Supabase.
 * SEC-01: API Key se inyecta desde local.properties via BuildConfig.
 */
object SupabaseConfig {
    val BASE_URL: String  = BuildConfig.SUPABASE_URL
    val API_KEY: String   = BuildConfig.SUPABASE_KEY
}