package com.nexohogar.core.tenant

import com.nexohogar.data.local.SessionManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

/**
 * Contexto centralizado para la gestión del "Tenant" (Household actual).
 *
 * userId y email se obtienen del SDK de Supabase (fuente de verdad del auth).
 * El SessionManager se mantiene para: householdId, biometric, role y extras.
 */
class TenantContext(
    private val sessionManager: SessionManager,
    private val supabaseClient: SupabaseClient
) {

    fun getCurrentHouseholdId(): String? = sessionManager.fetchSelectedHouseholdId()

    fun requireHouseholdId(): String =
        getCurrentHouseholdId()
            ?: throw IllegalStateException("Se intentó realizar una operación sin un Household seleccionado.")

    fun setHouseholdId(id: String) = sessionManager.saveSelectedHouseholdId(id)

    /** userId desde el SDK de Supabase (siempre actualizado) */
    fun getCurrentUserId(): String? =
        supabaseClient.auth.currentUserOrNull()?.id
            ?: sessionManager.fetchSession()?.userId // fallback para compatibilidad

    /** Email del usuario */
    fun getCurrentUserDisplayName(): String? =
        supabaseClient.auth.currentUserOrNull()?.email
            ?: sessionManager.fetchSession()?.email

    // ── Role management ───────────────────────────────────────────────────

    fun getCurrentUserRole(): String? = sessionManager.getExtra("current_user_role")

    fun setCurrentUserRole(role: String) = sessionManager.saveExtra("current_user_role", role)

    fun isSuperUser(): Boolean = getCurrentUserRole() == "super_user"
}
