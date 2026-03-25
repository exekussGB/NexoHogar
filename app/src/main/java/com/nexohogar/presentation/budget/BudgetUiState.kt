package com.nexohogar.presentation.budget

import com.nexohogar.domain.model.BudgetConsumption
import com.nexohogar.domain.model.Category
import com.nexohogar.domain.model.HouseholdMember
import java.time.LocalDate

data class BudgetUiState(
    val isLoading: Boolean = true,
    val budgets: List<BudgetConsumption> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedYear: Int = LocalDate.now().year,
    val selectedMonth: Int = LocalDate.now().monthValue,
    val totalBudgeted: Double = 0.0,
    val totalConsumed: Double = 0.0,
    val errorMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingBudget: BudgetConsumption? = null,
    val isCreating: Boolean = false,
    val members: List<HouseholdMember> = emptyList(),
    val selectedMemberId: String? = null
)