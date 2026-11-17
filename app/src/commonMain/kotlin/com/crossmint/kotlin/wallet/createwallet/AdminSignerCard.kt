package com.crossmint.kotlin.wallet.createwallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossmint.kotlin.signers.OTPDeliveryChannel
import com.crossmint.kotlin.utility.exposeTestTags
import com.crossmint.kotlin.wallet.externalwallet.DemoExternalWallet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSignerCard(
    index: Int,
    types: List<AdminSignerType>,
    selectedType: AdminSignerType,
    onTypeChange: (AdminSignerType) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    phoneChannel: OTPDeliveryChannel,
    onPhoneChannelChange: (OTPDeliveryChannel) -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().semantics { testTag = "recovery-signer-$index" },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Type",
                    fontSize = 12.sp,
                )
                if (onRemove != null) {
                    IconButton(
                        onClick = onRemove,
                        modifier =
                            Modifier
                                .size(24.dp)
                                .semantics { testTag = "recovery-signer-$index-remove" },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            Box {
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true }
                            .semantics { testTag = "recovery-signer-$index-type-picker" },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(selectedType.displayName)
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Select type",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.exposeTestTags(),
                ) {
                    types.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                onTypeChange(type)
                                expanded = false
                            },
                            modifier =
                                Modifier.semantics {
                                    testTag = "recovery-signer-$index-type-option-${type.name.lowercase()}"
                                },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedType) {
                AdminSignerType.EMAIL -> {
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Email address") },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .semantics { testTag = "recovery-signer-$index-email-input" },
                        shape = RoundedCornerShape(8.dp),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                        singleLine = true,
                    )
                }
                AdminSignerType.PHONE -> {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = onPhoneChange,
                        label = { Text("Phone number") },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .semantics { testTag = "recovery-signer-$index-phone-input" },
                        shape = RoundedCornerShape(8.dp),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                        singleLine = true,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "OTP delivery",
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        OTPDeliveryChannel.entries.forEachIndexed { index, channel ->
                            SegmentedButton(
                                selected = phoneChannel == channel,
                                onClick = { onPhoneChannelChange(channel) },
                                shape =
                                    SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = OTPDeliveryChannel.entries.size,
                                    ),
                            ) { Text(channel.displayName) }
                        }
                    }
                }
                AdminSignerType.API_KEY -> {
                    Text(
                        text = "Uses the API key configured in the SDK",
                        fontSize = 14.sp,
                    )
                }
                AdminSignerType.EXTERNAL_WALLET -> {
                    Text(
                        text = "The demo app signs with a key that it keeps until the app closes",
                        fontSize = 14.sp,
                    )
                    Text(
                        text =
                            "Add one more recovery signer, for example an email. " +
                                "You need it to recover the wallet after the app restarts.",
                        fontSize = 12.sp,
                    )
                    Text(
                        text = DemoExternalWallet.address,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.semantics { testTag = "recovery-signer-$index-external-wallet-address" },
                    )
                }
            }
        }
    }
}
