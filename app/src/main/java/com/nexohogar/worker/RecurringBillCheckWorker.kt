package com.nexohogar.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Worker que consulta facturas próximas a vencer y emite notificación con acción rápida.
 * Referencia: nexohogar_report.md — Sección 1.4
 *
 * Notifica 2 días antes del vencimiento:
 * "⏰ Internet Movistar vence en 2 días ($29.990). ¿Pagaste?"
 * → Acción rápida: "Ya pagué" (marca como pagada directamente)
 *
 * TODO: Implementar la lógica completa conectando con RecurringBillsRepository
 */
class RecurringBillCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // TODO: Implementar
        // 1. Obtener facturas recurrentes del mes actual
        // 2. Filtrar las que vencen en 2 días
        // 3. Para cada una, emitir notificación con acción "Ya pagué"
        return Result.success()
    }

    companion object {
        const val TAG = "RecurringBillCheck"
    }
}
