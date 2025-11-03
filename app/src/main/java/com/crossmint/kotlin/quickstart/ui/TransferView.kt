package com.crossmint.kotlin.quickstart.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.crossmint.kotlin.quickstart.ui.components.CustomTextField
import com.crossmint.kotlin.quickstart.ui.components.PrimaryButton
import com.crossmint.kotlin.quickstart.ui.theme.SemanticColors
import com.crossmint.kotlin.quickstart.ui.theme.SpacingTokens
import com.crossmint.kotlin.quickstart.ui.theme.TypographyTokens

@Composable
fun TransferView(
    dashboardViewModel: DashboardViewModel
) {
    val uiState by dashboardViewModel.uiState.collectAsState()
    var amount by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("") }
    var tokenLocator by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.hasTransactionError) {
        if (uiState.hasTransactionError) {
            showErrorDialog = true
        }
    }

    if (showErrorDialog && uiState.hasTransactionError) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                dashboardViewModel.clearTransactionError()
            },
            title = { Text("Transaction Error") },
            text = { Text(uiState.transactionError?.message ?: "Unknown error") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showErrorDialog = false
                        dashboardViewModel.clearTransactionError()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SpacingTokens.l)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Transfer funds",
            fontSize = TypographyTokens.title2,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(SpacingTokens.s))

        Text(
            text = "Send funds to another wallet",
            fontSize = TypographyTokens.subheadline,
            color = SemanticColors.textPrimary
        )

        Spacer(modifier = Modifier.height(SpacingTokens.xxl))

        CustomTextField(
            value = amount,
            onValueChange = { amount = it },
            placeholder = "0.0",
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(SpacingTokens.l))

        CustomTextField(
            value = tokenLocator,
            onValueChange = { tokenLocator = it },
            placeholder = "base-sepolia:usdc",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(SpacingTokens.l))

        CustomTextField(
            value = recipient,
            onValueChange = { recipient = it },
            placeholder = "Recipient address",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(SpacingTokens.xxl))

        PrimaryButton(
            text = "Transfer",
            onClick = {
                dashboardViewModel.sendTransaction(
                    recipient = recipient,
                    tokenLocator = tokenLocator,
                    amount = amount.toDoubleOrNull() ?: 0.0
                )
            },
            isLoading = uiState.isCreatingTransaction,
            isDisabled = amount.isEmpty() || recipient.isEmpty() || tokenLocator.isEmpty() || uiState.isCreatingTransaction,
            modifier = Modifier.fillMaxWidth()
        )
    }

}
