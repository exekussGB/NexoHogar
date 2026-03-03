package com.nexohogar.core.tenant

import com.nexohogar.data.local.SessionManager

/**
 * Contexto centralizado para la gestión del "Tenant" (Household actual).
 * Esta clase actúa como la única fuente de verdad para saber en qué hogar 
 * está operando el usuario en un momento dado.
 */
class TenantContext(private val sessionManager: SessionManager) {

    /**
     * Obtiene el ID del household actual. Puede ser nulo si no se ha seleccionado ninguno.
     */
    fun getCurrentHouseholdId(): String? {
        return sessionManager.fetchSelectedHouseholdId()
    }

    /**
     * Obtiene el ID del household actual o lanza una excepción si no existe.
     * Útil para operaciones que requieren obligatoriamente un contexto de hogar.
     */
    fun requireHouseholdId(): String {
        return getCurrentHouseholdId() 
            ?: throw IllegalStateException("Se intentó realizar una operación sin un Household seleccionado.")
    }

    /**
     * Actualiza el household actual en el contexto y persiste el cambio.
     */
    fun setHouseholdId(id: String) {
        sessionManager.saveSelectedHouseholdId(id)
    }
}
