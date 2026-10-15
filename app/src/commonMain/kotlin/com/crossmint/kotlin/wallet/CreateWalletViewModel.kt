package com.crossmint.kotlin.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.crossmint.kotlin.OTPSignerType
import com.crossmint.kotlin.signers.DelegatedSigner
import com.crossmint.kotlin.signers.SignerType
import com.crossmint.kotlin.types.Chain
import com.crossmint.kotlin.types.Result
import com.crossmint.kotlin.types.Wallet
import com.crossmint.kotlin.wallets.CrossmintWallets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateWalletUiState(
    val isCreating: Boolean = false,
    val errorMessage: String? = null,
    val createdWallet: Wallet? = null,
    val pendingOTPSignerType: OTPSignerType? = null,
) {
    val hasError: Boolean
        get() = errorMessage != null

    val walletCreated: Boolean
        get() = createdWallet != null
}

class CreateWalletViewModel(
    private val crossmintWallets: CrossmintWallets,
    private val navController: NavController,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateWalletUiState())
    val uiState: StateFlow<CreateWalletUiState> = _uiState.asStateFlow()

    fun createWallet(
        chain: Chain,
        recoveryMethods: List<SignerType>,
        delegatedSigners: List<DelegatedSigner> = emptyList(),
        deviceSigner: Boolean = false,
    ) {
        val primary = recoveryMethods.firstOrNull() ?: return
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isCreating = true,
                    errorMessage = null,
                    pendingOTPSignerType =
                        if (deviceSigner) {
                            when (primary) {
                                is SignerType.Email -> OTPSignerType.EMAIL
                                is SignerType.Phone -> OTPSignerType.PHONE
                                SignerType.ApiKey, is SignerType.Passkey, is SignerType.ExternalWallet -> null
                            }
                        } else {
                            null
                        },
                )

            val result =
                crossmintWallets.createWallet(chain, recoveryMethods, delegatedSigners, deviceSigner = deviceSigner)
            when (result) {
                is Result.Success -> {
                    recoveryMethods.filterIsInstance<SignerType.Phone>().forEach {
                        WalletEvents.rememberPhoneChannel(it.phoneNumber, it.channel)
                    }
                    _uiState.value =
                        _uiState.value.copy(
                            createdWallet = result.value,
                            isCreating = false,
                            errorMessage = null,
                            pendingOTPSignerType = null,
                        )
                    // Signal WalletViewModel to refresh
                    WalletEvents.notifyWalletCreated()
                    // Navigate back to wallet screen on success
                    navController.popBackStack()
                }
                is Result.Failure -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isCreating = false,
                            errorMessage = "Failed to create wallet: ${result.error.message}",
                            pendingOTPSignerType = null,
                        )
                }
            }
        }
    }

    fun cancel() {
        navController.popBackStack()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun reset() {
        _uiState.value = CreateWalletUiState()
    }
}
