package com.nexohogar.core.util

import com.nexohogar.core.util.PasswordValidator.PasswordStrength
import org.junit.Assert.*
import org.junit.Test

class PasswordValidatorTest {

    @Test
    fun `empty password returns EMPTY strength`() {
        val result = PasswordValidator.validate("")
        assertEquals(PasswordStrength.EMPTY, result.strength)
        assertFalse(result.meetsMinimum)
        assertTrue(result.suggestions.isEmpty())
    }

    @Test
    fun `very short password does not meet minimum`() {
        val result = PasswordValidator.validate("Ab1")
        assertFalse(result.meetsMinimum)
        assertTrue(result.suggestions.any { it.contains("8 caracteres") })
    }

    @Test
    fun `password with only lowercase does not meet minimum`() {
        val result = PasswordValidator.validate("abcdefgh")
        assertFalse(result.meetsMinimum)
    }

    @Test
    fun `password with only uppercase does not meet minimum`() {
        val result = PasswordValidator.validate("ABCDEFGH")
        assertFalse(result.meetsMinimum)
    }

    @Test
    fun `password meeting minimum requirements passes`() {
        // >= 8 chars, has uppercase, lowercase, and digit
        val result = PasswordValidator.validate("Abcdefg1")
        assertTrue(result.meetsMinimum)
    }

    @Test
    fun `strong password with special chars gets high score`() {
        val result = PasswordValidator.validate("Abcdefg1!")
        assertTrue(result.meetsMinimum)
        assertTrue(result.strength.score >= 4)
    }

    @Test
    fun `very strong password with 12+ chars gets maximum score`() {
        val result = PasswordValidator.validate("MyP@ssw0rd123!")
        assertTrue(result.meetsMinimum)
        assertEquals(PasswordStrength.VERY_STRONG, result.strength)
        assertTrue(result.suggestions.isEmpty())
    }

    @Test
    fun `password without digit suggests including number`() {
        val result = PasswordValidator.validate("Abcdefgh")
        assertFalse(result.meetsMinimum)
        assertTrue(result.suggestions.any { it.contains("número") })
    }

    @Test
    fun `password without uppercase suggests including uppercase`() {
        val result = PasswordValidator.validate("abcdefg1")
        assertFalse(result.meetsMinimum)
        assertTrue(result.suggestions.any { it.contains("mayúscula") })
    }

    @Test
    fun `password without special char suggests including one`() {
        val result = PasswordValidator.validate("Abcdefg1")
        assertTrue(result.suggestions.any { it.contains("especial") })
    }

    @Test
    fun `score calculation is correct for weak password`() {
        // Only lowercase, 5 chars → score 1 (lowercase only)
        val result = PasswordValidator.validate("abcde")
        assertEquals(PasswordStrength.VERY_WEAK, result.strength)
    }

    @Test
    fun `8-char password gets length score of 1`() {
        // 8 chars, lower + upper + digit = score 1 + 1 + 1 + 1 = 4 (STRONG)
        val result = PasswordValidator.validate("Abcdefg1")
        assertEquals(PasswordStrength.STRONG, result.strength)
    }

    @Test
    fun `12-char password gets extra length score`() {
        // 12 chars, lower + upper + digit + special = score 2 + 1 + 1 + 1 + 1 = 6 (VERY_STRONG)
        val result = PasswordValidator.validate("Abcdefghij1!")
        assertEquals(PasswordStrength.VERY_STRONG, result.strength)
    }
}
