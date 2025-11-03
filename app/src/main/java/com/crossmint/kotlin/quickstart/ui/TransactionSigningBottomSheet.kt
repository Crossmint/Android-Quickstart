package com.crossmint.kotlin.quickstart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.crossmint.kotlin.quickstart.ui.theme.DimensionTokens
import com.crossmint.kotlin.quickstart.ui.theme.SemanticColors
import com.crossmint.kotlin.quickstart.ui.theme.SpacingTokens
import com.crossmint.kotlin.quickstart.ui.theme.TypographyTokens
import com.crossmint.kotlin.types.TransactionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionSigningBottomSheet(
    viewModel: DashboardViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val transaction = uiState.transaction
    var showErrorDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    LaunchedEffect(uiState.hasTransactionError) {
        if (uiState.hasTransactionError) {
            showErrorDialog = true
        }
    }

    LaunchedEffect(transaction?.status) {
        if (transaction?.status == TransactionStatus.SUCCESS) {
            showSuccessDialog = true
        }
    }

    LaunchedEffect(showSuccessDialog) {
        if (showSuccessDialog) {
            kotlinx.coroutines.delay(2000)
            sheetState.hide()
            onDismiss()
        }
    }

    if (showErrorDialog && uiState.hasTransactionError) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                viewModel.clearTransactionError()
            },
            title = { Text("Error") },
            text = { Text(uiState.transactionError?.message ?: "Unknown error") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showErrorDialog = false
                        viewModel.clearTransactionError()
                    },
                ) {
                    Text("OK")
                }
            },
        )
    }

    if (showSuccessDialog && transaction?.status == TransactionStatus.SUCCESS) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onDismiss()
            },
            title = { Text("Success") },
            text = { Text("Transaction signed successfully!") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onDismiss()
                    },
                ) {
                    Text("OK")
                }
            },
        )
    }

    if (transaction != null) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!uiState.isLoading) {
                    onDismiss()
                }
            },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingTokens.xxl)
                    .padding(top = SpacingTokens.xl)
                    .padding(bottom = SpacingTokens.xxxl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction Signing",
                        fontSize = TypographyTokens.title3,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .size(SpacingTokens.xxxl)
                            .background(SemanticColors.backgroundSystem, CircleShape)
                            .clickable {
                                if (!uiState.isLoading) {
                                    onDismiss()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(DimensionTokens.Icon.sizeLarge),
                            tint = SemanticColors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(SpacingTokens.xxl))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.s),
                ) {
                    StatusBadge(status = transaction.status)

                    Spacer(modifier = Modifier.height(SpacingTokens.s))

                    Text(
                        text = "Transaction ID",
                        fontSize = TypographyTokens.caption,
                        color = SemanticColors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = transaction.id.take(24) + "...",
                        fontSize = TypographyTokens.subheadline,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                    )

                    transaction.onChain?.txId?.let { txId ->
                        Spacer(modifier = Modifier.height(SpacingTokens.s))
                        Text(
                            text = "Transaction Hash",
                            fontSize = TypographyTokens.caption,
                            color = SemanticColors.textPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = txId.take(24) + "...",
                            fontSize = TypographyTokens.subheadline,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(SpacingTokens.xxl))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.l),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (transaction.status == TransactionStatus.AWAITING_APPROVAL) {
                        com.crossmint.kotlin.quickstart.ui.components.PrimaryButton(
                            text = "Sign Transaction",
                            onClick = { viewModel.signTransaction(transaction.id) },
                            isLoading = uiState.isLoading,
                            isDisabled = uiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (!uiState.isLoading) {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SemanticColors.textPrimary
                        ),
                        shape = RoundedCornerShape(DimensionTokens.CornerRadius.s)
                    ) {
                        Text(
                            text = "Dismiss",
                            modifier = Modifier.padding(vertical = SpacingTokens.xs)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: TransactionStatus) {
    val (backgroundColor, textColor) = when (status) {
        TransactionStatus.AWAITING_APPROVAL -> SemanticColors.statusOrange to Color.White
        TransactionStatus.PENDING -> SemanticColors.statusPurple to Color.White
        TransactionStatus.SUCCESS -> SemanticColors.statusGreen to Color.White
        TransactionStatus.FAILED -> SemanticColors.statusRed to Color.White
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(DimensionTokens.CornerRadius.s))
            .padding(horizontal = SpacingTokens.m, vertical = SpacingTokens.xs)
    ) {
        Text(
            text = status.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            fontSize = TypographyTokens.small,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
