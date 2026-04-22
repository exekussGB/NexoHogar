package com.nexohogar.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.core.util.AppLogger
import com.nexohogar.data.remote.dto.CreateTransactionRequest

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val transactionDao = ServiceLocator.database.transactionDao()
        val api = ServiceLocator.transactionsApi
        
        val pending = transactionDao.getPendingSyncTransactions()
        if (pending.isEmpty()) return Result.success()

        AppLogger.d("SyncWorker", "Iniciando sincronización de ${pending.size} transacciones")

        var hasError = false
        pending.forEach { entity ->
            // No sincronizar si es un hogar de invitado (ID empieza con guest_)
            if (entity.householdId.startsWith("guest_")) {
                return@forEach
            }

            try {
                val request = CreateTransactionRequest(
                    pHouseholdId = entity.householdId,
                    pType = entity.type,
                    pAccountId = entity.accountId,
                    pAmountClp = entity.amountClp,
                    pCategoryId = entity.categoryId ?: "",
                    pDescription = entity.description,
                    pTransactionDate = entity.transactionDate
                )
                
                val response = api.createTransaction(request)
                if (response.isSuccessful) {
                    transactionDao.markAsSynced(entity.id)
                } else {
                    hasError = true
                }
            } catch (e: Exception) {
                AppLogger.e("SyncWorker", "Error sincronizando ${entity.id}", e)
                hasError = true
            }
        }

        return if (hasError) Result.retry() else Result.success()
    }
}
