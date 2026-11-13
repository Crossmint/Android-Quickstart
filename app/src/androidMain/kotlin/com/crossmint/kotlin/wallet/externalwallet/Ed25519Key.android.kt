package com.crossmint.kotlin.wallet.externalwallet

import com.google.crypto.tink.subtle.Ed25519Sign

actual fun generateEd25519Key(): Ed25519Key {
    val keyPair = Ed25519Sign.KeyPair.newKeyPair()
    val signer = Ed25519Sign(keyPair.privateKey)
    return object : Ed25519Key {
        override val publicKey: ByteArray = keyPair.publicKey

        override fun sign(message: ByteArray): ByteArray = signer.sign(message)
    }
}
