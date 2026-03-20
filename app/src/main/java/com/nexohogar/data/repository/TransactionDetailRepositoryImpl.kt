package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.TransactionDetailApi
import com.nexohogar.domain.model.TransactionDetail
import com.nexohogar.domain.repository.TransactionDetailRepository

class TransactionDetailRepositoryImpl(
    private val api: TransactionDetailApi,
    private val sessionManager: SessionManager
) : TransactionDetailRepository {

    override suspend fun getTransactionDetail(transactionId: String): AppResult<TransactionDetail> {
        return try {
            val token = sessionManager.fetchAuthToken()
                ?: return AppResult.Error("No hay sesión activa")
            val bearerToken = "Bearer $token"

            // 1. Obtener la transacción
            val transactionResponse = api.getTransactionDetail(
                token = bearerToken,
                idFilter = "eq.$transactionId"
            )
            if (!transactionResponse.isSuccessful) {
                return AppResult.Error("Error al obtener transacción: ${transactionResponse.code()}")
            }

            val dto = transactionResponse.body()?.firstOrNull()
                ?: return AppResult.Error("Transacción no encontrada")

            // 2. Obtener nombre de cuenta origen (si existe)
            val fromAccountName = dto.fromAccountId?.let { accountId ->
                val accResponse = api.getAccountName(
                    token = bearerToken,
                    idFilter = "eq.$accountId"
                )
                if (accResponse.isSuccessful) accResponse.body()?.firstOrNull()?.name
                else null
            }

            // 3. Obtener nombre de cuenta destino (para transferencias)
            val toAccountName = dto.toAccountId?.let { accountId ->
                val accResponse = api.getAccountName(
                    token = bearerToken,
                    idFilter = "eq.$accountId"
                )
                if (accResponse.isSuccessful) accResponse.body()?.firstOrNull()?.name
                else null
            }

            AppResult.Success(
                TransactionDetail(
                    id = dto.id,
                    type = dto.type,
                    description = dto.description,
                    transactionDate = dto.transactionDate,
                    amountClp = dto.amountClp ?: 0L,
                    status = dto.status,
                    fromAccountId = dto.fromAccountId,
                    toAccountId = dto.toAccountId,
                    fromAccountName = fromAccountName,
                    toAccountName = toAccountName
                )
            )
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}")
        }
    }
}