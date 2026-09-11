package com.crossmint.kotlin.checkoutdemo.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crossmint.kotlin.checkoutdemo.PlaygroundUiState
import com.crossmint.kotlin.checkoutdemo.model.OrderDraft
import com.crossmint.kotlin.checkoutdemo.model.TokenPreset
import com.crossmint.kotlin.core.ApiKey
import com.crossmint.kotlin.core.Environment

private enum class OrderMode(
    val label: String,
) {
    NEW("New order"),
    EXISTING("Existing order"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderSectionView(
    uiState: PlaygroundUiState,
    apiKey: String,
    onDraftChange: (OrderDraft) -> Unit,
    onCreateOrder: () -> Unit,
    onExistingOrderChange: (orderId: String, clientSecret: String) -> Unit,
    onUseExistingOrder: () -> Unit,
    onOpenPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = uiState.draft
    var mode by remember { mutableStateOf(OrderMode.NEW) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            OrderMode.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = mode == entry,
                    onClick = { mode = entry },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = OrderMode.entries.size),
                ) {
                    Text(entry.label)
                }
            }
        }

        if (mode == OrderMode.NEW) {
            TokenPicker(
                apiKey = apiKey,
                tokenLocator = draft.tokenLocator,
                onTokenLocatorChange = { onDraftChange(draft.copy(tokenLocator = it)) },
            )

            OutlinedTextField(
                value = draft.amount,
                onValueChange = { onDraftChange(draft.copy(amount = it)) },
                label = { Text("Amount") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = draft.recipientWalletAddress,
                onValueChange = { onDraftChange(draft.copy(recipientWalletAddress = it)) },
                label = { Text("Recipient wallet address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = draft.receiptEmail,
                onValueChange = { onDraftChange(draft.copy(receiptEmail = it)) },
                label = { Text("Receipt email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            uiState.orderErrorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = onCreateOrder,
                enabled =
                    !uiState.isCreatingOrder &&
                        draft.tokenLocator.isNotBlank() &&
                        draft.amount.isNotBlank() &&
                        draft.recipientWalletAddress.isNotBlank() &&
                        draft.receiptEmail.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isCreatingOrder) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create order")
                }
            }
        } else {
            OutlinedTextField(
                value = uiState.existingOrderId,
                onValueChange = { onExistingOrderChange(it, uiState.existingClientSecret) },
                label = { Text("Order ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.existingClientSecret,
                onValueChange = { onExistingOrderChange(uiState.existingOrderId, it) },
                label = { Text("Client secret") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(
                onClick = onUseExistingOrder,
                enabled = uiState.existingOrderId.isNotBlank() && uiState.existingClientSecret.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Use this order")
            }
        }

        if (uiState.isPreviewingCheckout) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            FilledTonalButton(onClick = onOpenPreview, modifier = Modifier.fillMaxWidth()) {
                Text("Open preview")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TokenPicker(
    apiKey: String,
    tokenLocator: String,
    onTokenLocatorChange: (String) -> Unit,
) {
    val environment =
        remember(apiKey) { runCatching { ApiKey.fromString(apiKey).environment }.getOrDefault(Environment.STAGING) }
    val presets = remember(environment) { TokenPreset.presets(environment) }
    val match = remember(presets, tokenLocator) { TokenPreset.findByLocator(environment, tokenLocator) }
    val selectedPreset = match?.first
    var isCustom by remember(presets) { mutableStateOf(tokenLocator.isNotBlank() && match == null) }

    var symbolExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = symbolExpanded, onExpandedChange = { symbolExpanded = it }) {
        OutlinedTextField(
            value =
                when {
                    selectedPreset != null -> selectedPreset.symbol
                    isCustom -> "Custom token"
                    else -> ""
                },
            onValueChange = {},
            readOnly = true,
            label = { Text("Token") },
            placeholder = { Text("Select a token") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = symbolExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = symbolExpanded, onDismissRequest = { symbolExpanded = false }) {
            for (preset in presets) {
                DropdownMenuItem(
                    text = { Text(preset.symbol) },
                    onClick = {
                        isCustom = false
                        onTokenLocatorChange(preset.networks.first().locator)
                        symbolExpanded = false
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Custom token") },
                onClick = {
                    isCustom = true
                    onTokenLocatorChange("")
                    symbolExpanded = false
                },
            )
        }
    }

    if (selectedPreset != null) {
        var networkExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = networkExpanded, onExpandedChange = { networkExpanded = it }) {
            OutlinedTextField(
                value = match.second.title,
                onValueChange = {},
                readOnly = true,
                label = { Text("Chain") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = networkExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = networkExpanded, onDismissRequest = { networkExpanded = false }) {
                for (network in selectedPreset.networks) {
                    DropdownMenuItem(
                        text = { Text(network.title) },
                        onClick = {
                            onTokenLocatorChange(network.locator)
                            networkExpanded = false
                        },
                    )
                }
            }
        }
    }

    if (isCustom) {
        OutlinedTextField(
            value = tokenLocator,
            onValueChange = onTokenLocatorChange,
            label = { Text("Token locator") },
            placeholder = { Text("chain:token-address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
