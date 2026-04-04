package com.nexohogar.core.util

import android.util.Patterns

object InputSanitizer {
    private val DANGEROUS_CHARS = Regex("""[<>"'`;\{\}\[\]\\]""")

    fun sanitizeText(input: String, maxLength: Int = 200): String {
        return input
            .trim()
            .take(maxLength)
            .replace(DANGEROUS_CHARS, "")
            .replace(Regex("""\s+"""), " ")
    }

    fun sanitizeAmount(input: String): Long? {
        val cleaned = input.trim().replace(",", ".")
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value <= 0 || value > 999_999_999) return null
        return value.toLong()
    }

    fun sanitizeUrl(input: String): String? {
        if (input.isBlank()) return null
        val trimmed = input.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return null
        return if (Patterns.WEB_URL.matcher(trimmed).matches()) trimmed else null
    }
}
