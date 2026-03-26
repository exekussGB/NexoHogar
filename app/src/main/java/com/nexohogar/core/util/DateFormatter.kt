package com.nexohogar.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalDate

object DateFormatter {
    private val chileTz = ZoneId.of("America/Santiago")
    private val displayFormat = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
    private val dateOnlyFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun formatForDisplay(isoDate: String): String {
        return try {
            val instant = Instant.parse(isoDate)
            val local = instant.atZone(chileTz)
            local.format(displayFormat)
        } catch (e: Exception) {
            try {
                // Try parsing as LocalDate (yyyy-MM-dd)
                val date = LocalDate.parse(isoDate)
                date.format(dateOnlyFormat)
            } catch (e2: Exception) {
                isoDate // fallback
            }
        }
    }
}
