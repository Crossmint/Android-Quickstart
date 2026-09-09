package com.crossmint.kotlin

import androidx.compose.runtime.Composable

/**
 * The type of signer requesting OTP verification.
 */
enum class OTPSignerType {
    EMAIL,
    PHONE,
}

@Composable
expect fun OTPDialog(
    signerType: OTPSignerType = OTPSignerType.EMAIL,
    onOTPSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
)
