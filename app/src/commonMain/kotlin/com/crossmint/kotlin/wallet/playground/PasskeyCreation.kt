package com.crossmint.kotlin.wallet.playground

import androidx.compose.runtime.Composable
import com.crossmint.kotlin.signers.DelegatedSigner
import com.crossmint.kotlin.types.Result
import com.crossmint.kotlin.types.Wallet
import com.crossmint.kotlin.types.WalletError

@Composable
expect fun rememberPasskeyCreator(): (suspend (name: String) -> DelegatedSigner.Passkey?)?

@Composable
expect fun rememberPasskeyActivator(): (suspend (Wallet, String) -> Result<Unit, WalletError>)?
