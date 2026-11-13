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
import com.crossmint.kotlin.checkoutdemo.ui.SwitchRow

@Composable
fun IdentitySectionView(
    uiState: PlaygroundUiState,
    onCredentialsChange: (usesSession: Boolean, id: String, secret: String) -> Unit,
    onOpenPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val usesSession = uiState.identityUsesSession

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Paste the inquiry ID (and, if you have one, the session token) from an order's " +
                "`payment.preparation.kyc` to preview the KYC step on its own. When it has a " +
                "`verificationId`, turn on session credentials and paste it with the client secret.",
            style = MaterialTheme.typography.bodyMedium,
        )

        SwitchRow(
            label = "Session credentials",
            checked = usesSession,
            onCheckedChange = { onCredentialsChange(it, uiState.identityId, uiState.identitySecret) },
        )

        OutlinedTextField(
            value = uiState.identityId,
            onValueChange = { onCredentialsChange(usesSession, it, uiState.identitySecret) },
            label = { Text(if (usesSession) "Verification ID" else "Inquiry ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = uiState.identitySecret,
            onValueChange = { onCredentialsChange(usesSession, uiState.identityId, it) },
            label = { Text(if (usesSession) "Client secret" else "Session token (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        FilledTonalButton(
            onClick = onOpenPreview,
            enabled = uiState.identityId.isNotBlank() && (!usesSession || uiState.identitySecret.isNotBlank()),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open preview")
        }
    }
}
