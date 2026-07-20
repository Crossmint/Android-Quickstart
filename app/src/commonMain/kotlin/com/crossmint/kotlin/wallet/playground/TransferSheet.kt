package com.crossmint.kotlin.wallet.playground

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossmint.kotlin.types.Balances
import com.crossmint.kotlin.types.TokenBalance
import com.crossmint.kotlin.types.TransactionStatus
import com.crossmint.kotlin.utility.exposeTestTags
import com.crossmint.kotlin.utility.truncateLocator
import com.crossmint.kotlin.wallet.SupportedChain
import com.crossmint.kotlin.wallet.WalletUiState
import com.crossmint.kotlin.wallet.WalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferSheet(
    walletViewModel: WalletViewModel,
    uiState: WalletUiState,
    chain: SupportedChain,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tokens = chain.transferTokens
    var selectedToken by remember(chain) { mutableStateOf(tokens.find { it.displayName == "USDXM" } ?: tokens.first()) }
    var recipient by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var tokenDropdownExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = {
            walletViewModel.clearTransaction()
            walletViewModel.clearTransactionError()
            onDismiss()
        },
        // ModalBottomSheet hosts its own window: re-expose testTags for Maestro.
        modifier = Modifier.exposeTestTags(),
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp),
        ) {
            if (uiState.isTransactionFetched && uiState.transaction != null) {
                TransferSuccessView(
                    transaction = uiState.transaction,
                    onDone = {
                        walletViewModel.clearTransaction()
                        onDismiss()
                    },
                )
            } else {
                Text("Transfer", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "Token",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    OutlinedButton(
                        onClick = { tokenDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth().semantics { testTag = "transfer-token-picker" },
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(selectedToken.displayName, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Icon(Icons.Filled.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = tokenDropdownExpanded,
                        onDismissRequest = { tokenDropdownExpanded = false },
                        // DropdownMenu hosts its own window: re-expose testTags for Maestro.
                        modifier = Modifier.exposeTestTags(),
                    ) {
                        tokens.forEach { token ->
                            DropdownMenuItem(
                                text = { Text(token.displayName) },
                                onClick = {
                                    selectedToken = token
                                    tokenDropdownExpanded = false
                                },
                                modifier =
                                    Modifier.semantics {
                                        testTag = "transfer-token-option-${token.testId}"
                                    },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text("Recipient Address") },
                    modifier = Modifier.fillMaxWidth().semantics { testTag = "transfer-recipient-input" },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth().semantics { testTag = "transfer-amount-input" },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                )

                val tokenBalance = uiState.balances?.let { findTokenBalance(it, selectedToken.displayName) }
                if (tokenBalance != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "${tokenBalance.amount} ${tokenBalance.symbol.uppercase()}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (uiState.hasMultipleSigners) {
                    Spacer(modifier = Modifier.height(14.dp))
                    SignerPickerSection(
                        uiState = uiState,
                        onSignerSelected = { walletViewModel.selectSigner(it) },
                    )
                }

                if (uiState.hasTransactionError) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        uiState.transactionError?.message ?: "Transaction failed",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { testTag = "transfer-error-label" },
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        walletViewModel.sendTransaction(recipient, selectedToken.locator, amount)
                    },
                    enabled =
                        recipient.isNotBlank() &&
                            amount.isNotBlank() &&
                            !uiState.isCreatingTransaction &&
                            uiState.hasWallet,
                    modifier = Modifier.fillMaxWidth().height(50.dp).semantics { testTag = "transfer-submit-button" },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    if (uiState.isCreatingTransaction) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sending...")
                    } else {
                        Text("Send", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

private fun findTokenBalance(
    balances: Balances,
    tokenDisplayName: String,
): TokenBalance? {
    val name = tokenDisplayName.lowercase()
    return (listOf(balances.nativeToken, balances.usdc) + balances.tokens)
        .firstOrNull { it.symbol.lowercase() == name || it.name.lowercase() == name }
}

@Composable
private fun TransferSuccessView(
    transaction: com.crossmint.kotlin.types.Transaction,
    onDone: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val statusColor =
        when (transaction.status) {
            TransactionStatus.SUCCESS -> MaterialTheme.colorScheme.primary
            TransactionStatus.PENDING -> Color(0xFFFF9800)
            TransactionStatus.AWAITING_APPROVAL -> Color(0xFF2196F3)
            TransactionStatus.FAILED -> MaterialTheme.colorScheme.error
        }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            Icons.Filled.CheckCircle,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Transaction Created",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { testTag = "transfer-success-label" },
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(transaction.status.name, fontSize = 14.sp, color = statusColor, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TxDetailRow("Transaction ID", transaction.id, useMonospace = true)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                TxDetailRow("Status", transaction.status.name, statusColor)
                transaction.onChain.txId?.let { txId ->
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tx Hash", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                txId.truncateLocator(),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(txId)) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                "Copy",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) { Text("Done", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
    }
}

@Composable
private fun TxDetailRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    useMonospace: Boolean = false,
) {
    Column {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            fontFamily = if (useMonospace) FontFamily.Monospace else FontFamily.Default,
        )
    }
}
