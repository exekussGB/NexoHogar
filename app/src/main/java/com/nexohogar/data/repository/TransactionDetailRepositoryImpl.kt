package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.TransactionDetailApi
import com.nexohogar.domain.model.TransactionDetail
import com.nexohogar.domain.repository.TransactionDetailRepository

/**
 * AuthInterceptor ya inyecta el token, no necesitamos SessionManager aquí.
 * Patrón de dos llamadas para evitar la ambigüedad de PostgREST cuando
 * la tabla tiene dos FK apuntando a la misma tabla padre (accounts).
 */
class TransactionDetailRepositoryImpl(
    private val api: TransactionDetailApi
) : TransactionDetailRepository {

    override suspend fun getTransactionDetail(transactionId: String): AppResult<TransactionDetail> {
        return try {
            // 1. Obtener la transacción
            val txResponse = api.getTransactionDetail(idFilter = "eq.$transactionId")
            if (!txResponse.isSuccessful) {
                val errorBody = txResponse.errorBody()?.string() ?: "sin cuerpo"
                return AppResult.Error("Error ${txResponse.code()}: $errorBody")
            }
            val dto = txResponse.body()?.firstOrNull()
                ?: return AppResult.Error("Transacción no encontrada (id=$transactionId)")

            // 2. Nombre de cuenta origen (si existe)
            val fromAccountName = dto.fromAccountId?.let { accountId ->
                val r = api.getAccountName(idFilter = "eq.$accountId")
                if (r.isSuccessful) r.body()?.firstOrNull()?.name else null
            }

            // 3. Nombre de cuenta destino (solo para transferencias)
            val toAccountName = dto.toAccountId?.let { accountId ->
                val r = api.getAccountName(idFilter = "eq.$accountId")
                if (r.isSuccessful) r.body()?.firstOrNull()?.name else null
            }

            AppResult.Success(
                TransactionDetail(
                    id              = dto.id,
                    type            = dto.type,
                    description     = dto.description,
                    transactionDate = dto.transactionDate,
                    amountClp       = dto.amountClp ?: 0L,
                    status          = dto.status,
                    fromAccountId   = dto.fromAccountId,
                    toAccountId     = dto.toAccountId,
                    fromAccountName = fromAccountName,
                    toAccountName   = toAccountName
                )
            )
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}")
        }
    }
}