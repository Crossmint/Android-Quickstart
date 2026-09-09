package com.crossmint.kotlin.wallet.playground

import androidx.compose.runtime.Composable
import com.crossmint.kotlin.signers.DelegatedSigner

@Composable
expect fun rememberPasskeyCreator(): (suspend (name: String) -> DelegatedSigner.Passkey?)?
