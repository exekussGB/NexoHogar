package com.nexohogar.core.tenant

import com.nexohogar.data.local.SessionManager

/**
 * Contexto centralizado para la gestión del "Tenant" (Household actual).
 */
class TenantContext(private val sessionManager: SessionManager) {

    fun getCurrentHouseholdId(): String? {
        return sessionManager.fetchSelectedHouseholdId()
    }

    fun requireHouseholdId(): String {
        return getCurrentHouseholdId()
            ?: throw IllegalStateException("Se intentó realizar una operación sin un Household seleccionado.")
    }

    fun setHouseholdId(id: String) {
        sessionManager.saveSelectedHouseholdId(id)
    }

    /** Returns the current user's ID from the stored session */
    fun getCurrentUserId(): String? {
        return sessionManager.fetchSession()?.userId
    }

    /** Returns the current user's display name from the stored session */
    fun getCurrentUserDisplayName(): String? {
        return sessionManager.fetchSession()?.email
    }

    // 🆕 Feature 1: Role management for super_user check
    /** Returns the current user's role in the selected household */
    fun getCurrentUserRole(): String? {
        return sessionManager.getExtra("current_user_role")
    }

    /** Saves the current user's role for the selected household */
    fun setCurrentUserRole(role: String) {
        sessionManager.saveExtra("current_user_role", role)
    }

    /** Returns true if the current user is a super_user in the selected household */
    fun isSuperUser(): Boolean {
        return getCurrentUserRole() == "super_user"
    }
}
