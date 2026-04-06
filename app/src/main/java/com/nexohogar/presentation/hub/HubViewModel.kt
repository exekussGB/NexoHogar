package com.nexohogar.presentation.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.model.RecurringBillStatus
import com.nexohogar.domain.repository.AccountsRepository
import com.nexohogar.domain.repository.BudgetRepository
import com.nexohogar.domain.repository.InventoryRepository
import com.nexohogar.domain.repository.RecurringBillsRepository
import com.nexohogar.domain.repository.WishlistRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HubAlertsState(
    val overdueCount        : Int = 0,  // RecurringBills OVERDUE o DUE_SOON
    val budgetAlertCount    : Int = 0,  // Presupuestos >= 80% consumido
    val lowStockCount       : Int = 0,  // Productos por debajo del stock mínimo
    val wishlistAffordCount : Int = 0   // Ítems de wishlist que el hogar puede pagar
)

class HubViewModel(
    private val recurringBillsRepository : RecurringBillsRepository,
    private val budgetRepository         : BudgetRepository,
    private val inventoryRepository      : InventoryRepository,
    private val wishlistRepository       : WishlistRepository,
    private val accountsRepository       : AccountsRepository,
    private val tenantContext            : TenantContext
) : ViewModel() {

    private val _alerts = MutableStateFlow(HubAlertsState())
    val alerts: StateFlow<HubAlertsState> = _alerts.asStateFlow()

    init { load() }

    fun load() {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        viewModelScope.launch {
            // Lanzar todas las peticiones en paralelo
            val billsDeferred     = async { recurringBillsRepository.getRecurringBills(householdId) }
            val budgetDeferred    = async { budgetRepository.getBudgetConsumption(householdId) }
            val inventoryDeferred = async { inventoryRepository.getProducts(householdId) }
            val wishlistDeferred  = async { wishlistRepository.getWishlistItems(householdId) }
            val accountsDeferred  = async { accountsRepository.getAccounts(householdId) }

            // ── Recurrentes: OVERDUE o DUE_SOON ────────────────────────────
            val overdueCount = when (val r = billsDeferred.await()) {
                is AppResult.Success -> r.data.count { bill ->
                    bill.isActive && (
                            bill.status() == RecurringBillStatus.OVERDUE ||
                                    bill.status() == RecurringBillStatus.DUE_SOON
                            )
                }
                else -> 0
            }

            // ── Presupuesto: >= 80% consumido ───────────────────────────────
            val budgetAlertCount = when (val r = budgetDeferred.await()) {
                is AppResult.Success -> r.data.count { it.consumptionPct >= 80.0 }
                else -> 0
            }

            // ── Inventario: stock actual <= stock mínimo ────────────────────
            val lowStockCount = when (val r = inventoryDeferred.await()) {
                is AppResult.Success -> r.data.count { product ->
                    product.minStock != null && product.currentStock <= product.minStock
                }
                else -> 0
            }

            // ── Balance total del hogar ─────────────────────────────────────
            val totalBalance = when (val r = accountsDeferred.await()) {
                is AppResult.Success -> r.data.sumOf { it.balance.toDouble() }
                else -> 0.0
            }

            // ── Wishlist: ítems no comprados que el hogar puede pagar ───────
            val wishlistAffordCount = when (val r = wishlistDeferred.await()) {
                is AppResult.Success -> r.data.count { item ->
                    !item.isPurchased &&
                            item.price != null &&
                            item.price > 0 &&
                            totalBalance >= item.price
                }
                else -> 0
            }

            _alerts.update {
                HubAlertsState(
                    overdueCount        = overdueCount,
                    budgetAlertCount    = budgetAlertCount,
                    lowStockCount       = lowStockCount,
                    wishlistAffordCount = wishlistAffordCount
                )
            }
        }
    }
}
