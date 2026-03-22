package com.nexohogar.domain.model

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
    /**
     * Calcula cuántos días faltan para el próximo vencimiento.
     * Retorna un número negativo si ya venció este mes sin pagar.
     */
    fun daysUntilDue(): Int {
        val today = java.util.Calendar.getInstance()
        val todayDay = today.get(java.util.Calendar.DAY_OF_MONTH)
        val todayMonth = today.get(java.util.Calendar.MONTH)
        val todayYear = today.get(java.util.Calendar.YEAR)

        // Verificar si ya se pagó este mes
        if (!lastPaidDate.isNullOrBlank()) {
            try {
                val parts = lastPaidDate.split("-")
                if (parts.size >= 3) {
                    val paidYear = parts[0].toInt()
                    val paidMonth = parts[1].toInt() - 1 // Calendar.MONTH es 0-indexed
                    if (paidYear == todayYear && paidMonth == todayMonth) {
                        // Ya pagado este mes
                        return Int.MAX_VALUE
                    }
                }
            } catch (_: Exception) { }
        }

        return dueDayOfMonth - todayDay
    }

    /** Estado visual de la cuenta. */
    fun status(): RecurringBillStatus {
        if (!isActive) return RecurringBillStatus.INACTIVE
        val days = daysUntilDue()
        return when {
            days == Int.MAX_VALUE -> RecurringBillStatus.PAID
            days < 0              -> RecurringBillStatus.OVERDUE
            days <= 5             -> RecurringBillStatus.DUE_SOON
            else                  -> RecurringBillStatus.OK
        }
    }
}

enum class RecurringBillStatus {
    OK,        // más de 5 días
    DUE_SOON,  // vence en 5 días o menos
    OVERDUE,   // venció este mes sin pagar
    PAID,      // ya pagado este mes
    INACTIVE   // cuenta desactivada
}
