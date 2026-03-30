package com.nexohogar.data.mapper

import com.nexohogar.data.remote.dto.TransactionResponse
import org.junit.Assert.*
import org.junit.Test

class TransactionMappersTest {

    @Test
    fun `TransactionResponse toDomain maps all fields correctly`() {
        val response = TransactionResponse(
            id = "tx-1",
            description = "Compra supermercado",
            amountClp = 45000L,
            createdAt = "2026-03-30T12:00:00Z",
            accountId = "acc-1",
            householdId = "hh-1",
            type = "expense",
            createdByName = "Juan"
        )
        val transaction = response.toDomain()

        assertEquals("tx-1", transaction.id)
        assertEquals("Compra supermercado", transaction.description)
        assertEquals(45000L, transaction.amount)
        assertEquals("acc-1", transaction.accountId)
        assertEquals("2026-03-30T12:00:00Z", transaction.createdAt)
        assertEquals("expense", transaction.type)
        assertEquals("Juan", transaction.createdByName)
    }

    @Test
    fun `toDomain handles null description`() {
        val response = TransactionResponse(
            id = "tx-2",
            description = null,
            amountClp = 10000L,
            createdAt = "2026-03-30T12:00:00Z",
            accountId = "acc-1",
            householdId = "hh-1",
            type = "income"
        )
        val transaction = response.toDomain()
        assertNull(transaction.description)
    }

    @Test
    fun `toDomain handles null createdByName`() {
        val response = TransactionResponse(
            id = "tx-3",
            description = "Test",
            amountClp = 5000L,
            createdAt = "2026-03-30T12:00:00Z",
            accountId = "acc-1",
            householdId = "hh-1",
            type = "expense",
            createdByName = null
        )
        val transaction = response.toDomain()
        assertNull(transaction.createdByName)
    }

    @Test
    fun `List TransactionResponse toDomain preserves order`() {
        val responses = listOf(
            TransactionResponse(id = "tx-1", description = "A", amountClp = 100L, createdAt = "2026-01-01", accountId = "a1", householdId = "h1", type = "income"),
            TransactionResponse(id = "tx-2", description = "B", amountClp = 200L, createdAt = "2026-01-02", accountId = "a1", householdId = "h1", type = "expense"),
            TransactionResponse(id = "tx-3", description = "C", amountClp = 300L, createdAt = "2026-01-03", accountId = "a1", householdId = "h1", type = "income")
        )
        val transactions = responses.toDomain()

        assertEquals(3, transactions.size)
        assertEquals("tx-1", transactions[0].id)
        assertEquals("tx-2", transactions[1].id)
        assertEquals("tx-3", transactions[2].id)
        assertEquals(100L, transactions[0].amount)
        assertEquals(200L, transactions[1].amount)
    }

    @Test
    fun `toDomain maps amountClp to amount correctly`() {
        val response = TransactionResponse(
            id = "tx-neg",
            description = "Negative",
            amountClp = -15000L,
            createdAt = "2026-03-30T12:00:00Z",
            accountId = "acc-1",
            householdId = "hh-1",
            type = "expense"
        )
        assertEquals(-15000L, response.toDomain().amount)
    }
}
