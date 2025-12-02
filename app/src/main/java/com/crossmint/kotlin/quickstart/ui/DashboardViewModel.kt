package com.crossmint.kotlin.quickstart.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossmint.kotlin.auth.AuthManager
import com.crossmint.kotlin.types.EVMChain
import com.crossmint.kotlin.types.Result
import com.crossmint.kotlin.types.SignerData
import com.crossmint.kotlin.types.Transaction
import com.crossmint.kotlin.types.TransactionError
import com.crossmint.kotlin.types.Wallet
import com.crossmint.kotlin.wallets.CrossmintWallets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val wallet: Wallet? = null,
    val isLoading: Boolean = false,
    val isCreatingWallet: Boolean = false,
    val errorMessage: String? = null,
    val isEmpty: Boolean = false,
    val transaction: Transaction? = null,
    val isCreatingTransaction: Boolean = false,
    val transactionError: TransactionError? = null,
    val isTransactionFetched: Boolean = false
) {
    val hasError: Boolean get() = errorMessage != null
    val hasWallet: Boolean get() = wallet != null && !isEmpty
    val walletAddress: String? get() = wallet?.address
    val hasTransaction: Boolean get() = transaction != null
    val hasTransactionError: Boolean get() = transactionError != null
}

class DashboardViewModel(
    private val crossmintWallets: CrossmintWallets,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val recipient: MutableState<String> = mutableStateOf("")
    val tokenLocator: MutableState<String> = mutableStateOf("")
    val amount: MutableState<String> = mutableStateOf("")

    private val chain = EVMChain.BaseSepolia

    fun fetchWallet() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (val result = crossmintWallets.getWallet(chain)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        wallet = result.value,
                        isLoading = false,
                        isEmpty = false
                    )
                }
                is Result.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.error.message,
                        isEmpty = true
                    )
                }
            }
        }
    }

    fun createWallet() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isCreatingWallet = true,
                errorMessage = null
            )

            val signerData = SignerData.Email(authManager.authState.value.email ?: "")

            when (val result = crossmintWallets.createWallet(chain, signerData)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        wallet = result.value,
                        isLoading = false,
                        isCreatingWallet = false,
                        isEmpty = false
                    )
                }
                is Result.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isCreatingWallet = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun sendTransaction(
        recipient: String,
        tokenLocator: String,
        amount: Double,
    ) {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isCreatingTransaction = true,
                    transactionError = null,
                    transaction = null,
                    isTransactionFetched = false,
                )

            val wallet = _uiState.value.wallet
            if (wallet == null) {
                _uiState.value =
                    _uiState.value.copy(
                        isCreatingTransaction = false,
                        transactionError = TransactionError.ServiceNotInitialized("Wallet not loaded"),
                    )
                return@launch
            }

            when (val result = wallet.send(recipient, tokenLocator, amount)) {
                is Result.Success -> {
                    _uiState.value =
                        _uiState.value.copy(
                            transaction = result.value,
                            transactionError = null,
                            isTransactionFetched = false,
                        )

                    fetchTransaction(wallet, result.value.id)
                }
                is Result.Failure -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isCreatingTransaction = false,
                            transaction = null,
                            transactionError = result.error,
                            isTransactionFetched = false,
                        )
                }
            }
        }
    }

    private suspend fun fetchTransaction(
        wallet: Wallet,
        transactionId: String,
    ) {
        when (val result = wallet.getTransaction(transactionId)) {
            is Result.Success -> {
                _uiState.value =
                    _uiState.value.copy(
                        isCreatingTransaction = false,
                        transaction = result.value,
                        transactionError = null,
                        isTransactionFetched = true,
                    )
            }
            is Result.Failure -> {
                _uiState.value =
                    _uiState.value.copy(
                        isCreatingTransaction = false,
                        transaction = _uiState.value.transaction,
                        transactionError = result.error,
                        isTransactionFetched = false,
                    )
            }
        }
    }

    fun clearTransaction() {
        _uiState.value =
            _uiState.value.copy(
                transaction = null,
                transactionError = null,
                isTransactionFetched = false,
                isLoading = false
            )
    }

    fun clearTransactionError() {
        _uiState.value = _uiState.value.copy(transactionError = null)
    }

    fun updateTokenLocator(newText: String) {
        if (newText.trim().equals("xxx", ignoreCase = true)) {
            tokenLocator.value = "base-sepolia:usdc"
            amount.value = "0.01"
            recipient.value = _uiState.value.wallet?.address ?: ""
        }
    }

    fun signTransaction(transactionId: String) {
        viewModelScope.launch {
            if (!_uiState.value.isTransactionFetched) {
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = "Transaction must be fetched before signing",
                    )
                return@launch
            }

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                )

            val wallet = _uiState.value.wallet
            if (wallet == null) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No wallet available",
                    )
                return@launch
            }

            when (val result = wallet.approve(transactionId)) {
                is Result.Success -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            transaction = result.value,
                            transactionError = null,
                            isTransactionFetched = true,
                        )
                }
                is Result.Failure -> {
                    val message = result.error.message
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Signing failed: $message",
                            transactionError = result.error,
                        )
                }
            }
        }
    }

    fun reset() {
        _uiState.value = DashboardUiState()
    }
}
