package com.nexohogar.data.mapper

import com.nexohogar.data.remote.dto.HouseholdResponse
import com.nexohogar.data.remote.dto.LoginResponse
import com.nexohogar.data.remote.dto.UserResponse
import org.junit.Assert.*
import org.junit.Test

class AuthMappersTest {

    // ── LoginResponse.toDomain() ────────────────────────────────────────────

    @Test
    fun `toDomain returns UserSession when all fields present`() {
        val response = LoginResponse(
            accessToken = "access-123",
            refreshToken = "refresh-456",
            expiresIn = 3600L,
            tokenType = "bearer",
            user = UserResponse(id = "user-1", email = "test@nexo.cl")
        )

        val session = response.toDomain()

        assertNotNull(session)
        assertEquals("access-123", session!!.accessToken)
        assertEquals("refresh-456", session.refreshToken)
        assertEquals("user-1", session.userId)
        assertEquals("test@nexo.cl", session.email)
        assertTrue(session.expiresAt > System.currentTimeMillis())
    }

    @Test
    fun `toDomain returns null when accessToken is null`() {
        val response = LoginResponse(
            accessToken = null,
            refreshToken = "refresh-456",
            expiresIn = 3600L,
            tokenType = "bearer",
            user = UserResponse(id = "user-1", email = "test@nexo.cl")
        )
        assertNull(response.toDomain())
    }

    @Test
    fun `toDomain returns null when refreshToken is null`() {
        val response = LoginResponse(
            accessToken = "access-123",
            refreshToken = null,
            expiresIn = 3600L,
            tokenType = "bearer",
            user = UserResponse(id = "user-1", email = "test@nexo.cl")
        )
        assertNull(response.toDomain())
    }

    @Test
    fun `toDomain returns null when user is null`() {
        val response = LoginResponse(
            accessToken = "access-123",
            refreshToken = "refresh-456",
            expiresIn = 3600L,
            tokenType = "bearer",
            user = null
        )
        assertNull(response.toDomain())
    }

    @Test
    fun `toDomain returns null when user id is null`() {
        val response = LoginResponse(
            accessToken = "access-123",
            refreshToken = "refresh-456",
            expiresIn = 3600L,
            tokenType = "bearer",
            user = UserResponse(id = null, email = "test@nexo.cl")
        )
        assertNull(response.toDomain())
    }

    @Test
    fun `toDomain returns null when user email is null`() {
        val response = LoginResponse(
            accessToken = "access-123",
            refreshToken = "refresh-456",
            expiresIn = 3600L,
            tokenType = "bearer",
            user = UserResponse(id = "user-1", email = null)
        )
        assertNull(response.toDomain())
    }

    @Test
    fun `toDomain uses default expiresIn of 3600 when null`() {
        val before = System.currentTimeMillis()
        val response = LoginResponse(
            accessToken = "access-123",
            refreshToken = "refresh-456",
            expiresIn = null,
            tokenType = "bearer",
            user = UserResponse(id = "user-1", email = "test@nexo.cl")
        )
        val session = response.toDomain()!!
        val after = System.currentTimeMillis()

        // expiresAt should be approximately now + 3600 seconds
        assertTrue(session.expiresAt >= before + 3_600_000)
        assertTrue(session.expiresAt <= after + 3_600_000)
    }

    // ── HouseholdResponse.toDomain() ────────────────────────────────────────

    @Test
    fun `HouseholdResponse toDomain maps correctly`() {
        val response = HouseholdResponse(
            id = "hh-1",
            name = "Mi Hogar",
            description = "Hogar de prueba"
        )
        val household = response.toDomain()

        assertEquals("hh-1", household.id)
        assertEquals("Mi Hogar", household.name)
        assertEquals("Hogar de prueba", household.description)
    }

    @Test
    fun `HouseholdResponse toDomain uses empty string for null description`() {
        val response = HouseholdResponse(
            id = "hh-2",
            name = "Hogar 2",
            description = null
        )
        val household = response.toDomain()
        assertEquals("", household.description)
    }

    @Test
    fun `List HouseholdResponse toDomain maps all items`() {
        val responses = listOf(
            HouseholdResponse(id = "1", name = "H1", description = null),
            HouseholdResponse(id = "2", name = "H2", description = "desc")
        )
        val households = responses.toDomain()

        assertEquals(2, households.size)
        assertEquals("1", households[0].id)
        assertEquals("2", households[1].id)
    }
}
