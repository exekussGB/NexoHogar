package com.nexohogar.presentation.login

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.nexohogar.R
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.databinding.FragmentLoginBinding

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LoginViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        // Inyección vía ServiceLocator (Interfaces de Dominio)
        val sessionManager = ServiceLocator.sessionManager
        val repository = ServiceLocator.authRepository
        
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel(repository, sessionManager) as T
            }
        })[LoginViewModel::class.java]

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is LoginState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    findNavController().navigate(R.id.action_login_to_household)
                }
                is LoginState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val pass = binding.etPassword.text.toString()
            viewModel.login(email, pass)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
