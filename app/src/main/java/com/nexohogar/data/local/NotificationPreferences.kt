package com.nexohogar.data.local

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Preferencias de notificaciones por categoría.
 * Cada categoría puede habilitarse/deshabilitarse independientemente.
 */
class NotificationPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("NexoHogarPrefs", Context.MODE_PRIVATE)

    var householdEnabled by mutableStateOf(prefs.getBoolean("notif_household", true))
        private set

    var billsEnabled by mutableStateOf(prefs.getBoolean("notif_bills", true))
        private set

    var budgetEnabled by mutableStateOf(prefs.getBoolean("notif_budget", true))
        private set

    var inventoryEnabled by mutableStateOf(prefs.getBoolean("notif_inventory", true))
        private set

    var generalEnabled by mutableStateOf(prefs.getBoolean("notif_general", true))
        private set

    fun setHousehold(enabled: Boolean) {
        householdEnabled = enabled
        prefs.edit().putBoolean("notif_household", enabled).apply()
    }

    fun setBills(enabled: Boolean) {
        billsEnabled = enabled
        prefs.edit().putBoolean("notif_bills", enabled).apply()
    }

    fun setBudget(enabled: Boolean) {
        budgetEnabled = enabled
        prefs.edit().putBoolean("notif_budget", enabled).apply()
    }

    fun setInventory(enabled: Boolean) {
        inventoryEnabled = enabled
        prefs.edit().putBoolean("notif_inventory", enabled).apply()
    }

    fun setGeneral(enabled: Boolean) {
        generalEnabled = enabled
        prefs.edit().putBoolean("notif_general", enabled).apply()
    }

    /**
     * Verifica si un tipo de notificación está habilitado.
     * @param type El tipo de notificación (coincide con los tipos de FCM data)
     */
    fun isTypeEnabled(type: String): Boolean = when (type) {
        "member_request", "member_decision", "household_join" -> householdEnabled
        "recurring_bill" -> billsEnabled
        "budget_alert" -> budgetEnabled
        "low_stock" -> inventoryEnabled
        else -> generalEnabled
    }
}
