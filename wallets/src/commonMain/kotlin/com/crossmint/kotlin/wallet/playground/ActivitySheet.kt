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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossmint.kotlin.types.Transfer
import com.crossmint.kotlin.types.TransferType
import com.crossmint.kotlin.utility.exposeTestTags
import com.crossmint.kotlin.utility.formatTimestamp
import com.crossmint.kotlin.utility.truncateLocator
import com.crossmint.kotlin.wallet.WalletUiState
import com.crossmint.kotlin.wallet.WalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitySheet(
    walletViewModel: WalletViewModel,
    uiState: WalletUiState,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { walletViewModel.loadTransfers() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            Text("Activity", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoadingTransfers -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                uiState.transfersError != null -> {
                    Text(
                        uiState.transfersError,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                uiState.transfers.isEmpty() -> {
                    Text("No transfers found.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    LazyColumn(modifier = Modifier.semantics { testTag = "activity-list" }) {
                        itemsIndexed(uiState.transfers) { index, transfer ->
                            Column(modifier = Modifier.semantics { testTag = "activity-item-$index" }) {
                                TransferRow(transfer = transfer, index = index)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferRow(
    transfer: Transfer,
    index: Int,
) {
    val isOutgoing = transfer.type == TransferType.OUTGOING
    val counterparty =
        if (isOutgoing) transfer.toAddress.truncateLocator() else transfer.fromAddress.truncateLocator()

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isOutgoing) Icons.Filled.ArrowCircleUp else Icons.Filled.ArrowCircleDown,
            contentDescription = null,
            tint = if (isOutgoing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isOutgoing) "Sent to $counterparty" else "Received from $counterparty",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                formatTimestamp(transfer.timestamp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${if (isOutgoing) "-" else "+"}${transfer.amount}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isOutgoing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.semantics { testTag = "activity-item-$index-amount" },
            )
            Text(
                transfer.tokenSymbol ?: "",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { testTag = "activity-item-$index-token" },
            )
        }
    }
}
