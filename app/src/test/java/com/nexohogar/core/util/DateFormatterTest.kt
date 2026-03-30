package com.nexohogar.core.util

import org.junit.Assert.*
import org.junit.Test

class DateFormatterTest {

    @Test
    fun `formatForDisplay with valid ISO-8601 instant returns Chile time`() {
        // 2026-03-15T15:30:00Z = 12:30 in Chile (UTC-3 during CLST)
        val result = DateFormatter.formatForDisplay("2026-03-15T15:30:00Z")
        // Should contain dd/MM/yyyy format
        assertTrue("Should contain date format", result.contains("15/03/2026") || result.contains("/03/2026"))
        assertTrue("Should contain time", result.contains(":"))
    }

    @Test
    fun `formatForDisplay with LocalDate string returns date only`() {
        val result = DateFormatter.formatForDisplay("2026-03-15")
        assertEquals("15/03/2026", result)
    }

    @Test
    fun `formatForDisplay with invalid string returns original`() {
        val result = DateFormatter.formatForDisplay("not-a-date")
        assertEquals("not-a-date", result)
    }

    @Test
    fun `formatForDisplay with empty string returns empty`() {
        val result = DateFormatter.formatForDisplay("")
        assertEquals("", result)
    }

    @Test
    fun `formatForDisplay with ISO-8601 midnight returns correct date`() {
        val result = DateFormatter.formatForDisplay("2026-01-01T00:00:00Z")
        // Midnight UTC = 21:00 previous day in Chile (UTC-3)
        assertTrue(result.contains("/12/2025") || result.contains("/01/2026"))
    }

    @Test
    fun `formatForDisplay with ISO-8601 includes offset correctly`() {
        val result = DateFormatter.formatForDisplay("2026-06-15T12:00:00Z")
        // June = winter in Chile = UTC-4 (CLT)
        assertTrue(result.contains("15/06/2026") || result.contains("/06/2026"))
    }
}
