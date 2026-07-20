package com.crossmint.kotlin.auth.crossmint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.crossmint.kotlin.Deps
import com.crossmint.kotlin.Routes
import com.crossmint.kotlin.auth.CrossmintAuthManager
import com.crossmint.kotlin.auth.models.OTPAuthenticationStatus
import com.crossmint.kotlin.types.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CrossmintAuthViewModel(
    private val authManager: CrossmintAuthManager,
    private val navController: NavController,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CrossmintOTPUiState())
    val uiState: StateFlow<CrossmintOTPUiState> = _uiState.asStateFlow()

    private var resendCooldownJob: Job? = null

    init {
        // Observe auth state from AuthManager
        viewModelScope.launch {
            authManager.authState.collect { authState ->
                _uiState.value =
                    _uiState.value.copy(
                        isAuthenticated = authState.isAuthenticated,
                        userEmail = authState.email,
                    )
            }
        }
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun updateOtpCode(code: String) {
        // Only allow numeric input and limit to 6 digits
        val filteredCode = code.filter { it.isDigit() }.take(6)
        _uiState.value =
            _uiState.value.copy(
                otpCode = filteredCode.ifEmpty { null },
                errorMessage = null,
            )
    }

    fun sendOTP() {
        if (!_uiState.value.isValidEmail) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address")
            return
        }

        val email = _uiState.value.email

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                )

            when (val result = authManager.sendOtp(email)) {
                is Result.Success -> {
                    when (result.value) {
                        OTPAuthenticationStatus.CODE_SENT -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    otpSent = true,
                                    successMessage = null, // No success message for initial send
                                    otpCode = null, // Clear any previous code
                                )
                            startResendCooldown()
                            navController.navigate(Routes.CrossmintOTPVerification)
                        }

                        else -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "Unexpected response. Please try again.",
                                )
                        }
                    }
                }

                is Result.Failure -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = result.error.message ?: "Failed to send OTP",
                        )
                }
            }
        }
    }

    fun verifyOTP() {
        val email = _uiState.value.email
        val code = _uiState.value.otpCode

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                )

            when (val result = authManager.verifyOtp(email, code ?: "")) {
                is Result.Success -> {
                    when (result.value) {
                        OTPAuthenticationStatus.AUTHENTICATED -> {
                            Deps.authEmail = email
                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    isAuthenticated = true,
                                    successMessage = "Successfully authenticated!",
                                )

                            navController.navigate(Routes.Wallet) {
                                popUpTo(Routes.AuthMethodSelection) {
                                    inclusive = true
                                }
                            }
                        }

                        OTPAuthenticationStatus.INVALID_CODE -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "Invalid code. Please check and try again.",
                                    otpCode = null, // Clear the invalid code
                                )
                        }

                        else -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "Verification failed. Please try again.",
                                )
                        }
                    }
                }

                is Result.Failure -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = result.error.message ?: "Failed to verify OTP",
                        )
                }
            }
        }
    }

    fun resendOTP() {
        if (!_uiState.value.canResendOTP) return

        val email = _uiState.value.email

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                )

            when (val result = authManager.sendOtp(email)) {
                is Result.Success -> {
                    when (result.value) {
                        OTPAuthenticationStatus.CODE_SENT -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    otpSent = true,
                                    successMessage = "OTP code resent to $email", // Show success for resend
                                    otpCode = null, // Clear any previous code
                                )
                            startResendCooldown()
                        }

                        else -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "Unexpected response. Please try again.",
                                )
                        }
                    }
                }

                is Result.Failure -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = result.error.message ?: "Failed to resend OTP",
                        )
                }
            }
        }
    }

    fun changeEmail() {
        _uiState.value =
            _uiState.value.copy(
                otpSent = false,
                otpCode = null,
                errorMessage = null,
                successMessage = null,
            )
        resendCooldownJob?.cancel()
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            authManager.logout()
            _uiState.value = CrossmintOTPUiState() // Reset to initial state
        }
    }

    fun clearMessages() {
        _uiState.value =
            _uiState.value.copy(
                errorMessage = null,
                successMessage = null,
            )
    }

    private fun startResendCooldown() {
        resendCooldownJob?.cancel()
        resendCooldownJob =
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(resendCooldown = 60)
                repeat(60) {
                    delay(1000)
                    _uiState.value = _uiState.value.copy(resendCooldown = _uiState.value.resendCooldown - 1)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        resendCooldownJob?.cancel()
    }
}

data class CrossmintOTPUiState(
    val email: String = "",
    val otpCode: String? = null,
    val isLoading: Boolean = false,
    val otpSent: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val resendCooldown: Int = 0,
    val isAuthenticated: Boolean = false,
    val userEmail: String? = null,
) {
    val canSendOTP: Boolean
        get() = email.isNotBlank() && !isLoading

    val canVerifyOTP: Boolean
        get() = otpCode?.length == 6 && !isLoading

    val canResendOTP: Boolean
        get() = resendCooldown == 0 && !isLoading

    val isOTPComplete: Boolean
        get() = otpCode?.length == 6

    val hasError: Boolean
        get() = errorMessage != null

    val hasSuccessMessage: Boolean
        get() = successMessage != null

    val isResendSuccessMessage: Boolean
        get() = successMessage?.contains("sent") == true

    val isValidEmail: Boolean
        get() {
            val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
            return email.matches(emailRegex)
        }
}
