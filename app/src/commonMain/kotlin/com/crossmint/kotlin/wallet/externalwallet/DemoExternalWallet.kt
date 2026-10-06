package com.crossmint.kotlin.wallet.externalwallet

import com.crossmint.kotlin.signers.DelegatedSigner
import com.crossmint.kotlin.signers.SignerType

interface Ed25519Key {
    val publicKey: ByteArray

    fun sign(message: ByteArray): ByteArray
}

expect fun generateEd25519Key(): Ed25519Key

object DemoExternalWallet {
    private val key by lazy { generateEd25519Key() }

    val address: String by lazy { Base58.encode(key.publicKey) }

    val locator: String get() = "external-wallet:$address"

    val onSign: suspend (String) -> String = { message -> Base58.encode(key.sign(Base58.decode(message))) }

    fun recoveryMethod(): SignerType = SignerType.ExternalWallet(address, onSign)

    fun signer(): DelegatedSigner = DelegatedSigner.ExternalWallet(address, onSign)
}

internal object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun encode(bytes: ByteArray): String {
        val leadingZeros = bytes.takeWhile { it == 0.toByte() }.size
        var digits = mutableListOf<Int>()
        for (byte in bytes) {
            var carry = byte.toInt() and 0xFF
            digits =
                digits
                    .map { digit ->
                        val value = digit * 256 + carry
                        carry = value / 58
                        value % 58
                    }.toMutableList()
            while (carry > 0) {
                digits.add(carry % 58)
                carry /= 58
            }
        }
        return "1".repeat(leadingZeros) + digits.reversed().joinToString("") { ALPHABET[it].toString() }
    }

    fun decode(text: String): ByteArray {
        val leadingZeros = text.takeWhile { it == '1' }.length
        var bytes = mutableListOf<Int>()
        for (char in text) {
            var carry = ALPHABET.indexOf(char)
            require(carry >= 0) { "Invalid base58 character '$char'" }
            bytes =
                bytes
                    .map { byte ->
                        val value = byte * 58 + carry
                        carry = value / 256
                        value % 256
                    }.toMutableList()
            while (carry > 0) {
                bytes.add(carry % 256)
                carry /= 256
            }
        }
        return ByteArray(leadingZeros) + bytes.reversed().map { it.toByte() }.toByteArray()
    }
}
