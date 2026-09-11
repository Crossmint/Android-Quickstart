package com.crossmint.kotlin.checkoutdemo.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crossmint.kotlin.checkoutdemo.PlaygroundUiState

@Composable
fun IdentitySectionView(
    uiState: PlaygroundUiState,
    onCredentialsChange: (inquiryId: String, sessionToken: String) -> Unit,
    onOpenPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Paste the inquiry ID (and, if you have one, the session token) from an order's " +
                "`payment.preparation.kyc` to preview the KYC step on its own.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = uiState.identityInquiryId,
            onValueChange = { onCredentialsChange(it, uiState.identitySessionToken) },
            label = { Text("Inquiry ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = uiState.identitySessionToken,
            onValueChange = { onCredentialsChange(uiState.identityInquiryId, it) },
            label = { Text("Session token (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        FilledTonalButton(
            onClick = onOpenPreview,
            enabled = uiState.identityInquiryId.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open preview")
        }
    }
}
