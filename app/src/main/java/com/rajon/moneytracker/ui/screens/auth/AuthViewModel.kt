package com.rajon.moneytracker.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.moneytracker.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authRepository.login(email, pass)
            _isLoading.value = false
            result.onSuccess { onSuccess() }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Login failed" }
        }
    }

    fun register(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authRepository.register(email, pass)
            _isLoading.value = false
            result.onSuccess { onSuccess() }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Registration failed" }
        }
    }

    fun forgotPassword(email: String, onSent: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.sendPasswordReset(email)
            _isLoading.value = false
            result.onSuccess { onSent() }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Failed to send reset email" }
        }
    }
}
