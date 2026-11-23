package com.crossmint.kotlin.wallet.createwallet

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.ui.graphics.vector.ImageVector
import com.crossmint.kotlin.signers.OTPDeliveryChannel
import com.crossmint.kotlin.signers.SignerType
import com.crossmint.kotlin.types.Chain
import com.crossmint.kotlin.types.EVMChain
import com.crossmint.kotlin.wallet.externalwallet.DemoExternalWallet

enum class AdminSignerType(
    val displayName: String,
) {
    EMAIL("Email"),
    PHONE("Phone"),
    API_KEY("API Key"),
    EXTERNAL_WALLET("External Wallet (demo key)"),
}

data class RecoverySignerEntry(
    val type: AdminSignerType,
    val email: String = "",
    val phone: String = "",
    val phoneChannel: OTPDeliveryChannel = OTPDeliveryChannel.SMS,
) {
    val isValid: Boolean
        get() =
            when (type) {
                AdminSignerType.EMAIL -> email.isNotBlank() && email.contains("@")
                AdminSignerType.PHONE -> phone.isNotBlank()
                AdminSignerType.API_KEY -> true
                AdminSignerType.EXTERNAL_WALLET -> true
            }

    val locator: String
        get() =
            when (type) {
                AdminSignerType.EMAIL -> "email:${email.trim().lowercase()}"
                AdminSignerType.PHONE -> "phone:${phone.trim()}"
                AdminSignerType.API_KEY -> "api-key"
                AdminSignerType.EXTERNAL_WALLET -> DemoExternalWallet.locator
            }

    fun toSignerType(): SignerType =
        when (type) {
            AdminSignerType.EMAIL -> SignerType.Email(email.trim())
            AdminSignerType.PHONE -> SignerType.Phone(phone.trim(), channel = phoneChannel)
            AdminSignerType.API_KEY -> SignerType.ApiKey
            AdminSignerType.EXTERNAL_WALLET -> DemoExternalWallet.recoveryMethod()
        }
}

fun List<RecoverySignerEntry>.hasDistinctSigners(): Boolean = map { it.locator }.toSet().size == size

fun List<RecoverySignerEntry>.canCreateWallet(): Boolean =
    all { it.isValid } && hasDistinctSigners() && any { it.type != AdminSignerType.EXTERNAL_WALLET }

fun List<RecoverySignerEntry>.canFormQuorum(threshold: Int): Boolean =
    size >= 2 && threshold in 1..size && none { it.type == AdminSignerType.API_KEY }

fun List<SignerType>.phoneSigners(): List<SignerType.Phone> =
    flatMap { signer ->
        when (signer) {
            is SignerType.Phone -> listOf(signer)
            is SignerType.Quorum -> signer.members.phoneSigners()
            else -> emptyList()
        }
    }

fun List<RecoverySignerEntry>.toRecoveryMethods(
    chain: Chain,
    quorumThreshold: Int?,
): List<SignerType> {
    if (quorumThreshold == null) return map { it.toSignerType() }
    val quorum = SignerType.Quorum(threshold = quorumThreshold, members = map { it.toSignerType() })
    return if (chain is EVMChain) listOf(first().toSignerType(), quorum) else listOf(quorum)
}

val OTPDeliveryChannel.displayName: String
    get() =
        when (this) {
            OTPDeliveryChannel.SMS -> "SMS"
            OTPDeliveryChannel.WHATSAPP -> "WhatsApp"
        }

enum class DelegatedSignerType(
    val displayName: String,
    val icon: ImageVector,
) {
    EMAIL("Email", Icons.Filled.Email),
    PHONE("Phone", Icons.Filled.Phone),
    EXTERNAL_WALLET("External Wallet", Icons.Filled.Key),
    DEVICE("Device (this phone)", Icons.Filled.Smartphone),
    PASSKEY("Passkey", Icons.Filled.Fingerprint),
    QUORUM("Quorum", Icons.Filled.Groups),
}

fun List<String>.canFormEmailQuorum(threshold: Int): Boolean {
    val members = map { it.trim().lowercase() }
    return members.size >= 2 &&
        members.all { "@" in it } &&
        members.toSet().size == members.size &&
        threshold in 1..members.size
}

data class DelegatedSignerEntry(
    val type: DelegatedSignerType,
    val email: String = "",
    val phone: String = "",
    val address: String = "",
)
