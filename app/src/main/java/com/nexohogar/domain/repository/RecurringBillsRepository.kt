package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.remote.dto.RecurringBillPaymentDto
import com.nexohogar.data.remote.dto.RecurringBillWithStatusDto
import com.nexohogar.data.remote.dto.RecurringSummaryDto
import com.nexohogar.domain.model.RecurringBill

interface RecurringBillsRepository {
    suspend fun getRecurringBills(householdId: String): AppResult<List<RecurringBill>>
    suspend fun createRecurringBill(
        householdId: String,
        name: String,
        amountClp: Long,
        dueDayOfMonth: Int,
        notes: String?,
        totalInstallments: Int? = null,
        paidInstallments: Int = 0
    ): AppResult<RecurringBill>
    suspend fun markAsPaid(billId: String, paidDate: String): AppResult<RecurringBill>
    suspend fun toggleActive(billId: String, isActive: Boolean): AppResult<RecurringBill>
    suspend fun deleteRecurringBill(billId: String): AppResult<Unit>

    // ── NUEVOS: con integración contable ─────────────────────────────────
    suspend fun payBill(
        billId: String,
        householdId: String,
        amountClp: Long,
        accountId: String? = null,
        notes: String? = null
    ): AppResult<Boolean>

    suspend fun getRecurringSummary(householdId: String): AppResult<RecurringSummaryDto>
    suspend fun getBillsWithStatus(householdId: String): AppResult<List<RecurringBillWithStatusDto>>
    suspend fun getBillHistory(billId: String, householdId: String): AppResult<List<RecurringBillPaymentDto>>
}
