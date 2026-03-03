package com.nexohogar.presentation.account

import androidx.lifecycle.ViewModel
import com.nexohogar.domain.usecase.GetAccountsUseCase

class AccountViewModel(
    private val getAccountsUseCase: GetAccountsUseCase
) : ViewModel() {
    // ViewModel logic here
}
