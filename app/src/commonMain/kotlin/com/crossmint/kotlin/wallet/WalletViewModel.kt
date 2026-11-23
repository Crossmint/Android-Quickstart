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
import com.crossmint.kotlin.types.ChainType
import com.crossmint.kotlin.types.DelegatedSignerData
import com.crossmint.kotlin.types.DelegatedSignerStatus
import com.crossmint.kotlin.types.EVMWallet
import com.crossmint.kotlin.types.Result
import com.crossmint.kotlin.types.SignatureError
import com.crossmint.kotlin.types.SignerChangeResult
import com.crossmint.kotlin.types.SignerData
import com.crossmint.kotlin.types.Transaction
import com.crossmint.kotlin.types.TransactionError
import com.crossmint.kotlin.types.TransactionStatus
import com.crossmint.kotlin.types.Wallet
import com.crossmint.kotlin.types.WalletError
import com.crossmint.kotlin.wallet.createwallet.phoneSigners
import com.crossmint.kotlin.wallet.externalwallet.DemoExternalWallet
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
    var passkeyActivator: (suspend (Wallet, String) -> Result<Unit, WalletError>)? = null

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
        val signers = loadSigners(wallet)
        val securityLevel = crossmintWallets.getDeviceSignerSecurityLevel(wallet.address)
        reapplyPhoneChannel(wallet)
        walletCache[supportedChain] = CachedWallet(wallet, securityLevel, signers)
        notFoundChains.remove(supportedChain)
        activeFetches.remove(supportedChain)
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                wallet = wallet,
                errorMessage = null,
                isEmpty = false,
                availableSigners = signers.available,
                pendingSigners = signers.pending,
                signersError = signers.error,
                selectedSignerIndex = 0,
                deviceSignerSecurityLevel = securityLevel,
                pendingRemovals =
                    _uiState.value.pendingRemovals.withoutFinished(
                        wallet.address,
                        wallet.config.recoveryMethods
                            .map { it.signer.locator }
                            .toSet(),
                    ),
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
                pendingSigners = emptyList(),
                signersError = null,
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
                            walletCache[chain] = CachedWallet(wallet, securityLevel, loadSigners(wallet))
                        }
                        is Result.Failure -> {
                            if (result.error is WalletError.WalletNotFound) notFoundChains.add(chain)
                        }
                    }
                    activeFetches.remove(chain)
                }
            }
    }

    private suspend fun loadSigners(wallet: Wallet): WalletSigners {
        val delegated =
            when (val result = wallet.signers()) {
                is Result.Success -> result.value.distinctBy { it.locator }
                is Result.Failure ->
                    return WalletSigners(
                        available = recoverySignerOptions(wallet),
                        pending = emptyList(),
                        error = "Failed to load signers: ${result.error.message}",
                    )
            }
        val working = delegated.filter { it.status == DelegatedSignerStatus.ACTIVE }
        val pending =
            delegated.filter {
                it.status != DelegatedSignerStatus.ACTIVE &&
                    it.status != DelegatedSignerStatus.FAILED
            }
        return WalletSigners(
            available = recoverySignerOptions(wallet) + working.flatMap { signerOptions(it) },
            pending = pending.map { delegatedSignerToOption(it).copy(isPending = true) },
        )
    }

    private fun recoverySignerOptions(wallet: Wallet): List<SignerOption> =
        wallet.config.recoveryMethods
            .filter {
                it.status == DelegatedSignerStatus.ACTIVE ||
                    (it.status == DelegatedSignerStatus.UNKNOWN && wallet.chainType == ChainType.EVM)
            }.flatMap { method ->
                (method.signer as? SignerData.Quorum)?.let { quorumMemberOptions(it, isAdmin = true) }
                    ?: listOf(method.signer).filter(::canSignInDemo).map(::adminSignerToOption)
            }

    private fun signerOptions(signer: DelegatedSignerData): List<SignerOption> =
        listOf(delegatedSignerToOption(signer)) +
            signer.quorum?.let { quorumMemberOptions(it, isAdmin = false) }.orEmpty()

    private fun quorumMemberOptions(
        quorum: SignerData.Quorum,
        isAdmin: Boolean,
    ): List<SignerOption> =
        quorum.members
            .filter(::canSignInDemo)
            .map { adminSignerToOption(it).copy(isAdmin = isAdmin, quorumLocator = quorum.locator) }

    private fun canSignInDemo(signer: SignerData): Boolean =
        signer !is SignerData.Server &&
            signer !is SignerData.Unknown &&
            (signer !is SignerData.ExternalWallet || signer.locator == DemoExternalWallet.locator)

    private fun adminSignerToOption(signer: SignerData): SignerOption {
        val (type, identifier) =
            when (signer) {
                is SignerData.Email -> "email" to signer.email
                is SignerData.Phone -> "phone" to signer.phone
                is SignerData.Passkey -> "passkey" to (signer.name ?: "passkey")
                is SignerData.ApiKey -> "api-key" to "api-key"
                is SignerData.ExternalWallet -> "external-wallet" to signer.address
                is SignerData.Device -> "device" to signer.locator
                is SignerData.Server -> "server" to signer.address
                is SignerData.Quorum -> "quorum" to "${signer.threshold} of ${signer.members.size}"
                is SignerData.Unknown -> signer.type to signer.locator
            }
        return SignerOption(
            locator = signer.locator,
            type = type,
            identifier = identifier,
            isAdmin = true,
            name = (signer as? SignerData.Passkey)?.name,
        )
    }

    private fun delegatedSignerToOption(signer: DelegatedSignerData): SignerOption =
        SignerOption(
            locator = signer.locator,
            type = signer.type.value.ifEmpty { "unknown" },
            identifier = signer.identifier,
            isAdmin = false,
            name = signer.name,
        )

    fun createWallet(
        chain: Chain,
        recoveryMethods: List<SignerType>,
        delegatedSigners: List<DelegatedSigner> = emptyList(),
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingWallet = true, errorMessage = null)
            when (val result = crossmintWallets.createWallet(chain, recoveryMethods, delegatedSigners)) {
                is Result.Success -> {
                    recoveryMethods.phoneSigners().forEach {
                        WalletEvents.rememberPhoneChannel(it.phoneNumber, it.channel)
                    }
                    val wallet = result.value
                    val signers = loadSigners(wallet)
                    val supportedChain = SupportedChain.entries.find { it.chain == chain }
                    if (supportedChain != null) {
                        walletCache[supportedChain] = CachedWallet(wallet, null, signers)
                        notFoundChains.remove(supportedChain)
                    }
                    _uiState.value =
                        _uiState.value.copy(
                            wallet = wallet,
                            isCreatingWallet = false,
                            isEmpty = false,
                            errorMessage = null,
                            availableSigners = signers.available,
                            pendingSigners = signers.pending,
                            signersError = signers.error,
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

    fun addDeviceSignerToWallet() =
        modifySigners("Failed to add device signer") { wallet, approver -> wallet.recover(approver) }

    fun addDelegatedSignerToWallet(signer: DelegatedSigner) =
        modifySigners("Failed to add signer") { wallet, approver -> wallet.addSigner(signer, approver) }

    fun removeSignerFromWallet(locator: String) =
        modifySigners("Failed to remove signer") { wallet, approver ->
            wallet.removeSigner(DelegatedSigner.Locator(locator), approver)
        }

    fun addRecoveryMethodToWallet(recoveryMethod: DelegatedSigner) =
        modifySigners("Failed to add recovery method") { wallet, approver ->
            wallet.addRecoveryMethod(recoveryMethod, approver)
        }

    fun removeRecoveryMethodFromWallet(locator: String) =
        modifySigners("Failed to remove recovery method") { wallet, approver ->
            wallet.removeRecoveryMethod(DelegatedSigner.Locator(locator), approver).also { result ->
                val transactionId =
                    (result as? Result.Success)
                        ?.value
                        ?.takeIf { it.status == TransactionStatus.AWAITING_APPROVAL }
                        ?.transactionId
                if (transactionId != null) {
                    _uiState.value =
                        _uiState.value.copy(
                            pendingRemovals =
                                _uiState.value.pendingRemovals +
                                    (transactionId to PendingRemoval(wallet.address, locator)),
                        )
                }
            }
        }

    private fun modifySigners(
        errorPrefix: String,
        operation: suspend (Wallet, approver: DelegatedSigner?) -> Result<SignerChangeResult, WalletError>,
    ) {
        val wallet = _uiState.value.wallet ?: return
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isModifyingSigners = true,
                    signerOperationError = null,
                    signerOperationSuccess = false,
                    signerOperationNotice = null,
                )
            val recoveryLocators = wallet.config.recoveryMethods.map { it.signer.locator }
            val recoverySigner = _uiState.value.selectedSigner?.takeIf { it.isAdmin }
            val approver =
                recoverySigner
                    ?.takeIf { it.locator in recoveryLocators || it.quorumLocator != null }
                    ?.locator ?: recoveryLocators.first()
            val passkeyActivation =
                if (approver.startsWith(PASSKEY_PREFIX)) activatePasskey(wallet, approver) else null
            val selection =
                passkeyActivation?.takeIf { it is Result.Failure }
                    ?: wallet.useRecoveryMethod(recoveryMethodFor(approver))
            val quorumApprover = recoverySigner?.quorumLocator?.let { DelegatedSigner.Locator(it) }
            val result =
                when (selection) {
                    is Result.Failure -> selection
                    is Result.Success -> operation(wallet, quorumApprover).approveChangeIfAwaiting(wallet)
                }
            val notice = (result as? Result.Success)?.value?.awaitingNotice()
            when (result) {
                is Result.Success -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isModifyingSigners = false,
                            signerOperationSuccess = true,
                            signerOperationNotice = notice,
                        )
                    fetchWallet(selectedChain.value.chain)
                }
                is Result.Failure -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isModifyingSigners = false,
                            signerOperationError = "$errorPrefix: ${result.error.message}",
                        )
                    fetchWallet(selectedChain.value.chain)
                }
            }
        }
    }

    private suspend fun Result<SignerChangeResult, WalletError>.approveChangeIfAwaiting(
        wallet: Wallet,
    ): Result<SignerChangeResult, WalletError> {
        val change = (this as? Result.Success)?.value ?: return this
        val transactionId =
            change.transactionId?.takeIf { change.status == TransactionStatus.AWAITING_APPROVAL } ?: return this
        return when (val approval = wallet.approve(transactionId, SignerSelection.Admin)) {
            is Result.Success -> {
                if (approval.value.status != TransactionStatus.AWAITING_APPROVAL) clearPendingRemoval(transactionId)
                Result.Success(SignerChangeResult(transactionId, approval.value.status))
            }
            is Result.Failure ->
                Result.Failure(
                    WalletError.WalletOperationFailed(
                        "${approval.error.message}. Approve transaction $transactionId to retry",
                    ),
                )
        }
    }

    private fun clearPendingRemoval(transactionId: String) {
        _uiState.value = _uiState.value.copy(pendingRemovals = _uiState.value.pendingRemovals - transactionId)
    }

    private fun SignerChangeResult.awaitingNotice(): String? =
        transactionId
            ?.takeIf { status == TransactionStatus.AWAITING_APPROVAL }
            ?.let { "Waiting for other quorum members. Transaction ID: $it" }

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
            val activation = activateSelectedSigner(wallet)
            if (activation is Result.Failure) {
                _uiState.value =
                    _uiState.value.copy(
                        isCreatingTransaction = false,
                        transactionError = TransactionError.SigningFailed(activation.error.message),
                    )
                return@launch
            }
            val signer = resolveSignerSelection(wallet)
            val sent = wallet.send(recipient, tokenLocator, amountDouble, newIdempotencyKey(), signer)
            applyTransactionResult(sent.approveIfAwaiting(wallet))
        }
    }

    fun approveTransaction(transactionId: String) {
        val wallet = _uiState.value.wallet ?: return
        if (transactionId.isBlank()) return
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isCreatingTransaction = true,
                    transactionError = null,
                    transaction = null,
                    isTransactionFetched = false,
                )
            val selection = activateSelectedSigner(wallet)
            if (selection is Result.Failure) {
                _uiState.value =
                    _uiState.value.copy(
                        isCreatingTransaction = false,
                        transactionError = TransactionError.SigningFailed(selection.error.message),
                    )
                return@launch
            }
            val approval = wallet.approve(transactionId.trim(), approvalSignerSelection(wallet))
            applyTransactionResult(approval)
            if (approval is Result.Success && approval.value.status != TransactionStatus.AWAITING_APPROVAL) {
                clearPendingRemoval(transactionId.trim())
                _uiState.value = _uiState.value.copy(signerOperationNotice = null)
                fetchWallet(selectedChain.value.chain)
            }
        }
    }

    private suspend fun Result<Transaction, TransactionError>.approveIfAwaiting(
        wallet: Wallet,
    ): Result<Transaction, TransactionError> {
        val created =
            (this as? Result.Success)?.value?.takeIf { it.status == TransactionStatus.AWAITING_APPROVAL } ?: return this
        return when (val approval = wallet.approve(created.id, SignerSelection.Admin)) {
            is Result.Success -> approval
            is Result.Failure ->
                Result.Failure(
                    TransactionError.SigningFailed(
                        "${approval.error.message}. Approve transaction ${created.id} to retry",
                    ),
                )
        }
    }

    private fun applyTransactionResult(result: Result<Transaction, TransactionError>) {
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

    private suspend fun activatePasskey(
        wallet: Wallet,
        locator: String,
    ): Result<Unit, WalletError> =
        passkeyActivator?.invoke(wallet, locator.removePrefix(PASSKEY_PREFIX))
            ?: Result.Failure(WalletError.InvalidInput("Passkey signing is not available on this platform"))

    private suspend fun activateSelectedSigner(wallet: Wallet): Result<Unit, WalletError>? {
        val selected = _uiState.value.selectedSigner ?: return null
        if (selected.locator.startsWith(PASSKEY_PREFIX)) return activatePasskey(wallet, selected.locator)
        val signer = selectedQuorumMember() ?: selectedRecoveryApprover() ?: return null
        return wallet.useSigner(signer)
    }

    private fun approvalSignerSelection(wallet: Wallet): SignerSelection =
        if (_uiState.value.selectedSigner?.quorumLocator != null) {
            SignerSelection.Admin
        } else {
            resolveSignerSelection(wallet)
        }

    private fun selectedQuorumMember(): DelegatedSigner? =
        _uiState.value.selectedSigner
            ?.takeIf { it.quorumLocator != null }
            ?.let { recoveryMethodFor(it.locator) }

    private fun selectedRecoveryApprover(): DelegatedSigner? =
        _uiState.value.selectedSigner
            ?.takeIf { it.isAdmin }
            ?.let { recoveryMethodFor(it.locator) }

    private fun recoveryMethodFor(locator: String): DelegatedSigner =
        when {
            locator == DemoExternalWallet.locator -> DemoExternalWallet.signer()
            locator.startsWith(PHONE_PREFIX) -> {
                val phoneNumber = locator.removePrefix(PHONE_PREFIX)
                DelegatedSigner.Phone(phoneNumber, channel = WalletEvents.phoneChannel(phoneNumber))
            }
            else -> DelegatedSigner.Locator(locator)
        }

    private fun resolveSignerSelection(wallet: Wallet): SignerSelection {
        val selectedOption = _uiState.value.selectedSigner ?: return SignerSelection.Admin
        selectedOption.quorumLocator?.let { return SignerSelection.Quorum(it) }
        if (selectedOption.locator == wallet.config.adminSigner.locator) return SignerSelection.Admin
        val delegatedSigner =
            wallet.config.delegatedSigners.find { it.locator == selectedOption.locator }
                ?: DelegatedSignerData(locator = selectedOption.locator)
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
                        availableSigners = cached.signers.available,
                        pendingSigners = cached.signers.pending,
                        signersError = cached.signers.error,
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
                        pendingSigners = emptyList(),
                        signersError = null,
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

private const val PASSKEY_PREFIX = "passkey:"
private const val PHONE_PREFIX = "phone:"
