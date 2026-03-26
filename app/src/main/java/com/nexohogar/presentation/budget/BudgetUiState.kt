package com.nexohogar.presentation.budget

import com.nexohogar.domain.model.BudgetItem

data class BudgetUiState(
    val items     : List<BudgetItem> = emptyList(),
    val isLoading : Boolean          = false,
    val error     : String?          = null,
    val year      : Int              = 0,
    val month     : Int              = 0
)
