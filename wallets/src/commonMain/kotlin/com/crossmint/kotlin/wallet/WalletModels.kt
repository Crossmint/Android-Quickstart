package com.crossmint.kotlin.wallet

import com.crossmint.kotlin.types.BalanceError
import com.crossmint.kotlin.types.Balances
import com.crossmint.kotlin.types.Chain
import com.crossmint.kotlin.types.EVMChain
import com.crossmint.kotlin.types.SolanaChain
import com.crossmint.kotlin.types.StellarChain
import com.crossmint.kotlin.types.Transaction
import com.crossmint.kotlin.types.TransactionError
import com.crossmint.kotlin.types.Transfer
import com.crossmint.kotlin.types.Wallet
import com.crossmint.kotlin.wallet.createwallet.DelegatedSignerType

data class SignerOption(
    val locator: String,
    val type: String,
    val identifier: String,
    val isAdmin: Boolean,
) {
    val displayName: String
        get() =
            if (isAdmin) {
                "$type: $identifier (Admin)"
            } else {
                "$type: $identifier"
            }
}

data class WalletUiState(
    val wallet: Wallet? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEmpty: Boolean = false,
    val isCreatingWallet: Boolean = false,
    val transaction: Transaction? = null,
    val isCreatingTransaction: Boolean = false,
    val transactionError: TransactionError? = null,
    val isTransactionFetched: Boolean = false,
    val balances: Balances? = null,
    val isLoadingBalances: Boolean = false,
    val balanceError: BalanceError? = null,
    val availableSigners: List<SignerOption> = emptyList(),
    val selectedSignerIndex: Int = 0,
    val deviceSignerSecurityLevel: String? = null,
    val isModifyingSigners: Boolean = false,
    val signerOperationError: String? = null,
    val signerOperationSuccess: Boolean = false,
    val transfers: List<Transfer> = emptyList(),
    val isLoadingTransfers: Boolean = false,
    val transfersError: String? = null,
    val signature: String? = null,
    val isSigning: Boolean = false,
    val signatureError: String? = null,
) {
    val hasError: Boolean get() = errorMessage != null
    val hasWallet: Boolean get() = wallet != null && !isEmpty
    val hasTransaction: Boolean get() = transaction != null
    val hasTransactionError: Boolean get() = transactionError != null
    val hasBalances: Boolean get() = balances != null
    val hasBalanceError: Boolean get() = balanceError != null
    val hasMultipleSigners: Boolean get() = availableSigners.size > 1
    val selectedSigner: SignerOption? get() = availableSigners.getOrNull(selectedSignerIndex)
}

data class TransferToken(
    val displayName: String,
    val locator: String,
) {
    val testId: String get() = locator.substringAfterLast(":").lowercase()
}

enum class SupportedChain(
    val chain: Chain,
    val displayName: String,
    val networkName: String,
    val transferTokens: List<TransferToken>,
) {
    EVM(
        EVMChain.BaseSepolia,
        "EVM",
        "Base Sepolia",
        listOf(
            TransferToken("ETH", "base-sepolia:eth"),
            TransferToken("USDC", "base-sepolia:usdc"),
            TransferToken("USDXM", "base-sepolia:usdxm"),
        ),
    ),
    SOLANA(
        SolanaChain.Solana,
        "Solana",
        "Solana",
        listOf(
            TransferToken("SOL", "solana:sol"),
            TransferToken("USDC", "solana:usdc"),
            TransferToken("USDXM", "solana:usdxm"),
        ),
    ),
    STELLAR(
        StellarChain.Stellar,
        "Stellar",
        "Stellar",
        listOf(
            TransferToken("XLM", "stellar:xlm"),
            TransferToken("USDC", "stellar:usdc"),
            TransferToken("USDXM", "stellar:usdxm"),
        ),
    ),
    ;

    val testId: String
        get() =
            when (this) {
                EVM -> "base-sepolia"
                SOLANA -> "solana"
                STELLAR -> "stellar"
            }

    fun compatibleDelegatedSignerTypes(supportsPasskey: Boolean = false): List<DelegatedSignerType> =
        when (this) {
            EVM ->
                buildList {
                    add(DelegatedSignerType.EXTERNAL_WALLET)
                    add(DelegatedSignerType.DEVICE)
                    if (supportsPasskey) add(DelegatedSignerType.PASSKEY)
                }
            SOLANA, STELLAR -> listOf(DelegatedSignerType.EXTERNAL_WALLET, DelegatedSignerType.DEVICE)
        }
}

internal data class CachedWallet(
    val wallet: Wallet,
    val securityLevel: String?,
)
