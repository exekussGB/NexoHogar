package com.nexohogar.presentation.household

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.nexohogar.R
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.databinding.FragmentHouseholdBinding

class HouseholdFragment : Fragment(R.layout.fragment_household) {

    private var _binding: FragmentHouseholdBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HouseholdViewModel
    private lateinit var adapter: HouseholdAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHouseholdBinding.bind(view)

        // Inyección vía ServiceLocator (Interfaces de Dominio)
        val sessionManager = ServiceLocator.sessionManager
        val repository = ServiceLocator.householdRepository

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HouseholdViewModel(repository, sessionManager) as T
            }
        })[HouseholdViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        
        viewModel.fetchHouseholds()
    }

    private fun setupRecyclerView() {
        adapter = HouseholdAdapter { household ->
            viewModel.selectHousehold(household.id)
            Toast.makeText(context, "Seleccionado: ${household.name}", Toast.LENGTH_SHORT).show()
        }
        binding.rvHouseholds.layoutManager = LinearLayoutManager(context)
        binding.rvHouseholds.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.householdState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is HouseholdState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                }
                is HouseholdState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (state.households.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        adapter.submitList(state.households)
                    }
                }
                is HouseholdState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
