package com.nexohogar.core.tenant

import com.nexohogar.data.local.SessionManager

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

    fun getCurrentUserId(): String? {
        return sessionManager.fetchSession()?.userId
    }

    fun requireUserId(): String {
        return getCurrentUserId()
            ?: throw IllegalStateException("No hay sesión de usuario activa.")
    }
}
