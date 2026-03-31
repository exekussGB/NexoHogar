package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.RecurringBillsApi
import com.nexohogar.data.remote.dto.CreateRecurringBillRequest
import com.nexohogar.data.remote.dto.PayRecurringBillRequest
import com.nexohogar.data.remote.dto.RecurringBillPaymentDto
import com.nexohogar.data.remote.dto.RecurringBillWithStatusDto
import com.nexohogar.data.remote.dto.RecurringSummaryDto
import com.nexohogar.data.remote.dto.ToggleActiveRequest
import com.nexohogar.data.remote.dto.UpdateLastPaidRequest
import com.nexohogar.data.remote.dto.UpdateRecurringBillRequest
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
        notes: String?,
        totalInstallments: Int?
    ): AppResult<RecurringBill> {
        return try {
            val request = CreateRecurringBillRequest(
                householdId       = householdId,
                name              = name,
                amountClp         = amountClp,
                dueDayOfMonth     = dueDayOfMonth,
                notes             = notes,
                totalInstallments = totalInstallments
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

    override suspend fun updateRecurringBill(
        billId: String,
        name: String?,
        amountClp: Long?,
        dueDayOfMonth: Int?,
        notes: String?,
        isActive: Boolean?,
        totalInstallments: Int?
    ): AppResult<RecurringBill> {
        return try {
            val request = UpdateRecurringBillRequest(
                name              = name,
                amountClp         = amountClp,
                dueDayOfMonth     = dueDayOfMonth,
                notes             = notes,
                isActive          = isActive,
                totalInstallments = totalInstallments
            )
            val response = api.updateRecurringBill(
                idFilter = "eq.$billId",
                request  = request
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

    // ── Pagar con integración contable ───────────────────────────────

    override suspend fun payBill(
        billId: String,
        householdId: String,
        amountClp: Long,
        accountId: String?,
        notes: String?
    ): AppResult<Boolean> {
        return try {
            val response = api.payRecurringBill(
                PayRecurringBillRequest(
                    billId      = billId,
                    householdId = householdId,
                    amountClp   = amountClp,
                    accountId   = accountId,
                    notes       = notes
                )
            )
            if (response.isSuccessful) {
                AppResult.Success(true)
            } else {
                AppResult.Error("Error al registrar pago: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun getRecurringSummary(householdId: String): AppResult<RecurringSummaryDto> {
        return try {
            val response = api.getRecurringSummary(mapOf("p_household_id" to householdId))
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return AppResult.Error("Sin datos de resumen")
                AppResult.Success(body)
            } else {
                AppResult.Error("Error al obtener resumen: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun getBillsWithStatus(householdId: String): AppResult<List<RecurringBillWithStatusDto>> {
        return try {
            val response = api.getRecurringBillsWithStatus(mapOf("p_household_id" to householdId))
            if (response.isSuccessful) {
                AppResult.Success(response.body() ?: emptyList())
            } else {
                AppResult.Error("Error al obtener estado: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun getBillHistory(
        billId: String,
        householdId: String
    ): AppResult<List<RecurringBillPaymentDto>> {
        return try {
            val response = api.getRecurringBillHistory(
                mapOf("p_bill_id" to billId, "p_household_id" to householdId)
            )
            if (response.isSuccessful) {
                AppResult.Success(response.body() ?: emptyList())
            } else {
                AppResult.Error("Error al obtener historial: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }
}
