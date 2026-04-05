package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.TransactionDetailApi
import com.nexohogar.data.remote.dto.UpdateTransactionRequest
import com.nexohogar.domain.model.TransactionDetail
import com.nexohogar.domain.repository.TransactionDetailRepository

class TransactionDetailRepositoryImpl(
    private val api: TransactionDetailApi
) : TransactionDetailRepository {

    override suspend fun getTransactionDetail(transactionId: String): AppResult<TransactionDetail> {
        return try {
            val txResponse = api.getTransactionDetail(idFilter = "eq.$transactionId")
            if (!txResponse.isSuccessful) {
                val errorBody = txResponse.errorBody()?.string() ?: "sin cuerpo"
                return AppResult.Error("Error ${txResponse.code()}: $errorBody")
            }
            val dto = txResponse.body()?.firstOrNull()
                ?: return AppResult.Error("Transacción no encontrada (id=$transactionId)")

            val fromAccountName = dto.fromAccountId?.let { accountId ->
                val r = api.getAccountName(idFilter = "eq.$accountId")
                if (r.isSuccessful) r.body()?.firstOrNull()?.name else null
            }

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
                    toAccountName   = toAccountName,
                    createdByName   = dto.createdByName,
                    createdAt       = dto.createdAt
                )
            )
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}")
        }
    }

    // Feature 1: Editar transacción
    override suspend fun updateTransaction(
        transactionId  : String,
        householdId    : String,
        amountClp      : Long,
        description    : String,
        transactionDate: String
    ): AppResult<Unit> {
        return try {
            val request = UpdateTransactionRequest(
                transactionId   = transactionId,
                householdId     = householdId,
                amountClp       = amountClp,
                description     = description,
                transactionDate = transactionDate
            )
            val response = api.updateTransaction(request)
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                AppResult.Error("Error al actualizar: $errorBody")
            }
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}")
        }
    }
}
