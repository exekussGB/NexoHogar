package com.nexohogar.domain.model

import org.junit.Assert.*
import org.junit.Test

class HouseholdMemberTest {

    @Test
    fun `id returns userId`() {
        val member = HouseholdMember(userId = "user-123", role = "user", joinedAt = null)
        assertEquals("user-123", member.id)
    }

    @Test
    fun `isPending is true when status is pending`() {
        val member = HouseholdMember(
            userId = "u1", role = "user", joinedAt = null, status = "pending"
        )
        assertTrue(member.isPending)
        assertFalse(member.isActive)
    }

    @Test
    fun `isActive is true when status is active`() {
        val member = HouseholdMember(
            userId = "u1", role = "user", joinedAt = null, status = "active"
        )
        assertTrue(member.isActive)
        assertFalse(member.isPending)
    }

    @Test
    fun `default status is active`() {
        val member = HouseholdMember(userId = "u1", role = "user", joinedAt = null)
        assertTrue(member.isActive)
    }

    // ── label() ─────────────────────────────────────────────────────────────

    @Test
    fun `label returns displayName when available`() {
        val member = HouseholdMember(
            userId = "u1", role = "user", joinedAt = null,
            email = "test@nexo.cl", displayName = "Juan Pérez"
        )
        assertEquals("Juan Pérez", member.label())
    }

    @Test
    fun `label returns email when displayName is null`() {
        val member = HouseholdMember(
            userId = "u1", role = "user", joinedAt = null,
            email = "test@nexo.cl", displayName = null
        )
        assertEquals("test@nexo.cl", member.label())
    }

    @Test
    fun `label returns email when displayName is blank`() {
        val member = HouseholdMember(
            userId = "u1", role = "user", joinedAt = null,
            email = "test@nexo.cl", displayName = "  "
        )
        assertEquals("test@nexo.cl", member.label())
    }

    @Test
    fun `label returns truncated userId when no name or email`() {
        val member = HouseholdMember(
            userId = "abcdef12-3456-7890", role = "user", joinedAt = null,
            email = null, displayName = null
        )
        assertEquals("Usuario ABCDEF12", member.label())
    }

    @Test
    fun `label returns truncated userId when email is blank`() {
        val member = HouseholdMember(
            userId = "xyz12345-long-id", role = "user", joinedAt = null,
            email = "", displayName = ""
        )
        assertEquals("Usuario XYZ12345", member.label())
    }
}
