package com.nexohogar.domain.model

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Modelo de dominio para una cuenta recurrente (servicios mensuales).
 *
 * @param dueDayOfMonth día del mes en que vence (1-31)
 * @param lastPaidDate fecha del último pago en formato "YYYY-MM-DD", o null si nunca se ha pagado
 * @param totalInstallments total de cuotas (null = cuenta fija, sin cuotas)
 * @param paidInstallments cuotas ya pagadas
 * @param categoryId ID de categoría asociada (opcional)
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
    val createdAt: String,
    val totalInstallments: Int? = null,
    val paidInstallments: Int = 0,
    val categoryId: String? = null
) {
    companion object {
        private val ZONE = ZoneId.of("America/Santiago")
    }

    /** true si esta cuenta es en cuotas */
    val isInstallment: Boolean get() = totalInstallments != null && totalInstallments > 0

    /** Etiqueta de cuotas, ej: "3/12 cuotas" */
    val installmentLabel: String?
        get() = if (isInstallment) "$paidInstallments/$totalInstallments cuotas" else null

    /** true si ya se pagaron todas las cuotas */
    val installmentsCompleted: Boolean
        get() = isInstallment && paidInstallments >= (totalInstallments ?: 0)

    fun daysUntilDue(): Int {
        val today = LocalDate.now(ZONE)
        if (!lastPaidDate.isNullOrBlank()) {
            try {
                val paidDate = LocalDate.parse(lastPaidDate)
                if (paidDate.year == today.year && paidDate.monthValue == today.monthValue) {
                    return Int.MAX_VALUE
                }
            } catch (_: Exception) { }
        }
        var dueDate = today.withDayOfMonth(minOf(dueDayOfMonth, today.lengthOfMonth()))
        if (dueDate.isBefore(today)) {
            val nextMonth = today.plusMonths(1)
            dueDate = nextMonth.withDayOfMonth(minOf(dueDayOfMonth, nextMonth.lengthOfMonth()))
        }
        return ChronoUnit.DAYS.between(today, dueDate).toInt()
    }

    fun getStatus(): BillStatus {
        if (!isActive) return BillStatus.INACTIVE
        val days = daysUntilDue()
        return when {
            days == Int.MAX_VALUE -> BillStatus.GREEN
            days < 0              -> BillStatus.RED
            days <= 3             -> BillStatus.YELLOW
            else                  -> BillStatus.GREEN
        }
    }

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
    GREEN, YELLOW, RED, INACTIVE
}

enum class RecurringBillStatus {
    PAID, OK, DUE_SOON, OVERDUE, INACTIVE
}
