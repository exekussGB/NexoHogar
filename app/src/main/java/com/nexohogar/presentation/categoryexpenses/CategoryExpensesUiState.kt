package com.nexohogar.presentation.categoryexpenses

import com.nexohogar.domain.model.CategoryExpense

data class CategoryExpensesUiState(
    val categories: List<CategoryExpense> = emptyList(),
    val isLoading: Boolean                = false,
    val error: String?                    = null
)
