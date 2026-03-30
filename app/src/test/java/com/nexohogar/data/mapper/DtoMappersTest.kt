package com.nexohogar.data.mapper

import com.nexohogar.data.remote.dto.AccountBalanceDto
import com.nexohogar.data.remote.dto.AccountDto
import com.nexohogar.data.remote.dto.DashboardDto
import com.nexohogar.data.remote.dto.DualDashboardDto
import com.nexohogar.data.remote.dto.PersonalDashboardDto
import com.nexohogar.data.remote.dto.toDomain
import com.nexohogar.data.remote.dto.toSection
import org.junit.Assert.*
import org.junit.Test

class DtoMappersTest {

    // ── DashboardDto ────────────────────────────────────────────────────────

    @Test
    fun `DashboardDto toDomain maps all fields`() {
        val dto = DashboardDto(
            householdId = "hh-1",
            totalBalance = 500000.0,
            totalIncome = 1000000.0,
            totalExpense = 500000.0,
            accountsCount = 3,
            transactionsCount = 42
        )
        val summary = dto.toDomain()

        assertEquals("hh-1", summary.householdId)
        assertEquals(500000.0, summary.totalBalance, 0.01)
        assertEquals(1000000.0, summary.totalIncome, 0.01)
        assertEquals(500000.0, summary.totalExpense, 0.01)
        assertEquals(3, summary.accountsCount)
        assertEquals(42, summary.transactionsCount)
    }

    // ── DualDashboardDto ────────────────────────────────────────────────────

    @Test
    fun `DualDashboardDto toSection maps correctly`() {
        val dto = DualDashboardDto(
            section = "shared",
            totalBalance = 100000.0,
            totalIncome = 200000.0,
            totalExpense = 100000.0,
            accountsCount = 2
        )
        val section = dto.toSection()

        assertEquals(100000.0, section.totalBalance, 0.01)
        assertEquals(200000.0, section.totalIncome, 0.01)
        assertEquals(100000.0, section.totalExpense, 0.01)
        assertEquals(2, section.accountsCount)
    }

    // ── PersonalDashboardDto ────────────────────────────────────────────────

    @Test
    fun `PersonalDashboardDto toDomain maps correctly`() {
        val dto = PersonalDashboardDto(
            totalBalance = 75000.0,
            totalIncome = 150000.0,
            totalExpense = 75000.0,
            accountsCount = 1,
            transactionsCount = 10
        )
        val summary = dto.toDomain()

        assertEquals(75000.0, summary.totalBalance, 0.01)
        assertEquals(150000.0, summary.totalIncome, 0.01)
        assertEquals(75000.0, summary.totalExpense, 0.01)
        assertEquals(1, summary.accountsCount)
        assertEquals(10, summary.transactionsCount)
    }

    // ── AccountDto ──────────────────────────────────────────────────────────

    @Test
    fun `AccountDto toDomain maps all fields`() {
        val dto = AccountDto(
            id = "acc-1",
            name = "Cuenta Corriente",
            accountType = "ASSET",
            accountSubtype = "checking",
            balance = 150000.0,
            householdId = "hh-1",
            isShared = true,
            ownerUserId = null,
            isSystem = false
        )
        val account = dto.toDomain()

        assertEquals("acc-1", account.id)
        assertEquals("Cuenta Corriente", account.name)
        assertEquals("ASSET", account.type)
        assertEquals("checking", account.subtype)
        assertEquals(150000L, account.balance)
        assertEquals("hh-1", account.householdId)
        assertTrue(account.isShared)
        assertNull(account.ownerUserId)
    }

    @Test
    fun `AccountDto toDomain uses defaults for null fields`() {
        val dto = AccountDto(
            id = "acc-2",
            name = "Ahorro",
            accountType = null,
            accountSubtype = null,
            balance = null,
            householdId = "hh-1",
            isShared = null,
            ownerUserId = "user-1",
            isSystem = null
        )
        val account = dto.toDomain()

        assertEquals("ASSET", account.type)
        assertEquals("other", account.subtype)
        assertEquals(0L, account.balance)
        assertTrue(account.isShared) // default
        assertEquals("user-1", account.ownerUserId)
    }

    @Test
    fun `AccountDto toDomain truncates decimals in balance`() {
        val dto = AccountDto(
            id = "acc-3", name = "Test", accountType = "ASSET",
            accountSubtype = "other", balance = 99999.99,
            householdId = "hh-1", isShared = true, ownerUserId = null, isSystem = false
        )
        assertEquals(99999L, dto.toDomain().balance)
    }

    @Test
    fun `List AccountDto toDomain maps all items`() {
        val dtos = listOf(
            AccountDto("a1", "C1", "ASSET", "checking", 100.0, "hh-1", true, null, false),
            AccountDto("a2", "C2", "LIABILITY", "credit", 200.0, "hh-1", false, "u1", false)
        )
        val accounts = dtos.toDomain()

        assertEquals(2, accounts.size)
        assertEquals("a1", accounts[0].id)
        assertEquals("a2", accounts[1].id)
        assertFalse(accounts[1].isShared)
    }

    // ── AccountBalanceDto ───────────────────────────────────────────────────

    @Test
    fun `AccountBalanceDto toDomain maps correctly`() {
        val dto = AccountBalanceDto(
            accountId = "acc-1",
            accountName = "Corriente",
            accountType = "ASSET",
            movementBalance = 250000.0,
            isShared = true
        )
        val balance = dto.toDomain()

        assertEquals("acc-1", balance.accountId)
        assertEquals("Corriente", balance.accountName)
        assertEquals("ASSET", balance.accountType)
        assertEquals(250000L, balance.movementBalance)
        assertTrue(balance.isShared)
    }

    @Test
    fun `AccountBalanceDto toDomain defaults isShared to true`() {
        val dto = AccountBalanceDto(
            accountId = "acc-2",
            accountName = "Personal",
            accountType = "ASSET",
            movementBalance = 50000.0,
            isShared = null
        )
        assertTrue(dto.toDomain().isShared)
    }

    @Test
    fun `List AccountBalanceDto toDomain maps all`() {
        val dtos = listOf(
            AccountBalanceDto("a1", "C1", "ASSET", 100.0, true),
            AccountBalanceDto("a2", "C2", "LIABILITY", 200.0, false)
        )
        val balances = dtos.toDomain()

        assertEquals(2, balances.size)
        assertFalse(balances[1].isShared)
    }
}
