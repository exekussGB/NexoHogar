package com.nexohogar.domain.usecase

import com.nexohogar.domain.model.Account
import com.nexohogar.domain.repository.AccountRepository

class GetAccountsUseCase(private val repository: AccountRepository) {
    operator fun invoke(): List<Account> {
        // Business logic here
        return emptyList()
    }
}
