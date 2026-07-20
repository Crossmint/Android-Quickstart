package com.crossmint.kotlin.wallet

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Global event bus for wallet-related events.
 * Used to communicate between ViewModels (e.g., CreateWalletViewModel -> WalletViewModel).
 */
object WalletEvents {
    private val _walletCreated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * Emitted when a new wallet is created.
     * WalletViewModel should observe this to refresh the wallet list.
     */
    val walletCreated: SharedFlow<Unit> = _walletCreated.asSharedFlow()

    /**
     * Signal that a wallet was created.
     * Called by CreateWalletViewModel after successful wallet creation.
     */
    fun notifyWalletCreated() {
        _walletCreated.tryEmit(Unit)
    }
}
