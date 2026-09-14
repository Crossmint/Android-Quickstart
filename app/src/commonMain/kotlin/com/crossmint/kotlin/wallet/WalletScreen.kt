package com.crossmint.kotlin.wallet

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.crossmint.crossmintdemoapp.generated.resources.Res
import com.crossmint.crossmintdemoapp.generated.resources.ic_crossmint
import com.crossmint.crossmintdemoapp.generated.resources.ic_eth
import com.crossmint.crossmintdemoapp.generated.resources.ic_solana
import com.crossmint.crossmintdemoapp.generated.resources.ic_stellar
import com.crossmint.kotlin.Deps
import com.crossmint.kotlin.Routes
import com.crossmint.kotlin.auth.AuthMethod
import com.crossmint.kotlin.auth.crossmint.CrossmintAuthViewModel
import com.crossmint.kotlin.compose.LocalCrossmintSDK
import com.crossmint.kotlin.utility.exposeTestTags
import com.crossmint.kotlin.utility.truncateLocator
import com.crossmint.kotlin.wallet.playground.ActivitySheet
import com.crossmint.kotlin.wallet.playground.SignersSheet
import com.crossmint.kotlin.wallet.playground.SigningSheet
import com.crossmint.kotlin.wallet.playground.TransferSheet
import com.crossmint.kotlin.wallet.playground.rememberPasskeyCreator
import com.crossmint.kotlin.wallet.subcomponents.WalletTransactionErrorDialog
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    navController: NavHostController,
    authMethod: AuthMethod,
    crossmintViewModel: CrossmintAuthViewModel? = null,
    walletViewModel: WalletViewModel,
    onCreateWalletClick: () -> Unit = {},
) {
    val sdk = LocalCrossmintSDK.current
    val uiState by walletViewModel.uiState.collectAsState()
    val selectedChain = walletViewModel.selectedChain.value
    val scope = rememberCoroutineScope()

    val crossmintAuthState = crossmintViewModel?.uiState?.collectAsState()
    val userEmail = crossmintAuthState?.value?.userEmail ?: Deps.authEmail

    var chainPickerExpanded by remember { mutableStateOf(false) }
    var userMenuExpanded by remember { mutableStateOf(false) }
    var showTransferSheet by remember { mutableStateOf(false) }
    var showSignersSheet by remember { mutableStateOf(false) }
    var showActivitySheet by remember { mutableStateOf(false) }
    var showSigningSheet by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val passkeyCreator = rememberPasskeyCreator()

    LaunchedEffect(Unit) {
        walletViewModel.sessionExpired.collect {
            when (authMethod) {
                AuthMethod.CROSSMINT_OTP -> crossmintViewModel?.signOut()
                AuthMethod.BYOA -> sdk.logout()
            }
            walletViewModel.reset()
            Deps.authEmail = null
            navController.navigate(Routes.AuthMethodSelection) {
                popUpTo(Routes.AuthMethodSelection) { inclusive = true }
            }
        }
    }

    LaunchedEffect(uiState.wallet?.address) {
        if (uiState.wallet != null) walletViewModel.fetchBalances()
    }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) isRefreshing = false
    }

    if (uiState.hasTransactionError) {
        WalletTransactionErrorDialog(walletViewModel, uiState)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                navigationIcon = {
                    Box {
                        IconButton(
                            onClick = { chainPickerExpanded = true },
                            modifier = Modifier.semantics { testTag = "chain-picker-button" },
                        ) {
                            Image(
                                painter = painterResource(chainDrawable(selectedChain)),
                                contentDescription = selectedChain.displayName,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = chainPickerExpanded,
                            onDismissRequest = { chainPickerExpanded = false },
                            // DropdownMenu hosts its own window: re-expose testTags for Maestro.
                            modifier = Modifier.exposeTestTags(),
                        ) {
                            SupportedChain.entries.forEach { chain ->
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Image(
                                            painter = painterResource(chainDrawable(chain)),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    },
                                    text = {
                                        Text(
                                            chain.displayName,
                                            fontWeight =
                                                if (chain ==
                                                    selectedChain
                                                ) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Normal
                                                },
                                        )
                                    },
                                    onClick = {
                                        walletViewModel.selectChain(chain)
                                        chainPickerExpanded = false
                                    },
                                    modifier =
                                        Modifier.semantics {
                                            testTag = "chain-option-${chain.testId}"
                                        },
                                )
                            }
                        }
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(Res.drawable.ic_crossmint),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("crossmint", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { userMenuExpanded = true },
                            modifier = Modifier.semantics { testTag = "logout-button" },
                        ) {
                            Icon(
                                Icons.Filled.AccountCircle,
                                "Account",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = userMenuExpanded,
                            onDismissRequest = { userMenuExpanded = false },
                        ) {
                            if (userEmail != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            userEmail,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    onClick = {},
                                    enabled = false,
                                )
                                HorizontalDivider()
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Sign Out",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium,
                                    )
                                },
                                onClick = {
                                    userMenuExpanded = false
                                    scope.launch {
                                        when (authMethod) {
                                            AuthMethod.CROSSMINT_OTP -> crossmintViewModel?.signOut()
                                            AuthMethod.BYOA -> sdk.logout()
                                        }
                                        walletViewModel.reset()
                                        Deps.authEmail = null
                                        navController.navigate(Routes.AuthMethodSelection) {
                                            popUpTo(Routes.AuthMethodSelection) { inclusive = true }
                                        }
                                    }
                                },
                                modifier = Modifier.semantics { testTag = "logout-button" },
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                walletViewModel.fetchWallet(selectedChain.chain)
            },
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                SectionHeader("Wallet")
                Spacer(modifier = Modifier.height(8.dp))
                WalletCard(
                    uiState = uiState,
                    selectedChain = selectedChain,
                    userEmail = userEmail,
                    onCreateWallet = onCreateWalletClick,
                    onAddTestFunds = { walletViewModel.fundTestTokens() },
                )

                if (uiState.hasWallet) {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader("Features")
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column {
                            FeatureListItem(
                                icon = { Icon(Icons.Filled.Key, null, tint = MaterialTheme.colorScheme.primary) },
                                title = "Signers",
                                onClick = {
                                    walletViewModel.clearAddSignerState()
                                    showSignersSheet = true
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            FeatureListItem(
                                icon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                title = "Transfer",
                                onClick = {
                                    walletViewModel.clearTransaction()
                                    walletViewModel.clearTransactionError()
                                    showTransferSheet = true
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            FeatureListItem(
                                icon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                title = "Activity",
                                onClick = {
                                    walletViewModel.clearTransfers()
                                    showActivitySheet = true
                                },
                            )
                            if (selectedChain == SupportedChain.EVM) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 56.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                                FeatureListItem(
                                    icon = { Icon(Icons.Filled.Draw, null, tint = MaterialTheme.colorScheme.primary) },
                                    title = "Signing",
                                    onClick = {
                                        walletViewModel.clearSignature()
                                        showSigningSheet = true
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        } // PullToRefreshBox

        if (showTransferSheet) {
            TransferSheet(
                walletViewModel = walletViewModel,
                uiState = uiState,
                chain = selectedChain,
                onDismiss = { showTransferSheet = false },
            )
        }
        if (showSignersSheet) {
            SignersSheet(
                walletViewModel = walletViewModel,
                uiState = uiState,
                passkeyCreator = passkeyCreator,
                onDismiss = { showSignersSheet = false },
            )
        }
        if (showActivitySheet) {
            ActivitySheet(
                walletViewModel = walletViewModel,
                uiState = uiState,
                onDismiss = { showActivitySheet = false },
            )
        }
        if (showSigningSheet) {
            SigningSheet(
                walletViewModel = walletViewModel,
                uiState = uiState,
                onDismiss = { showSigningSheet = false },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun WalletCard(
    uiState: WalletUiState,
    selectedChain: SupportedChain,
    userEmail: String?,
    onCreateWallet: () -> Unit,
    onAddTestFunds: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
            uiState.hasWallet -> {
                val wallet = uiState.wallet!!
                val clipboardManager = LocalClipboardManager.current
                val usdxmBalance =
                    uiState.balances
                        ?.tokens
                        ?.find { it.symbol.equals("usdxm", ignoreCase = true) }
                        ?.let { "${it.amount} USDXM" }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    WalletDetailRow(
                        label = "Balance",
                        value =
                            when {
                                uiState.isLoadingBalances -> "Loading…"
                                usdxmBalance != null -> usdxmBalance
                                else -> "—"
                            },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Address", fontSize = 15.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            wallet.address.truncateLocator(),
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.semantics { testTag = "wallet-address-label" },
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(wallet.address)) },
                            modifier = Modifier.size(24.dp).semantics { testTag = "wallet-address-copy-button" },
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                "Copy",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    WalletDetailRow(label = "Owner", value = userEmail ?: "—")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    WalletDetailRow(label = "Network", value = selectedChain.networkName)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    TextButton(
                        onClick = onAddTestFunds,
                        enabled = !uiState.isLoadingBalances,
                        modifier = Modifier.padding(vertical = 2.dp).semantics { testTag = "fund-button" },
                    ) {
                        Text(
                            "Add Test Funds",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
            uiState.isEmpty -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No wallet found", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onCreateWallet) {
                        Text(
                            "Create Wallet",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            uiState.hasError -> {
                Text(
                    uiState.errorMessage ?: "Error loading wallet",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun WalletDetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FeatureListItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, fontSize = 16.sp) },
        leadingContent = icon,
        trailingContent = {
            Icon(
                Icons.Filled.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(20.dp),
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private fun chainDrawable(chain: SupportedChain): DrawableResource =
    when (chain) {
        SupportedChain.EVM -> Res.drawable.ic_eth
        SupportedChain.SOLANA -> Res.drawable.ic_solana
        SupportedChain.STELLAR -> Res.drawable.ic_stellar
    }
