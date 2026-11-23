package com.crossmint.kotlin.wallet.playground

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossmint.kotlin.utility.exposeTestTags
import com.crossmint.kotlin.wallet.WalletUiState
import com.crossmint.kotlin.wallet.WalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproveSheet(
    walletViewModel: WalletViewModel,
    uiState: WalletUiState,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var transactionId by remember { mutableStateOf("") }

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
                Text("Approve", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(20.dp))

                if (uiState.hasMultipleSigners) {
                    SignerPickerSection(
                        uiState = uiState,
                        onSignerSelected = { walletViewModel.selectSigner(it) },
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                OutlinedTextField(
                    value = transactionId,
                    onValueChange = { transactionId = it },
                    label = { Text("Transaction ID") },
                    modifier = Modifier.fillMaxWidth().semantics { testTag = "quorum-approve-id-input" },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                )

                if (uiState.hasTransactionError) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        uiState.transactionError?.message ?: "Approval failed",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { testTag = "quorum-approve-error-label" },
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { walletViewModel.approveTransaction(transactionId) },
                    enabled = transactionId.isNotBlank() && !uiState.isCreatingTransaction,
                    modifier = Modifier.fillMaxWidth().height(50.dp).semantics { testTag = "quorum-approve-button" },
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
                        Text("Approving...")
                    } else {
                        Text("Approve", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
