package com.crossmint.kotlin.wallet.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Smartphone
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossmint.kotlin.signers.DelegatedSigner
import com.crossmint.kotlin.types.SignerData
import com.crossmint.kotlin.utility.exposeTestTags
import com.crossmint.kotlin.utility.truncateLocator
import com.crossmint.kotlin.wallet.WalletUiState
import com.crossmint.kotlin.wallet.WalletViewModel
import com.crossmint.kotlin.wallet.createwallet.DelegatedSignerType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignersSheet(
    walletViewModel: WalletViewModel,
    uiState: WalletUiState,
    passkeyCreator: (suspend (name: String) -> DelegatedSigner.Passkey?)? = null,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddSignerSheet by remember { mutableStateOf(false) }
    var removingLocator by remember { mutableStateOf<String?>(null) }
    var removedLocators by remember { mutableStateOf(setOf<String>()) }

    val wallet = uiState.wallet
    val delegatedSigners =
        uiState.availableSigners
            .filter { !it.isAdmin && it.locator !in removedLocators }

    LaunchedEffect(uiState.isModifyingSigners) {
        if (!uiState.isModifyingSigners) removingLocator = null
    }

    LaunchedEffect(uiState.signerOperationError) {
        val locator = removingLocator
        if (uiState.signerOperationError != null && locator != null) {
            removedLocators = removedLocators - locator
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // ModalBottomSheet hosts its own window: re-expose testTags for Maestro.
        modifier = Modifier.exposeTestTags(),
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Signers", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text("Done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 40.dp),
        ) {
            if (wallet != null) {
                SignerSectionLabel("Recovery")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    val admin = wallet.config.adminSigner
                    ListItem(
                        headlineContent = {
                            Text(signerTypeLabel(admin), fontWeight = FontWeight.Medium)
                        },
                        supportingContent = {
                            Text(
                                admin.locator.truncateLocator(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                            )
                        },
                        leadingContent = {
                            Icon(
                                signerIcon(admin),
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        trailingContent = {
                            Text(
                                "Recovery",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            SignerSectionLabel("Signers")
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth().semantics { testTag = "signers-list" },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                if (delegatedSigners.isEmpty()) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.People,
                                null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.outlineVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No signers",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                            )
                        }
                    }
                } else {
                    Column {
                        delegatedSigners.forEachIndexed { index, signer ->
                            val dismissState =
                                rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart && removingLocator == null) {
                                            removingLocator = signer.locator
                                            removedLocators = removedLocators + signer.locator
                                            walletViewModel.removeSignerFromWallet(signer.locator)
                                            true
                                        } else {
                                            false
                                        }
                                    },
                                )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.error),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            "Remove",
                                            tint = MaterialTheme.colorScheme.onError,
                                            modifier =
                                                Modifier
                                                    .padding(end = 20.dp)
                                                    .size(22.dp),
                                        )
                                    }
                                },
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            signer.type.replaceFirstChar { it.titlecase() },
                                            fontWeight = FontWeight.Medium,
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            signer.locator.truncateLocator(),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            modifier = Modifier.semantics { testTag = "signer-$index-locator" },
                                        )
                                    },
                                    leadingContent = {
                                        Icon(
                                            delegatedSignerIcon(signer.type),
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    },
                                    trailingContent = {
                                        if (removingLocator == signer.locator) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                strokeWidth = 2.dp,
                                            )
                                        }
                                    },
                                    colors =
                                        ListItemDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                        ),
                                    modifier = Modifier.semantics { testTag = "signer-$index" },
                                )
                            }

                            if (index < delegatedSigners.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 56.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.signerOperationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    uiState.signerOperationError,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            val canAddSigner = wallet != null && !uiState.isModifyingSigners
            Card(
                modifier = Modifier.fillMaxWidth().semantics { testTag = "signers-add-button" },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            "Add Signer",
                            color =
                                if (canAddSigner) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Add,
                            null,
                            tint =
                                if (canAddSigner) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                        )
                    },
                    trailingContent = {
                        if (uiState.isModifyingSigners) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier =
                        Modifier.clickable(
                            enabled = canAddSigner,
                            onClick = {
                                walletViewModel.clearAddSignerState()
                                showAddSignerSheet = true
                            },
                        ),
                )
            }
        }
    }

    if (showAddSignerSheet) {
        AddSignerSheet(
            compatibleTypes =
                walletViewModel.selectedChain.value.compatibleDelegatedSignerTypes(
                    passkeyCreator != null,
                ),
            hasDeviceSigner = uiState.deviceSignerSecurityLevel != null,
            passkeyCreator = passkeyCreator,
            onDismiss = { showAddSignerSheet = false },
            onAddDevice = {
                showAddSignerSheet = false
                walletViewModel.addDeviceSignerToWallet()
            },
            onAddSigner = { signer ->
                showAddSignerSheet = false
                walletViewModel.addDelegatedSignerToWallet(signer)
            },
        )
    }
}

