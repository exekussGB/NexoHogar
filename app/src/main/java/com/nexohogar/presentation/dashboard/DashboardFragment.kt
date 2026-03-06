package com.nexohogar.presentation.dashboard

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

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(
                    ServiceLocator.dashboardRepository,
                    ServiceLocator.tenantContext
                ) as T
            }
        })[DashboardViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                NexoHogarTheme {
                    DashboardScreen(
                        viewModel = viewModel,
                        onAccountsClick = {
                            findNavController().navigate(R.id.action_dashboardFragment_to_accountsFragment)
                        },
                        onTransactionsClick = {
                            findNavController().navigate(R.id.action_dashboardFragment_to_transactionsFragment)
                        }
                    )
                }
            }
        }
    }
}
