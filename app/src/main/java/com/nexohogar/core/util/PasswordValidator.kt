package com.nexohogar.core.util

/**
 * Validador de fortaleza de contraseña.
 * Evalúa múltiples criterios y retorna un nivel de seguridad.
 */
object PasswordValidator {

    enum class PasswordStrength(val label: String, val score: Int) {
        EMPTY("", 0),
        VERY_WEAK("Muy débil", 1),
        WEAK("Débil", 2),
        FAIR("Regular", 3),
        STRONG("Fuerte", 4),
        VERY_STRONG("Muy fuerte", 5)
    }

    data class ValidationResult(
        val strength: PasswordStrength,
        val suggestions: List<String>,
        val meetsMinimum: Boolean
    )

    fun validate(password: String): ValidationResult {
        if (password.isEmpty()) {
            return ValidationResult(PasswordStrength.EMPTY, emptyList(), false)
        }

        val suggestions = mutableListOf<String>()
        var score = 0

        // Longitud
        when {
            password.length >= 12 -> score += 2
            password.length >= 8 -> score += 1
            else -> suggestions.add("Mínimo 8 caracteres")
        }

        // Mayúsculas
        if (password.any { it.isUpperCase() }) score += 1
        else suggestions.add("Incluir al menos una mayúscula")

        // Minúsculas
        if (password.any { it.isLowerCase() }) score += 1
        else suggestions.add("Incluir al menos una minúscula")

        // Números
        if (password.any { it.isDigit() }) score += 1
        else suggestions.add("Incluir al menos un número")

        // Caracteres especiales
        if (password.any { !it.isLetterOrDigit() }) score += 1
        else suggestions.add("Incluir un carácter especial (!@#\$%)")

        val strength = when {
            score <= 1 -> PasswordStrength.VERY_WEAK
            score == 2 -> PasswordStrength.WEAK
            score == 3 -> PasswordStrength.FAIR
            score == 4 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }

        val meetsMinimum = password.length >= 8
                && password.any { it.isUpperCase() }
                && password.any { it.isLowerCase() }
                && password.any { it.isDigit() }

        return ValidationResult(strength, suggestions, meetsMinimum)
    }
}
