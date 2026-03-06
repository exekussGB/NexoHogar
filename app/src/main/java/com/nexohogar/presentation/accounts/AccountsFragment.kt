package com.nexohogar.presentation.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.presentation.theme.NexoHogarTheme

class AccountsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AccountsViewModel(
                    ServiceLocator.accountsRepository,
                    ServiceLocator.tenantContext
                ) as T
            }
        })[AccountsViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                NexoHogarTheme {
                    AccountsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
