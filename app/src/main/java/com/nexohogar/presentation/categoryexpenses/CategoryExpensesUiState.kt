package com.nexohogar.presentation.categoryexpenses

import com.nexohogar.domain.model.CategoryExpenseGroup

data class CategoryExpensesUiState(
    val categories: List<CategoryExpenseGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedMonths: Int = 1
)
