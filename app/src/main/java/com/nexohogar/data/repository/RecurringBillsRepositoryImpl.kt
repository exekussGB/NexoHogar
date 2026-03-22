package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.RecurringBillsApi
import com.nexohogar.data.remote.dto.CreateRecurringBillRequest
import com.nexohogar.data.remote.dto.ToggleActiveRequest
import com.nexohogar.data.remote.dto.UpdateLastPaidRequest
import com.nexohogar.domain.model.RecurringBill
import com.nexohogar.domain.repository.RecurringBillsRepository

class RecurringBillsRepositoryImpl(
    private val api: RecurringBillsApi
) : RecurringBillsRepository {

    override suspend fun getRecurringBills(householdId: String): AppResult<List<RecurringBill>> {
        return try {
            val response = api.getRecurringBills(householdIdFilter = "eq.$householdId")
            if (response.isSuccessful) {
                AppResult.Success(response.body()?.map { it.toDomain() } ?: emptyList())
            } else {
                AppResult.Error("Error al obtener cuentas recurrentes: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun createRecurringBill(
        householdId: String,
        name: String,
        amountClp: Long,
        dueDayOfMonth: Int,
        notes: String?
    ): AppResult<RecurringBill> {
        return try {
            val request = CreateRecurringBillRequest(
                householdId   = householdId,
                name          = name,
                amountClp     = amountClp,
                dueDayOfMonth = dueDayOfMonth,
                notes         = notes
            )
            val response = api.createRecurringBill(request = request)
            if (response.isSuccessful) {
                val created = response.body()?.firstOrNull()
                    ?: return AppResult.Error("No se recibió respuesta del servidor")
                AppResult.Success(created.toDomain())
            } else {
                AppResult.Error("Error al crear cuenta recurrente: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun markAsPaid(billId: String, paidDate: String): AppResult<RecurringBill> {
        return try {
            val response = api.markAsPaid(
                idFilter = "eq.$billId",
                request  = UpdateLastPaidRequest(lastPaidDate = paidDate)
            )
            if (response.isSuccessful) {
                val updated = response.body()?.firstOrNull()
                    ?: return AppResult.Error("No se recibió respuesta del servidor")
                AppResult.Success(updated.toDomain())
            } else {
                AppResult.Error("Error al marcar como pagado: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun toggleActive(billId: String, isActive: Boolean): AppResult<RecurringBill> {
        return try {
            val response = api.toggleActive(
                idFilter = "eq.$billId",
                request  = ToggleActiveRequest(isActive = isActive)
            )
            if (response.isSuccessful) {
                val updated = response.body()?.firstOrNull()
                    ?: return AppResult.Error("No se recibió respuesta del servidor")
                AppResult.Success(updated.toDomain())
            } else {
                AppResult.Error("Error al actualizar cuenta: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun deleteRecurringBill(billId: String): AppResult<Unit> {
        return try {
            val response = api.deleteRecurringBill(idFilter = "eq.$billId")
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error("Error al eliminar: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }
}