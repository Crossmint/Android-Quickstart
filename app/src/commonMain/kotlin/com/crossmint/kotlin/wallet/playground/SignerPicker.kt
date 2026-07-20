package com.crossmint.kotlin.wallet.playground

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossmint.kotlin.utility.truncateLocator
import com.crossmint.kotlin.wallet.SignerOption
import com.crossmint.kotlin.wallet.WalletUiState

@Composable
internal fun SignerPickerSection(
    uiState: WalletUiState,
    onSignerSelected: (Int) -> Unit,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Text(
        "Sign with",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Box {
        OutlinedButton(
            onClick = { dropdownExpanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val sel = uiState.selectedSigner
                if (sel != null) {
                    Text(
                        sel.type.replaceFirstChar { it.titlecase() } + if (sel.isAdmin) " (Recovery)" else "",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                    Text(
                        sel.locator.truncateLocator(),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                } else {
                    Text("Select signer", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }
            Icon(Icons.Filled.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false },
        ) {
            uiState.availableSigners.forEachIndexed { index, signer ->
                DropdownMenuItem(
                    text = { SignerDropdownItem(signer, isSelected = index == uiState.selectedSignerIndex) },
                    onClick = {
                        onSignerSelected(index)
                        dropdownExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun SignerDropdownItem(
    signer: SignerOption,
    isSelected: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                signer.type.replaceFirstChar { it.titlecase() } + if (signer.isAdmin) " (Recovery)" else "",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                signer.locator.truncateLocator(),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
        }
        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Filled.CheckCircle,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
