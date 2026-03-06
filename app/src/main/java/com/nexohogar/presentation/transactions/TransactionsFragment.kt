package com.nexohogar.presentation.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.nexohogar.R
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.presentation.theme.NexoHogarTheme

class TransactionsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TransactionsViewModel(
                    ServiceLocator.transactionsRepository,
                    ServiceLocator.tenantContext
                ) as T
            }
        })[TransactionsViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                NexoHogarTheme {
                    TransactionsScreen(
                        viewModel = viewModel,
                        onTransactionClick = { transaction ->
                            val bundle = Bundle().apply {
                                putString("transactionId", transaction.id)
                            }
                            findNavController().navigate(
                                R.id.action_transactionsFragment_to_transactionDetailFragment,
                                bundle
                            )
                        }
                    )
                }
            }
        }
    }
}
