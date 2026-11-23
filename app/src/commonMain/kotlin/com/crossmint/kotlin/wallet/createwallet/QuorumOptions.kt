package com.crossmint.kotlin.wallet.createwallet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun QuorumOptions(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    threshold: Int,
    memberCount: Int,
    onThresholdChange: (Int) -> Unit,
    firstSignerActsAlone: Boolean = false,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Make these signers a quorum", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    if (firstSignerActsAlone) {
                        "The signers approve together. On EVM, the first signer can also act on its own"
                    } else {
                        "The signers approve together instead of on their own"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.semantics { testTag = "recovery-quorum-switch" },
            )
        }
        if (enabled) {
            QuorumThreshold(threshold, memberCount, onThresholdChange, testTagPrefix = "recovery-quorum")
            if (memberCount < 2) {
                Text("A quorum needs at least 2 signers", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun QuorumThreshold(
    threshold: Int,
    memberCount: Int,
    onThresholdChange: (Int) -> Unit,
    testTagPrefix: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = { onThresholdChange(threshold - 1) },
            enabled = threshold > 1,
        ) { Text("-") }
        Text("$threshold of $memberCount must approve", fontSize = 14.sp)
        TextButton(
            onClick = { onThresholdChange(threshold + 1) },
            enabled = threshold < memberCount,
            modifier = Modifier.semantics { testTag = "$testTagPrefix-threshold-increase" },
        ) { Text("+") }
    }
}
