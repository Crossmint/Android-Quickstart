package com.crossmint.kotlin.wallet

import com.crossmint.kotlin.signers.OTPDeliveryChannel
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

    /**
     * The OTP delivery channel chosen for a phone signer, by phone number.
     *
     * The wallet API never returns this, so a wallet fetched with getWallet always falls back to
     * SMS. The demo keeps the choice here and reapplies it with useSigner on every wallet load.
     */
    private val phoneChannels = mutableMapOf<String, OTPDeliveryChannel>()

    fun rememberPhoneChannel(
        phoneNumber: String,
        channel: OTPDeliveryChannel?,
    ) {
        if (channel != null) phoneChannels[phoneNumber] = channel
    }

    fun phoneChannel(phoneNumber: String): OTPDeliveryChannel? = phoneChannels[phoneNumber]

    /** Called on sign out, so a channel never carries over to the next session. */
    fun clearPhoneChannels() {
        phoneChannels.clear()
    }
}
