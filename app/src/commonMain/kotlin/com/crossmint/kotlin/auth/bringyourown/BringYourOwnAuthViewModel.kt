package com.crossmint.kotlin.auth.bringyourown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossmint.kotlin.Deps
import com.crossmint.kotlin.auth.AuthManager
import com.crossmint.kotlin.utility.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BringYourOwnAuthViewModel(
    private val goToWallets: () -> Unit,
    private val authManager: AuthManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BringYourOwnAuthUiState())
    val uiState: StateFlow<BringYourOwnAuthUiState> = _uiState.asStateFlow()

    fun clearMessages() {
        _uiState.value =
            _uiState.value.copy(
                errorMessage = null,
                successMessage = null,
            )
    }

    fun updateJWT(token: String) {
        _uiState.value = _uiState.value.copy(jwt = token, errorMessage = null)
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun submitJWT() {
        viewModelScope.launch {
            val email = _uiState.value.email.trim()
            if (email.isBlank() || !isValidEmail(email)) {
                _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address")
                return@launch
            }

            val jwt = _uiState.value.jwt.trim()
            if (jwt.isBlank() || !isValidJWT(jwt)) {
                _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid JWT token")
                return@launch
            }

            authManager.setJWT(jwt)

            showToast("Authentication successful")
            withContext(Dispatchers.Main) {
                Deps.authEmail = email
                goToWallets()
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }

    private fun isValidJWT(token: String): Boolean {
        val parts = token.split(".")
        return parts.size == 3 && parts.all { it.isNotBlank() }
    }
}

data class BringYourOwnAuthUiState(
    val jwt: String = "",
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isAuthenticated: Boolean = false,
) {
    val hasError: Boolean
        get() = errorMessage != null
}
