package com.nexohogar.domain.model

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Modelo de dominio para una cuenta recurrente (servicios mensuales).
 *
 * @param dueDayOfMonth día del mes en que vence (1-31)
 * @param lastPaidDate fecha del último pago en formato "YYYY-MM-DD", o null si nunca se ha pagado
 */
data class RecurringBill(
    val id: String,
    val householdId: String,
    val name: String,
    val amountClp: Long,
    val dueDayOfMonth: Int,
    val isActive: Boolean,
    val lastPaidDate: String?,
    val notes: String?,
    val createdAt: String
) {
    companion object {
        private val ZONE = ZoneId.of("America/Santiago")
    }

    /**
     * Calcula cuántos días faltan para el vencimiento este mes.
     * Retorna [Int.MAX_VALUE] si ya fue pagado este mes.
     * Retorna negativo si ya venció sin pagar.
     */
    fun daysUntilDue(): Int {
        val today = LocalDate.now(ZONE)

        // Si ya fue pagado este mes → no hay urgencia
        if (!lastPaidDate.isNullOrBlank()) {
            try {
                val paidDate = LocalDate.parse(lastPaidDate)
                if (paidDate.year == today.year && paidDate.monthValue == today.monthValue) {
                    return Int.MAX_VALUE
                }
            } catch (_: Exception) { }
        }

        // Calcular fecha de vencimiento: si el día ya pasó este mes, usar el mes siguiente
        var dueDate = today.withDayOfMonth(minOf(dueDayOfMonth, today.lengthOfMonth()))
        if (dueDate.isBefore(today)) {
            val nextMonth = today.plusMonths(1)
            dueDate = nextMonth.withDayOfMonth(minOf(dueDayOfMonth, nextMonth.lengthOfMonth()))
        }
        return ChronoUnit.DAYS.between(today, dueDate).toInt()
    }

    /**
     * Estado visual tipo semáforo.
     *
     * 🟢 GREEN  → pagado este mes O faltan > 3 días
     * 🟡 YELLOW → faltan ≤ 3 días (y no pagado)
     * 🔴 RED    → vencido (día actual > due_day y no pagado)
     * ⚪ INACTIVE → cuenta pausada
     */
    fun getStatus(): BillStatus {
        if (!isActive) return BillStatus.INACTIVE
        val days = daysUntilDue()
        return when {
            days == Int.MAX_VALUE -> BillStatus.GREEN   // Pagado
            days < 0              -> BillStatus.RED     // Vencido
            days <= 3             -> BillStatus.YELLOW  // Próximo a vencer
            else                  -> BillStatus.GREEN   // Holgado
        }
    }

    /** Texto descriptivo del estado */
    fun statusLabel(): String {
        if (!isActive) return "PAUSADO"
        val days = daysUntilDue()
        return when {
            days == Int.MAX_VALUE -> "PAGADO"
            days < 0              -> "VENCIDO"
            days == 0             -> "VENCE HOY"
            days == 1             -> "VENCE MAÑANA"
            days <= 3             -> "VENCE EN $days DÍAS"
            else                  -> "Día $dueDayOfMonth"
        }
    }

    /**
     * Estado con nombres descriptivos, usado por RecurringBillsScreen.
     */
    fun status(): RecurringBillStatus {
        if (!isActive) return RecurringBillStatus.INACTIVE
        val days = daysUntilDue()
        return when {
            days == Int.MAX_VALUE -> RecurringBillStatus.PAID
            days < 0              -> RecurringBillStatus.OVERDUE
            days <= 3             -> RecurringBillStatus.DUE_SOON
            else                  -> RecurringBillStatus.OK
        }
    }
}

enum class BillStatus {
    GREEN,    // pagado o faltan > 3 días
    YELLOW,   // faltan ≤ 3 días
    RED,      // vencido
    INACTIVE  // cuenta desactivada
}

/** Estado descriptivo para la UI de RecurringBillsScreen */
enum class RecurringBillStatus {
    PAID,      // pagado este mes
    OK,        // al día, faltan > 3 días
    DUE_SOON,  // faltan ≤ 3 días
    OVERDUE,   // vencido sin pagar
    INACTIVE   // cuenta pausada
}
