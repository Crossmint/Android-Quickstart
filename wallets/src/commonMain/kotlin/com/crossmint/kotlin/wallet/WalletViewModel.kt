package com.crossmint.kotlin.wallet

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossmint.kotlin.auth.AuthManager
import com.crossmint.kotlin.auth.CrossmintAuthManager
import com.crossmint.kotlin.signers.DelegatedSigner
import com.crossmint.kotlin.signers.SignerSelection
import com.crossmint.kotlin.signers.SignerType
import com.crossmint.kotlin.types.Chain
import com.crossmint.kotlin.types.DelegatedSignerData
import com.crossmint.kotlin.types.DelegatedSignerStatus
import com.crossmint.kotlin.types.EVMWallet
import com.crossmint.kotlin.types.Result
import com.crossmint.kotlin.types.SignatureError
import com.crossmint.kotlin.types.SignerData
import com.crossmint.kotlin.types.TransactionError
import com.crossmint.kotlin.types.Wallet
import com.crossmint.kotlin.types.WalletError
import com.crossmint.kotlin.wallets.CrossmintWallets
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class WalletViewModel(
    private val crossmintWallets: CrossmintWallets,
    private val authManager: AuthManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    private val _selectedChain = mutableStateOf(SupportedChain.EVM)
    val selectedChain: State<SupportedChain> = _selectedChain

    private val walletCache = mutableMapOf<SupportedChain, CachedWallet>()
    private val notFoundChains = mutableSetOf<SupportedChain>()
    private val activeFetches = mutableSetOf<SupportedChain>()

    init {
        fetchWallet(selectedChain.value.chain)
        viewModelScope.launch {
            WalletEvents.walletCreated.collect {
                fetchWallet(selectedChain.value.chain)
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun newIdempotencyKey(): String = Uuid.random().toString()

    private suspend fun reapplyPhoneChannel(wallet: Wallet) {
        val admin = wallet.config.adminSigner as? SignerData.Phone ?: return
        val channel = WalletEvents.phoneChannel(admin.phone) ?: return
        wallet.useSigner(DelegatedSigner.Phone(admin.phone, channel = channel))
    }

    fun fetchWallet(chain: Chain) {
        val supportedChain = SupportedChain.entries.find { it.chain == chain } ?: return
        viewModelScope.launch {
            activeFetches.add(supportedChain)
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, isEmpty = false)
            when (val result = crossmintWallets.getWallet(chain)) {
                is Result.Success -> applyFetchSuccess(supportedChain, result.value)
                is Result.Failure -> applyFetchFailure(supportedChain, chain, result.error)
            }
        }
    }

    private suspend fun applyFetchSuccess(
        supportedChain: SupportedChain,
        wallet: Wallet,
    ) {
        val signers = buildAvailableSigners(wallet)
        val securityLevel = crossmintWallets.getDeviceSignerSecurityLevel(wallet.address)
        reapplyPhoneChannel(wallet)
        walletCache[supportedChain] = CachedWallet(wallet, securityLevel)
        notFoundChains.remove(supportedChain)
        activeFetches.remove(supportedChain)
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                wallet = wallet,
                errorMessage = null,
                isEmpty = false,
                availableSigners = signers,
                selectedSignerIndex = 0,
                deviceSignerSecurityLevel = securityLevel,
            )
        preloadOtherChains()
    }

    private suspend fun applyFetchFailure(
        supportedChain: SupportedChain,
        chain: Chain,
        error: WalletError,
    ) {
        activeFetches.remove(supportedChain)
        if (error is WalletError.Unauthorized) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            val refreshed = waitForTokenRefresh()
            if (refreshed) fetchWallet(chain) else _sessionExpired.tryEmit(Unit)
            return
        }
        val isEmpty = error is WalletError.WalletNotFound
        if (isEmpty) notFoundChains.add(supportedChain)
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                wallet = null,
                errorMessage = if (!isEmpty) error.message else null,
                isEmpty = isEmpty,
                availableSigners = emptyList(),
                selectedSignerIndex = 0,
            )
    }

    private suspend fun waitForTokenRefresh(): Boolean {
        val crossmintAuth = authManager as? CrossmintAuthManager ?: return false
        val currentJwt =
            crossmintAuth.authState.value.token
                ?.jwt
        return try {
            withTimeout(5_000) {
                crossmintAuth.authState
                    .filter { state -> state.token?.jwt != currentJwt || !state.isAuthenticated }
                    .first()
                    .isAuthenticated
            }
        } catch (_: TimeoutCancellationException) {
            false
        }
    }

    private fun preloadOtherChains() {
        SupportedChain.entries
            .filter {
                it != selectedChain.value &&
                    walletCache[it] == null &&
                    !activeFetches.contains(it) &&
                    !notFoundChains.contains(it)
            }.forEach { chain ->
                activeFetches.add(chain)
                viewModelScope.launch {
                    when (val result = crossmintWallets.getWallet(chain.chain)) {
                        is Result.Success -> {
                            val wallet = result.value
                            val securityLevel = crossmintWallets.getDeviceSignerSecurityLevel(wallet.address)
                            reapplyPhoneChannel(wallet)
                            walletCache[chain] = CachedWallet(wallet, securityLevel)
                        }
                        is Result.Failure -> {
                            if (result.error is WalletError.WalletNotFound) notFoundChains.add(chain)
                        }
                    }
                    activeFetches.remove(chain)
                }
            }
    }

    private fun buildAvailableSigners(wallet: Wallet): List<SignerOption> {
        val admin = adminSignerToOption(wallet.config.adminSigner)
        val delegated =
            wallet.config.delegatedSigners
                .distinctBy { it.locator }
                .filter { it.status == DelegatedSignerStatus.UNKNOWN || it.status == DelegatedSignerStatus.ACTIVE }
                .map { delegatedSignerToOption(it) }
        return listOf(admin) + delegated
    }

    private fun adminSignerToOption(signer: SignerData): SignerOption {
        val (type, identifier) =
            when (signer) {
                is SignerData.Email -> "email" to signer.email
                is SignerData.Phone -> "phone" to signer.phone
                is SignerData.Passkey -> "passkey" to (signer.name ?: "passkey")
                is SignerData.ApiKey -> "api-key" to "api-key"
                is SignerData.ExternalWallet -> "external-wallet" to signer.address
                is SignerData.Device -> "device" to signer.locator
            }
        return SignerOption(locator = signer.locator, type = type, identifier = identifier, isAdmin = true)
    }

    private fun delegatedSignerToOption(signer: DelegatedSignerData): SignerOption =
        SignerOption(
            locator = signer.locator,
            type = signer.type.value.ifEmpty { "unknown" },
            identifier = signer.identifier,
            isAdmin = false,
        )

    fun createWallet(
        chain: Chain,
        signer: SignerType,
        delegatedSigners: List<DelegatedSigner> = emptyList(),
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingWallet = true, errorMessage = null)
            when (val result = crossmintWallets.createWallet(chain, signer, delegatedSigners)) {
                is Result.Success -> {
                    if (signer is SignerType.Phone) {
                        WalletEvents.rememberPhoneChannel(signer.phoneNumber, signer.channel)
                    }
                    val wallet = result.value
                    val signers = buildAvailableSigners(wallet)
                    val supportedChain = SupportedChain.entries.find { it.chain == chain }
                    if (supportedChain != null) {
                        walletCache[supportedChain] = CachedWallet(wallet, null)
                        notFoundChains.remove(supportedChain)
                    }
                    _uiState.value =
                        _uiState.value.copy(
                            wallet = wallet,
                            isCreatingWallet = false,
                            isEmpty = false,
                            errorMessage = null,
                            availableSigners = signers,
                            selectedSignerIndex = 0,
                        )
                }
                is Result.Failure -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isCreatingWallet = false,
                            errorMessage = "Failed to create wallet: ${result.error.message}",
                        )
                }
            }
        }
    }

    fun selectSigner(index: Int) {
        if (index in 0 until _uiState.value.availableSigners.size) {
            _uiState.value = _uiState.value.copy(selectedSignerIndex = index)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun addDeviceSignerToWallet() {
        val wallet = _uiState.value.wallet ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isModifyingSigners = true, signerOperationError = null)
            when (val result = wallet.recover()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isModifyingSigners = false, signerOperationError = null)
                    fetchWallet(selectedChain.value.chain)
                }
                is Result.Failure ->
                    _uiState.value =
                        _uiState.value.copy(
                            isModifyingSigners = false,
                            signerOperationError = result.error.message,
                        )
            }
        }
    }

    fun addDelegatedSignerToWallet(signer: DelegatedSigner) {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isModifyingSigners = true,
                    signerOperationError = null,
                    signerOperationSuccess = false,
                )
            when (val result = crossmintWallets.addSigner(signer, selectedChain.value.chain)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isModifyingSigners = false, signerOperationSuccess = true)
                    fetchWallet(selectedChain.value.chain)
                }
                is Result.Failure -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isModifyingSigners = false,
                            signerOperationError = "Failed to add signer: ${result.error.message}",
                        )
                }
            }
        }
    }

    fun removeSignerFromWallet(locator: String) {
        val wallet = _uiState.value.wallet ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isModifyingSigners = true, signerOperationError = null)
            when (val result = wallet.removeSigner(DelegatedSigner.Locator(locator))) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isModifyingSigners = false)
                    fetchWallet(selectedChain.value.chain)
                }
                is Result.Failure -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isModifyingSigners = false,
                            signerOperationError = "Failed to remove signer: ${result.error.message}",
                        )
                }
            }
        }
    }

    fun clearAddSignerState() {
        _uiState.value =
            _uiState.value.copy(isModifyingSigners = false, signerOperationError = null, signerOperationSuccess = false)
    }

    fun reset() {
        walletCache.clear()
        notFoundChains.clear()
        activeFetches.clear()
        WalletEvents.clearPhoneChannels()
        _uiState.value = WalletUiState()
    }

    fun sendTransaction(
        recipient: String,
        tokenLocator: String,
        amount: String,
    ) {
        val amountDouble =
            amount.toDoubleOrNull() ?: run {
                _uiState.value =
                    _uiState.value.copy(
                        transactionError = TransactionError.TransactionCreationFailed("Invalid amount"),
                    )
                return
            }
        if (recipient.isBlank() || tokenLocator.isBlank()) return
        val wallet =
            _uiState.value.wallet ?: run {
                _uiState.value =
                    _uiState.value.copy(
                        transactionError = TransactionError.ServiceNotInitialized("Wallet not loaded"),
                    )
                return
            }
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isCreatingTransaction = true,
                    transactionError = null,
                    transaction = null,
                    isTransactionFetched = false,
                )
            val signer = resolveSignerSelection(wallet)
            val result = wallet.send(recipient, tokenLocator, amountDouble, newIdempotencyKey(), signer)
            when (result) {
                is Result.Success -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isCreatingTransaction = false,
                            transaction = result.value,
                            transactionError = null,
                            isTransactionFetched = true,
                        )
                    fetchBalances()
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

    private fun resolveSignerSelection(wallet: Wallet): SignerSelection {
        val selectedOption = _uiState.value.selectedSigner ?: return SignerSelection.Admin
        if (selectedOption.isAdmin) return SignerSelection.Admin
        val delegatedSigner =
            wallet.config.delegatedSigners.find { it.locator == selectedOption.locator }
                ?: return SignerSelection.Admin
        return SignerSelection.Delegated(delegatedSigner)
    }

    fun clearTransaction() {
        _uiState.value =
            _uiState.value.copy(
                transaction = null,
                transactionError = null,
                isTransactionFetched = false,
                isLoading = false,
            )
    }

    fun clearTransactionError() {
        _uiState.value = _uiState.value.copy(transactionError = null)
    }

    fun selectChain(chain: SupportedChain) {
        _selectedChain.value = chain
        clearTransaction()
        clearBalances()
        clearTransfers()

        val cached = walletCache[chain]
        when {
            cached != null -> {
                _uiState.value =
                    _uiState.value.copy(
                        wallet = cached.wallet,
                        isLoading = false,
                        isEmpty = false,
                        errorMessage = null,
                        availableSigners = buildAvailableSigners(cached.wallet),
                        selectedSignerIndex = 0,
                        deviceSignerSecurityLevel = cached.securityLevel,
                    )
            }
            notFoundChains.contains(chain) -> {
                _uiState.value =
                    _uiState.value.copy(
                        wallet = null,
                        isLoading = false,
                        isEmpty = true,
                        errorMessage = null,
                        availableSigners = emptyList(),
                        selectedSignerIndex = 0,
                    )
            }
            else -> fetchWallet(chain.chain)
        }
    }

    fun fetchBalances() {
        val wallet = _uiState.value.wallet ?: return
        val extraTokens = listOf("usdxm")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingBalances = true, balanceError = null)
            when (val result = wallet.balances(extraTokens)) {
                is Result.Success -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoadingBalances = false,
                            balances = result.value,
                            balanceError = null,
                        )
                }
                is Result.Failure -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoadingBalances = false,
                            balances = null,
                            balanceError = result.error,
                        )
                }
            }
        }
    }

    fun clearBalances() {
        _uiState.value = _uiState.value.copy(balances = null, balanceError = null, isLoadingBalances = false)
    }

    fun fundTestTokens() {
        val wallet = _uiState.value.wallet ?: return
        val chain = selectedChain.value
        val (token, chainName) =
            when (chain) {
                SupportedChain.EVM -> "usdxm" to "base-sepolia"
                SupportedChain.SOLANA -> "usdxm" to "solana"
                SupportedChain.STELLAR -> "usdxm" to "stellar"
            }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingBalances = true)
            val result = wallet.fund(token = token, amount = 10, chain = chainName)
            if (result is Result.Failure) {
                _uiState.value = _uiState.value.copy(isLoadingBalances = false, balanceError = result.error)
                return@launch
            }
            // Wait for the balance to propagate on-chain (same as iOS SDK)
            delay(2_500L)
            fetchBalances()
        }
    }

    fun loadTransfers() {
        val wallet = _uiState.value.wallet ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingTransfers = true, transfersError = null)
            when (val result = wallet.listTransfers(selectedChain.value.transferTokens.map { it.locator })) {
                is Result.Success ->
                    _uiState.value =
                        _uiState.value.copy(
                            isLoadingTransfers = false,
                            transfers = result.value.transfers,
                            transfersError = null,
                        )
                is Result.Failure ->
                    _uiState.value =
                        _uiState.value.copy(
                            isLoadingTransfers = false,
                            transfersError = result.error.message,
                        )
            }
        }
    }

    fun clearTransfers() {
        _uiState.value = _uiState.value.copy(transfers = emptyList(), transfersError = null, isLoadingTransfers = false)
    }

    fun signMessage(message: String) = performSigning { wallet, signer -> wallet.signMessage(message, signer) }

    fun signTypedData(typedDataJson: String) =
        performSigning { wallet, signer -> wallet.signTypedData(typedDataJson, signer) }

    fun clearSignature() {
        _uiState.value = _uiState.value.copy(signature = null, signatureError = null, isSigning = false)
    }

    private fun performSigning(block: suspend (EVMWallet, SignerSelection) -> Result<String, SignatureError>) {
        val wallet = _uiState.value.wallet ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSigning = true, signatureError = null, signature = null)
            if (wallet !is EVMWallet) {
                _uiState.value =
                    _uiState.value.copy(
                        isSigning = false,
                        signatureError = SignatureError.ChainNotSupported().message,
                    )
                return@launch
            }
            val signer = resolveSignerSelection(wallet)
            when (val result = block(wallet, signer)) {
                is Result.Success ->
                    _uiState.value =
                        _uiState.value.copy(isSigning = false, signature = result.value, signatureError = null)
                is Result.Failure ->
                    _uiState.value =
                        _uiState.value.copy(isSigning = false, signatureError = result.error.message)
            }
        }
    }
}
