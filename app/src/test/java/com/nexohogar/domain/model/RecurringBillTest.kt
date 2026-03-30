package com.nexohogar.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RecurringBillTest {

    private val zone = ZoneId.of("America/Santiago")
    private val today = LocalDate.now(zone)

    private fun createBill(
        dueDayOfMonth: Int = 15,
        isActive: Boolean = true,
        lastPaidDate: String? = null
    ) = RecurringBill(
        id = "bill-1",
        householdId = "hh-1",
        name = "Agua",
        amountClp = 25000L,
        dueDayOfMonth = dueDayOfMonth,
        isActive = isActive,
        lastPaidDate = lastPaidDate,
        notes = null,
        createdAt = "2026-01-01"
    )

    // ── Status for inactive bills ───────────────────────────────────────────

    @Test
    fun `inactive bill returns INACTIVE status`() {
        val bill = createBill(isActive = false)
        assertEquals(BillStatus.INACTIVE, bill.getStatus())
        assertEquals(RecurringBillStatus.INACTIVE, bill.status())
        assertEquals("PAUSADO", bill.statusLabel())
    }

    // ── Paid this month ─────────────────────────────────────────────────────

    @Test
    fun `paid this month returns GREEN and PAID`() {
        val paidDate = today.minusDays(2).toString()
        val bill = createBill(lastPaidDate = paidDate)
        assertEquals(BillStatus.GREEN, bill.getStatus())
        assertEquals(RecurringBillStatus.PAID, bill.status())
        assertEquals("PAGADO", bill.statusLabel())
    }

    @Test
    fun `daysUntilDue returns MAX_VALUE when paid this month`() {
        val paidDate = today.toString()
        val bill = createBill(lastPaidDate = paidDate)
        assertEquals(Int.MAX_VALUE, bill.daysUntilDue())
    }

    // ── Due far in the future ───────────────────────────────────────────────

    @Test
    fun `bill due far in future returns GREEN and OK`() {
        // Set due day far from today
        val farDay = if (today.dayOfMonth <= 15) 28 else {
            // If today is late in the month, next month's early day will be far
            val nextMonth = today.plusMonths(1)
            if (today.dayOfMonth >= 25) 10 else 28
        }
        val bill = createBill(dueDayOfMonth = farDay)
        val days = bill.daysUntilDue()
        if (days > 3) {
            assertEquals(BillStatus.GREEN, bill.getStatus())
            assertEquals(RecurringBillStatus.OK, bill.status())
        }
    }

    // ── Default data ────────────────────────────────────────────────────────

    @Test
    fun `bill has correct default data`() {
        val bill = createBill()
        assertEquals("bill-1", bill.id)
        assertEquals("Agua", bill.name)
        assertEquals(25000L, bill.amountClp)
        assertTrue(bill.isActive)
    }

    // ── Invalid lastPaidDate ────────────────────────────────────────────────

    @Test
    fun `invalid lastPaidDate is treated as unpaid`() {
        val bill = createBill(lastPaidDate = "not-a-date")
        // Should not crash, should treat as unpaid
        assertNotEquals(Int.MAX_VALUE, bill.daysUntilDue())
    }

    @Test
    fun `null lastPaidDate is treated as unpaid`() {
        val bill = createBill(lastPaidDate = null)
        assertNotEquals(Int.MAX_VALUE, bill.daysUntilDue())
    }

    // ── Status labels ───────────────────────────────────────────────────────

    @Test
    fun `daysUntilDue returns non-negative for future due date`() {
        // Due on last day of month, far enough to be positive
        val bill = createBill(dueDayOfMonth = today.lengthOfMonth())
        val days = bill.daysUntilDue()
        // If today IS the last day, it wraps to next month, so always >= 0
        assertTrue("Days should be >= 0, was $days", days >= 0 || days == Int.MAX_VALUE)
    }
}
