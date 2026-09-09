package com.crossmint.kotlin.wallet.subcomponents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossmint.kotlin.types.TransactionError
import com.crossmint.kotlin.wallet.WalletUiState
import com.crossmint.kotlin.wallet.WalletViewModel

@Composable
fun WalletTransactionErrorDialog(
    walletViewModel: WalletViewModel,
    walletUiState: WalletUiState,
) {
    AlertDialog(
        onDismissRequest = {
            walletViewModel.clearTransactionError()
        },
        icon = {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp),
            )
        },
        title = {
            Text(
                "Transaction Failed",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            walletUiState.transactionError?.let { error ->
                Column {
                    Text(
                        text = error.message,
                        fontSize = 14.sp,
                    )

                    // Show additional error details based on error type
                    when (error) {
                        is TransactionError.TransactionCreationFailed -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text =
                                    "The transaction could not be created. " +
                                        "Please check your inputs and try again.",
                                fontSize = 12.sp,
                            )
                        }
                        is TransactionError.TransactionNotFound -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "The requested transaction was not found on the blockchain.",
                                fontSize = 12.sp,
                            )
                        }
                        is TransactionError.Unauthorized -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Authentication required. Please sign in again.",
                                fontSize = 12.sp,
                            )
                        }
                        is TransactionError.RateLimited -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Too many requests. Please wait a moment and try again.",
                                fontSize = 12.sp,
                            )
                        }
                        is TransactionError.ServiceNotInitialized -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Service initialization error. Please restart the app.",
                                fontSize = 12.sp,
                            )
                        }
                        else -> {
                            // Generic error message already shown
                        }
                    }

                    // Show error code/type
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Error Type: ${error::class.simpleName}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    walletViewModel.clearTransactionError()
                },
            ) {
                Text("OK")
            }
        },
    )
}
