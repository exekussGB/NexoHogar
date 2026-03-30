package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.TransactionDetailApi
import com.nexohogar.domain.model.TransactionDetail
import com.nexohogar.domain.repository.TransactionDetailRepository

/**
 * AuthInterceptor ya inyecta el token, no necesitamos SessionManager aquí.
 *
 * ✅ SIMPLIFICADO: Ahora usa v_transactions_with_user que ya trae created_by_name.
 *    Se mantiene la resolución de nombres de cuenta por separado porque
 *    la vista no incluye join con accounts (tiene dos FK).
 */
class TransactionDetailRepositoryImpl(
    private val api: TransactionDetailApi
) : TransactionDetailRepository {


    override suspend fun getTransactionDetail(transactionId: String): AppResult<TransactionDetail> {
        return try {
            // 1. Obtener la transacción (ahora desde v_transactions_with_user)
            val txResponse = api.getTransactionDetail(idFilter = "eq.$transactionId")
            if (!txResponse.isSuccessful) {
                val errorBody = txResponse.errorBody()?.string() ?: "sin cuerpo"
                return AppResult.Error("Error ${txResponse.code()}: $errorBody")
            }
            val dto = txResponse.body()?.firstOrNull()
                ?: return AppResult.Error("Transacción no encontrada (id=$transactionId)")

            // 2. Nombre de cuenta origen (si existe)
            //    ✅ FIX: ahora fromAccountId mapea correctamente a account_id
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
                    toAccountName   = toAccountName,
                    // ✅ NUEVO: nombre del usuario ya viene resuelto de la vista
                    createdByName   = dto.createdByName,
                    // ✅ NUEVO: timestamp completo para fecha + hora
                    createdAt       = dto.createdAt
                )
            )
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}")
        }
    }
    override suspend fun getTransactionsByAccount(
        householdId: String,
        accountId: String,
        limit: Int
    ): AppResult<List<Transaction>> {
        return try {
            val response = api.getTransactionsByAccount(
                householdIdFilter = "eq.$householdId",
                accountIdFilter = "eq.$accountId",
                limit = limit,
                order = "transaction_date.desc"
            )
            if (response.isSuccessful) {
                val transactions = response.body()?.map { it.toDomain() } ?: emptyList()
                AppResult.Success(transactions)
            } else {
                AppResult.Error("Error ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}")
        }
    }
}

