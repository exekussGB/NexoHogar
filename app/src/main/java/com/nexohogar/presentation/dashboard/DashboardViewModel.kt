package com.nexohogar.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.repository.AccountsRepository
import com.nexohogar.domain.repository.DashboardRepository
import com.nexohogar.domain.repository.TransactionsRepository
import com.nexohogar.domain.repository.RecurringBillsRepository
import com.nexohogar.domain.model.RecurringBillStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository,
    private val transactionsRepository: TransactionsRepository,
    private val accountsRepository: AccountsRepository,
    private val recurringBillsRepository: RecurringBillsRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { refreshDashboard() }

    fun refreshDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val householdId = tenantContext.getCurrentHouseholdId()
            if (householdId == null) {
                _uiState.update { it.copy(isLoading = false, error = "No hay hogar seleccionado") }
                return@launch
            }

            val userId = tenantContext.getCurrentUserId()

            val summaryDeferred      = async { dashboardRepository.getDashboardSummary(householdId) }
            val transactionsDeferred = async { transactionsRepository.getTransactions(householdId) }
            val monthlyDeferred      = async { dashboardRepository.getMonthlyBalance(householdId) }
            val personalDeferred     = async {
                if (userId != null) accountsRepository.hasPersonalAccounts(householdId, userId)
                else AppResult.Success(false)
            }
            val billsDeferred    = async { recurringBillsRepository.getRecurringBills(householdId) }
            val balancesDeferred = async { accountsRepository.getAccountBalances(householdId) }

            val summaryResult      = summaryDeferred.await()
            val transactionsResult = transactionsDeferred.await()
            val monthlyResult      = monthlyDeferred.await()
            val personalResult     = personalDeferred.await()
            val billsResult        = billsDeferred.await()
            val balancesResult     = balancesDeferred.await()

            // Próximas facturas y presupuesto comprometido
            val (upcomingBills, pendingTotal) = if (billsResult is AppResult.Success) {
                val bills = billsResult.data.filter { it.isActive }
                val upcoming = bills
                    .filter {
                        it.status() == RecurringBillStatus.OVERDUE ||
                                it.status() == RecurringBillStatus.DUE_SOON ||
                                it.status() == RecurringBillStatus.OK
                    }
                    .sortedBy { it.daysUntilDue() }
                    .take(3)
                val pending = bills
                    .filter { it.status() != RecurringBillStatus.PAID }
                    .sumOf { it.amountClp }
                upcoming to pending
            } else {
                emptyList<com.nexohogar.domain.model.RecurringBill>() to 0L
            }

            // Segregación de cuentas
            var operationalBalance = 0.0
            var savingsTotal       = 0L
            var liabilityTotal     = 0L
            var creditLimitTotal   = 0L
            val creditCardsList    = mutableListOf<com.nexohogar.domain.model.AccountBalance>()

            if (balancesResult is AppResult.Success) {
                balancesResult.data.forEach { account ->
                    when {
                        account.accountType == "EXPENSE" ||
                                account.accountType == "INCOME" -> Unit

                        account.isSavings ->
                            savingsTotal += account.movementBalance

                        account.accountType == "LIABILITY" -> {
                            liabilityTotal += account.movementBalance
                            creditLimitTotal += (account.creditLimit ?: 0L)
                            creditCardsList.add(account)
                        }

                        account.accountType == "ASSET" ->
                            operationalBalance += account.movementBalance
                    }
                }
            }
            android.util.Log.d("Dashboard", "balancesResult: $balancesResult")
            android.util.Log.d("Dashboard", "creditCards count: ${creditCardsList.size}")

            // Ingresos/gastos del mes actual
            var currentIncome  = 0.0
            var currentExpense = 0.0

            if (summaryResult is AppResult.Success &&
                (summaryResult.data.totalIncome > 0 || summaryResult.data.totalExpense > 0)
            ) {
                currentIncome  = summaryResult.data.totalIncome
                currentExpense = summaryResult.data.totalExpense
            } else if (monthlyResult is AppResult.Success && monthlyResult.data.isNotEmpty()) {
                val lastMonth  = monthlyResult.data.last()
                currentIncome  = lastMonth.income.toDouble()
                currentExpense = lastMonth.expense.toDouble()
            }

            val computedTotalBalance = operationalBalance
            val actualLiquidity      = operationalBalance - pendingTotal

            _uiState.update {
                it.copy(
                    summary = if (summaryResult is AppResult.Success) {
                        summaryResult.data.copy(
                            totalBalance  = operationalBalance,
                            totalIncome   = currentIncome,
                            totalExpense  = currentExpense
                        )
                    } else {
                        com.nexohogar.domain.model.DashboardSummary(
                            householdId       = householdId,
                            totalBalance      = operationalBalance,
                            totalIncome       = currentIncome,
                            totalExpense      = currentExpense,
                            accountsCount     = 0,
                            transactionsCount = 0
                        )
                    },
                    recentTransactions   = if (transactionsResult is AppResult.Success) transactionsResult.data.take(5) else emptyList(),
                    monthlyBalance       = if (monthlyResult is AppResult.Success) monthlyResult.data else emptyList(),
                    hasPersonalAccounts  = if (personalResult is AppResult.Success) personalResult.data else false,
                    totalSavings         = savingsTotal,
                    totalLiabilities     = liabilityTotal,
                    totalCreditLimit     = creditLimitTotal,
                    creditCards          = creditCardsList,
                    upcomingBills        = upcomingBills,
                    pendingBillsTotal    = pendingTotal,
                    computedTotalBalance = computedTotalBalance,
                    actualLiquidity      = actualLiquidity,
                    isLoading            = false,
                    error                = null
                )
            }
        }
    }
}