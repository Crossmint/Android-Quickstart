package com.crossmint.kotlin.checkoutdemo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.crossmint.kotlin.identity.CrossmintIdentityVerification
import com.crossmint.kotlin.identity.IdentityVerificationCredentials

@Composable
actual fun IdentityPreview(
    apiKey: String,
    usesSession: Boolean,
    id: String,
    secret: String,
    modifier: Modifier,
    onEvent: (String) -> Unit,
) {
    if (id.isBlank()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Enter an inquiry or verification ID to preview identity verification.")
        }
        return
    }

    CrossmintIdentityVerification(
        apiKey = apiKey,
        credentials =
            if (usesSession) {
                IdentityVerificationCredentials.Session(verificationId = id, clientSecret = secret)
            } else {
                IdentityVerificationCredentials(inquiryId = id, sessionToken = secret.ifBlank { null })
            },
        modifier = modifier,
        onReady = { onEvent("Identity verification ready") },
        onComplete = { status -> onEvent("Identity verification completed: $status") },
        onCancel = { onEvent("Identity verification cancelled") },
        onError = { error -> onEvent("Identity verification error: ${error.message}") },
    )
}
