package com.nexohogar.presentation.budget

import com.nexohogar.domain.model.BudgetItem

data class BudgetUiState(
    val budgets          : List<BudgetItem>  = emptyList(),
    val isLoading        : Boolean           = false,
    val error            : String?           = null
)
