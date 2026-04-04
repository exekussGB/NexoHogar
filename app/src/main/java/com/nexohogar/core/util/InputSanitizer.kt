package com.nexohogar.core.util

import android.util.Patterns

/**
 * Utilidad centralizada para sanitización de inputs.
 * Referencia: nexohogar_report.md — Sección F.2 (Fix #1 prioritario)
 */
object InputSanitizer {
    fun sanitizeText(input: String, maxLength: Int = 200): String {
        return input
            .trim()
            .take(maxLength)
            .replace(Regex("[<>\"'\`;{}\\[\\]\\\\]"), "")
            .replace(Regex("\\s+"), " ") // Normalizar espacios
    }

    fun sanitizeAmount(input: String): Double? {
        val cleaned = input.trim().replace(",", ".")
        val value = cleaned.toDoubleOrNull() ?: return null
        return if (value > 0 && value <= 999_999_999) value else null
    }

    fun sanitizeUrl(input: String): String? {
        if (input.isBlank()) return null
        val trimmed = input.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return null
        return if (Patterns.WEB_URL.matcher(trimmed).matches()) trimmed else null
    }
}
