package com.crossmint.kotlin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
actual fun OTPDialog(
    signerType: OTPSignerType,
    onOTPSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var otpCode by remember { mutableStateOf("") }

    val message =
        when (signerType) {
            OTPSignerType.EMAIL -> "Please enter the OTP code sent to your email"
            OTPSignerType.PHONE -> "Please enter the OTP code sent to your phone"
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter OTP Code") },
        text = {
            Column {
                Text(message)
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = otpCode,
                    onValueChange = { otpCode = it },
                    label = { Text("OTP Code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onOTPSubmit(otpCode) },
                enabled = otpCode.isNotBlank(),
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
