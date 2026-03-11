package com.nexohogar.presentation.addtransaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.presentation.theme.NexoHogarTheme

class AddTransactionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AddTransactionViewModel(
                    ServiceLocator.transactionsRepository,
                    ServiceLocator.categoriesRepository,
                    ServiceLocator.tenantContext
                ) as T
            }
        })[AddTransactionViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                NexoHogarTheme {
                    AddTransactionScreen(
                        viewModel = viewModel,
                        onNavigateBack = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }
}
