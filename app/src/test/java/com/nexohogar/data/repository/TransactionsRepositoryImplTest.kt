package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.local.room.dao.AccountDao
import com.nexohogar.data.local.room.dao.TransactionDao
import com.nexohogar.data.network.AccountsApi
import com.nexohogar.data.network.TransactionsApi
import com.nexohogar.data.remote.dto.TransactionResponse
import io.mockk.*
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class TransactionsRepositoryImplTest {

    private lateinit var api: TransactionsApi
    private lateinit var accountsApi: AccountsApi
    private lateinit var sessionManager: SessionManager
    private lateinit var transactionDao: TransactionDao
    private lateinit var accountDao: AccountDao
    private lateinit var repository: TransactionsRepositoryImpl

    private val fakeTransactions = listOf(
        TransactionResponse(
            id = "tx-1",
            description = "Supermercado",
            amountClp = 45000L,
            createdAt = "2026-03-30T12:00:00Z",
            accountId = "acc-1",
            householdId = "hh-1",
            type = "expense",
            createdByName = "Juan"
        ),
        TransactionResponse(
            id = "tx-2",
            description = "Sueldo",
            amountClp = 1500000L,
            createdAt = "2026-03-01T00:00:00Z",
            accountId = "acc-1",
            householdId = "hh-1",
            type = "income",
            createdByName = "Sistema"
        )
    )

    @Before
    fun setUp() {
        api = mockk()
        accountsApi = mockk()
        sessionManager = mockk(relaxed = true)
        transactionDao = mockk(relaxed = true)
        accountDao = mockk(relaxed = true)
        repository = TransactionsRepositoryImpl(api, accountsApi, sessionManager, transactionDao, accountDao)
    }

    @Test
    fun `getTransactions success returns mapped sorted domain list`() = runTest {
        coEvery { api.getTransactions(any(), any()) } returns Response.success(fakeTransactions)

        val result = repository.getTransactions("hh-1")

        assertTrue(result is AppResult.Success)
        val transactions = (result as AppResult.Success).data
        assertEquals(2, transactions.size)
        // Should be sorted by createdAt descending
        assertEquals("tx-1", transactions[0].id)  // 2026-03-30 > 2026-03-01
        assertEquals("tx-2", transactions[1].id)
        assertEquals(45000L, transactions[0].amount)
        assertEquals("expense", transactions[0].type)
    }

    @Test
    fun `getTransactions API error returns Error`() = runTest {
        coEvery { api.getTransactions(any(), any()) } returns Response.error(
            500, "Internal Server Error".toResponseBody(null)
        )

        val result = repository.getTransactions("hh-1")

        assertTrue(result is AppResult.Error)
        assertEquals("Error cargando transacciones", (result as AppResult.Error).message)
    }

    @Test
    fun `getTransactions network exception returns Error`() = runTest {
        coEvery { api.getTransactions(any(), any()) } throws java.io.IOException("Connection reset")

        val result = repository.getTransactions("hh-1")

        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `getTransactions null body returns empty list`() = runTest {
        coEvery { api.getTransactions(any(), any()) } returns Response.success(null)

        val result = repository.getTransactions("hh-1")

        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).data.isEmpty())
    }

    @Test
    fun `getTransactions passes correct filter`() = runTest {
        coEvery { api.getTransactions(any(), any()) } returns Response.success(emptyList())

        repository.getTransactions("my-household-123")

        coVerify { api.getTransactions("eq.my-household-123", any()) }
    }

    @Test
    fun `getTransactionsByAccount filters by accountId`() = runTest {
        coEvery { api.getTransactions(any(), any()) } returns Response.success(fakeTransactions)

        val result = repository.getTransactionsByAccount("hh-1", "acc-1", limit = 1)

        assertTrue(result is AppResult.Success)
        val transactions = (result as AppResult.Success).data
        assertEquals(1, transactions.size)  // limited to 1
    }

    @Test
    fun `getTransactionsByAccount with non-matching account returns empty`() = runTest {
        coEvery { api.getTransactions(any(), any()) } returns Response.success(fakeTransactions)

        val result = repository.getTransactionsByAccount("hh-1", "non-existent-acc")

        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).data.isEmpty())
    }

    @Test
    fun `createTransaction success returns Success`() = runTest {
        coEvery { api.createTransaction(any()) } returns Response.success(Unit)

        val result = repository.createTransaction(mockk(relaxed = true))

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `createTransaction failure returns Error`() = runTest {
        coEvery { api.createTransaction(any()) } returns Response.error(
            400, "bad request".toResponseBody(null)
        )

        val result = repository.createTransaction(mockk(relaxed = true))

        assertTrue(result is AppResult.Error)
    }
}
