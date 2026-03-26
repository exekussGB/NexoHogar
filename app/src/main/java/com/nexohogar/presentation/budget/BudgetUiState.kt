package com.nexohogar.presentation.budget

import com.nexohogar.domain.model.BudgetItem
import com.nexohogar.domain.model.Category

data class BudgetUiState(
    val items           : List<BudgetItem>  = emptyList(),
    val categories      : List<Category>    = emptyList(),
    val isLoading       : Boolean           = false,
    val error           : String?           = null,
    val year            : Int               = 0,
    val month           : Int               = 0,
    // Dialog states
    val showCreateDialog: Boolean           = false,
    val showEditDialog  : BudgetItem?       = null,      // non-null = editing this item
    val showDeleteConfirm: BudgetItem?      = null,      // non-null = confirming delete
    val isCreating      : Boolean           = false,
    val isUpdating      : Boolean           = false,
    val isDeleting      : Boolean           = false,
    val snackMessage    : String?           = null
)
