package com.crossmint.kotlin.wallet.createwallet

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.ui.graphics.vector.ImageVector
import com.crossmint.kotlin.signers.OTPDeliveryChannel
import com.crossmint.kotlin.signers.SignerType
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
}

data class DelegatedSignerEntry(
    val type: DelegatedSignerType,
    val email: String = "",
    val phone: String = "",
    val address: String = "",
)
