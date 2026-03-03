package com.nexohogar.core.result

/**
 * Clase sellada genérica para representar el estado de una operación (Network/Database).
 * Proporciona estados para éxito, error y carga.
 */
sealed class AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : AppResult<Nothing>()
    object Loading : AppResult<Nothing>()
}
