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

/**
 * COH-03: Extensión para extraer el valor de un AppResult.Success o lanzar excepción.
 * Útil para ViewModels que ya manejan errores con try/catch.
 */
fun <T> AppResult<T>.getOrThrow(): T = when (this) {
    is AppResult.Success -> data
    is AppResult.Error -> throw Exception(message, exception)
    is AppResult.Loading -> throw IllegalStateException("Operation still loading")
}
