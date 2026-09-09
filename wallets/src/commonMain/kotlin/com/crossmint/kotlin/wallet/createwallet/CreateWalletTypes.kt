package com.crossmint.kotlin.wallet.createwallet

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.ui.graphics.vector.ImageVector

enum class AdminSignerType(
    val displayName: String,
) {
    EMAIL("Email"),
    PHONE("Phone"),
    API_KEY("API Key"),
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