@Composable
private fun SignerSectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSignerSheet(
    compatibleTypes: List<DelegatedSignerType>,
    hasDeviceSigner: Boolean,
    passkeyCreator: (suspend (name: String) -> DelegatedSigner.Passkey?)?,
    onDismiss: () -> Unit,
    onAddDevice: () -> Unit,
    onAddSigner: (DelegatedSigner) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val availableTypes = compatibleTypes.filter { !(it == DelegatedSignerType.DEVICE && hasDeviceSigner) }
    var selectedType by remember(availableTypes) {
        mutableStateOf(
            availableTypes.firstOrNull { it == DelegatedSignerType.DEVICE } ?: availableTypes.firstOrNull(),
        )
    }
    var address by remember { mutableStateOf("") }
    var passkeyName by remember { mutableStateOf("") }
    var isCreatingPasskey by remember { mutableStateOf(false) }
    var passkeyError by remember { mutableStateOf<String?>(null) }

    val isValid =
        when (selectedType) {
            DelegatedSignerType.EXTERNAL_WALLET -> address.isNotBlank()
            DelegatedSignerType.PASSKEY -> passkeyName.isNotBlank()
            else -> true
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // ModalBottomSheet hosts its own window: re-expose testTags for Maestro.
        modifier = Modifier.exposeTestTags(),
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Add Signer", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 40.dp),
        ) {
            SignerSectionLabel("Type")
            Spacer(modifier = Modifier.height(6.dp))
            SignerTypeSection(
                availableTypes = availableTypes,
                selectedType = selectedType,
                onTypeSelected = { type ->
                    selectedType = type
                    address = ""
                },
            )

            SignerInputSection(
                selectedType = selectedType,
                address = address,
                onAddressChange = { address = it },
                passkeyName = passkeyName,
                onPasskeyNameChange = {
                    passkeyName = it
                    passkeyError = null
                },
                passkeyError = passkeyError,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    when (selectedType) {
                        DelegatedSignerType.PASSKEY -> {
                            scope.launch {
                                isCreatingPasskey = true
                                passkeyError = null
                                try {
                                    val passkey = passkeyCreator?.invoke(passkeyName.trim())
                                    isCreatingPasskey = false
                                    if (passkey != null) {
                                        sheetState.hide()
                                        onDismiss()
                                        onAddSigner(passkey)
                                    }
                                } catch (e: Exception) {
                                    isCreatingPasskey = false
                                    passkeyError = e.message ?: "Passkey creation failed"
                                }
                            }
                        }
                        else -> {
                            scope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                            when (selectedType) {
                                DelegatedSignerType.DEVICE -> onAddDevice()
                                DelegatedSignerType.EXTERNAL_WALLET ->
                                    onAddSigner(DelegatedSigner.ExternalWallet(address.trim()))
                                else -> {}
                            }
                        }
                    }
                },
                enabled = isValid && !isCreatingPasskey,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                if (isCreatingPasskey) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creating passkey…", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                } else {
                    Text("Add", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun SignerTypeSection(
    availableTypes: List<DelegatedSignerType>,
    selectedType: DelegatedSignerType?,
    onTypeSelected: (DelegatedSignerType) -> Unit,
) {
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Box {
            ListItem(
                headlineContent = {
                    Text(selectedType?.displayName ?: "Select type", fontWeight = FontWeight.Medium)
                },
                trailingContent = {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.clickable { typeDropdownExpanded = true },
            )
            DropdownMenu(
                expanded = typeDropdownExpanded,
                onDismissRequest = { typeDropdownExpanded = false },
            ) {
                availableTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.displayName) },
                        onClick = {
                            onTypeSelected(type)
                            typeDropdownExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SignerInputSection(
    selectedType: DelegatedSignerType?,
    address: String,
    onAddressChange: (String) -> Unit,
    passkeyName: String,
    onPasskeyNameChange: (String) -> Unit,
    passkeyError: String?,
) {
    when (selectedType) {
        DelegatedSignerType.EXTERNAL_WALLET -> {
            Spacer(modifier = Modifier.height(20.dp))
            SignerSectionLabel("Address")
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                OutlinedTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    placeholder = {
                        Text(
                            "0x…",
                            color = MaterialTheme.colorScheme.outlineVariant,
                            fontFamily = FontFamily.Monospace,
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                )
            }
        }
        DelegatedSignerType.DEVICE -> {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Registers this device's hardware-backed key as a signer on the wallet.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        DelegatedSignerType.PASSKEY -> {
            Spacer(modifier = Modifier.height(20.dp))
            SignerSectionLabel("Name")
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                OutlinedTextField(
                    value = passkeyName,
                    onValueChange = onPasskeyNameChange,
                    placeholder = { Text("e.g. My Passkey", color = MaterialTheme.colorScheme.outlineVariant) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                )
            }
            if (passkeyError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    passkeyError,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        else -> {}
    }
}

private fun signerTypeLabel(signer: SignerData): String =
    when (signer) {
        is SignerData.Email -> "Email"
        is SignerData.Phone -> "Phone"
        is SignerData.Passkey -> "Passkey"
        is SignerData.ApiKey -> "API Key"
        is SignerData.ExternalWallet -> "External Wallet"
        is SignerData.Device -> "Device"
    }

private fun signerIcon(signer: SignerData): ImageVector =
    when (signer) {
        is SignerData.Email -> Icons.Filled.Email
        is SignerData.Phone -> Icons.Filled.Phone
        is SignerData.Device -> Icons.Filled.Smartphone
        else -> Icons.Filled.Key
    }

private fun delegatedSignerIcon(type: String): ImageVector =
    when (type) {
        "email" -> Icons.Filled.Email
        "phone" -> Icons.Filled.Phone
        "device" -> Icons.Filled.Smartphone
        else -> Icons.Filled.Key
    }
