package com.nexohogar.domain.model

import org.junit.Assert.*
import org.junit.Test

class BudgetTest {

    @Test
    fun `percentage is correct when budget is partially used`() {
        val budget = Budget(
            id = "b1", householdId = "hh-1", categoryId = "cat-1",
            categoryName = "Comida", amountClp = 100000L, spentClp = 50000L
        )
        assertEquals(50.0, budget.percentage, 0.01)
    }

    @Test
    fun `percentage is 100 when fully spent`() {
        val budget = Budget(
            id = "b2", householdId = "hh-1", categoryId = "cat-1",
            categoryName = "Comida", amountClp = 50000L, spentClp = 50000L
        )
        assertEquals(100.0, budget.percentage, 0.01)
    }

    @Test
    fun `percentage exceeds 100 when overspent`() {
        val budget = Budget(
            id = "b3", householdId = "hh-1", categoryId = "cat-1",
            categoryName = "Comida", amountClp = 50000L, spentClp = 75000L
        )
        assertEquals(150.0, budget.percentage, 0.01)
    }

    @Test
    fun `percentage is 0 when nothing spent`() {
        val budget = Budget(
            id = "b4", householdId = "hh-1", categoryId = "cat-1",
            categoryName = "Comida", amountClp = 100000L, spentClp = 0L
        )
        assertEquals(0.0, budget.percentage, 0.01)
    }

    @Test
    fun `percentage is 0 when amountClp is 0`() {
        val budget = Budget(
            id = "b5", householdId = "hh-1", categoryId = "cat-1",
            categoryName = "Comida", amountClp = 0L, spentClp = 50000L
        )
        assertEquals(0.0, budget.percentage, 0.01)
    }

    @Test
    fun `remaining is correct`() {
        val budget = Budget(
            id = "b6", householdId = "hh-1", categoryId = "cat-1",
            categoryName = "Comida", amountClp = 100000L, spentClp = 35000L
        )
        assertEquals(65000L, budget.remaining)
    }

    @Test
    fun `remaining is negative when overspent`() {
        val budget = Budget(
            id = "b7", householdId = "hh-1", categoryId = "cat-1",
            categoryName = "Comida", amountClp = 50000L, spentClp = 75000L
        )
        assertEquals(-25000L, budget.remaining)
    }

    @Test
    fun `default values are correct`() {
        val budget = Budget(
            id = "b8", householdId = "hh-1", categoryId = "cat-1",
            categoryName = "Comida", amountClp = 100000L
        )
        assertEquals(0L, budget.spentClp)
        assertEquals("monthly", budget.period)
        assertTrue(budget.isActive)
    }
}
