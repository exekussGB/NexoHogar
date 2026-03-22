package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.RecurringBill

interface RecurringBillsRepository {
    suspend fun getRecurringBills(householdId: String): AppResult<List<RecurringBill>>
    suspend fun createRecurringBill(
        householdId: String,
        name: String,
        amountClp: Long,
        dueDayOfMonth: Int,
        notes: String?
    ): AppResult<RecurringBill>
    suspend fun markAsPaid(billId: String, paidDate: String): AppResult<RecurringBill>
    suspend fun toggleActive(billId: String, isActive: Boolean): AppResult<RecurringBill>
    suspend fun deleteRecurringBill(billId: String): AppResult<Unit>
}
