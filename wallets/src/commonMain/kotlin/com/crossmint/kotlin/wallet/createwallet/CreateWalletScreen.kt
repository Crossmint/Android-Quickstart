package com.crossmint.kotlin.wallet.createwallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossmint.kotlin.signers.DelegatedSigner
import com.crossmint.kotlin.signers.SignerType
import com.crossmint.kotlin.types.Chain
import com.crossmint.kotlin.wallet.CreateWalletViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWalletScreen(
    chain: Chain,
    chainDisplayName: String,
    userEmail: String?,
    viewModel: CreateWalletViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isCreating = uiState.isCreating
    val errorMessage = uiState.errorMessage
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var selectedAdminSignerType by remember { mutableStateOf(AdminSignerType.EMAIL) }
    var adminEmail by remember { mutableStateOf(userEmail ?: "") }
    var adminPhone by remember { mutableStateOf("") }

    val delegatedSigners = remember { mutableStateListOf<DelegatedSignerEntry>() }

    var showSignerTypePicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val isValid =
        when (selectedAdminSignerType) {
            AdminSignerType.EMAIL -> adminEmail.isNotBlank() && adminEmail.contains("@")
            AdminSignerType.PHONE -> adminPhone.isNotBlank()
            AdminSignerType.API_KEY -> true
        }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Create Wallet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = chainDisplayName,
                            fontSize = 12.sp,
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = { viewModel.cancel() }) {
                        Text("Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val adminSigner =
                                when (selectedAdminSignerType) {
                                    AdminSignerType.EMAIL -> SignerType.Email(adminEmail)
                                    AdminSignerType.PHONE -> SignerType.Phone(adminPhone)
                                    AdminSignerType.API_KEY -> SignerType.ApiKey
                                }
                            val deviceEntry = delegatedSigners.firstOrNull { it.type == DelegatedSignerType.DEVICE }
                            val delegated =
                                delegatedSigners.mapNotNull { entry ->
                                    when (entry.type) {
                                        DelegatedSignerType.EMAIL ->
                                            if (entry.email.isNotBlank()) DelegatedSigner.Email(entry.email) else null
                                        DelegatedSignerType.PHONE ->
                                            if (entry.phone.isNotBlank()) DelegatedSigner.Phone(entry.phone) else null
                                        DelegatedSignerType.EXTERNAL_WALLET ->
                                            if (entry.address.isNotBlank()) {
                                                DelegatedSigner.ExternalWallet(
                                                    entry.address,
                                                )
                                            } else {
                                                null
                                            }
                                        DelegatedSignerType.DEVICE -> null
                                        DelegatedSignerType.PASSKEY -> null
                                    }
                                }
                            viewModel.createWallet(chain, adminSigner, delegated, deviceSigner = deviceEntry != null)
                        },
                        enabled = isValid && !isCreating,
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                "Create",
                                color =
                                    if (isValid) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Admin Signer",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "The primary signer that controls this wallet",
                fontSize = 12.sp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            AdminSignerCard(
                selectedType = selectedAdminSignerType,
                onTypeChange = { selectedAdminSignerType = it },
                email = adminEmail,
                onEmailChange = { adminEmail = it },
                phone = adminPhone,
                onPhoneChange = { adminPhone = it },
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Delegated Signers",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Additional signers that can authorize transactions",
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            delegatedSigners.forEachIndexed { index, entry ->
                DelegatedSignerCard(
                    entry = entry,
                    onUpdate = { updated -> delegatedSigners[index] = updated },
                    onRemove = { delegatedSigners.removeAt(index) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            AddSignerButton(
                onClick = { showSignerTypePicker = true },
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    fontSize = 14.sp,
                    color = Color.Red,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showSignerTypePicker) {
        ModalBottomSheet(
            onDismissRequest = { showSignerTypePicker = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
            ) {
                Text(
                    text = "Add Delegated Signer",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(16.dp))

                DelegatedSignerType.entries.forEach { type ->
                    // Only allow one DEVICE signer
                    val alreadyAdded =
                        type == DelegatedSignerType.DEVICE &&
                            delegatedSigners.any { it.type == DelegatedSignerType.DEVICE }
                    if (!alreadyAdded) {
                        SignerTypeOption(
                            type = type,
                            onClick = {
                                delegatedSigners.add(DelegatedSignerEntry(type = type))
                                scope.launch {
                                    sheetState.hide()
                                    showSignerTypePicker = false
                                }
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
