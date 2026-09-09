package com.crossmint.kotlin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.crossmint.kotlin.auth.AuthMethod
import com.crossmint.kotlin.auth.CrossmintAuthManager
import com.crossmint.kotlin.auth.bringyourown.BringYourOwnAuthScreen
import com.crossmint.kotlin.auth.crossmint.CrossmintAuthViewModel
import com.crossmint.kotlin.auth.crossmint.CrossmintOTPEmailScreen
import com.crossmint.kotlin.auth.crossmint.CrossmintOTPVerificationScreen
import com.crossmint.kotlin.compose.LocalCrossmintSDK
import com.crossmint.kotlin.wallet.CreateWalletViewModel
import com.crossmint.kotlin.wallet.WalletScreen
import com.crossmint.kotlin.wallet.WalletViewModel
import com.crossmint.kotlin.wallet.createwallet.CreateWalletScreen
import kotlinx.coroutines.launch

// AuthMode is defined in androidMain, so we pass it as a parameter from AppRoot
@Composable
fun DemoApp(
    authMode: Any? = null,
    isStaging: Boolean = false,
    onAuthModeChange: ((Boolean) -> Unit)? = null,
) {
    val sdk = LocalCrossmintSDK.current
    val navController = rememberNavController()

    // Determine the current auth method based on authMode
    val isBYOAMode = authMode?.toString() == "BYOA"
    val currentAuthMethod = if (isBYOAMode) AuthMethod.BYOA else AuthMethod.CROSSMINT_OTP

    // Only create CrossmintAuthViewModel when in CROSSMINT mode
    val crossmintAuthViewModel: CrossmintAuthViewModel? =
        if (!isBYOAMode && sdk.authManager is CrossmintAuthManager) {
            viewModel { CrossmintAuthViewModel(sdk.authManager as CrossmintAuthManager, navController) }
        } else {
            null
        }

    // Create WalletViewModel at app level so it can be shared between Wallet and CreateWallet routes
    val walletViewModel = remember { WalletViewModel(sdk.wallets, sdk.authManager) }

    // Create CreateWalletViewModel at app level for the CreateWallet route
    val createWalletViewModel = remember { CreateWalletViewModel(sdk.wallets, navController) }

    LaunchedEffect(Unit) {
        val isAuthed = sdk.authManager.getJWT() != null
        if (isAuthed) {
            navController.navigate(Routes.Wallet) {
                popUpTo(Routes.AuthMethodSelection) {
                    inclusive = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.AuthMethodSelection,
    ) {
        composable<Routes.AuthMethodSelection> {
            AuthMethodSelectionScreen(
                navController = navController,
                isStaging = isStaging,
                isBYOAMode = isBYOAMode,
                onAuthModeChange = onAuthModeChange,
            )
        }

        composable<Routes.CrossmintOTPEmail> {
            crossmintAuthViewModel?.let { viewModel ->
                CrossmintOTPEmailScreen(
                    viewModel = viewModel,
                    navController = navController,
                    isStaging = isStaging,
                )
            }
        }

        composable<Routes.CrossmintOTPVerification> {
            crossmintAuthViewModel?.let { viewModel ->
                CrossmintOTPVerificationScreen(
                    viewModel = viewModel,
                    isStaging = isStaging,
                    onBackClick = {
                        navController.popBackStack()
                    },
                )
            }
        }

        composable<Routes.Wallet> {
            // Refresh wallet when entering the Wallet screen
            // This ensures wallet is fetched after authentication completes
            LaunchedEffect(Unit) {
                walletViewModel.fetchWallet(walletViewModel.selectedChain.value.chain)
            }

            WalletScreen(
                navController = navController,
                authMethod = currentAuthMethod,
                crossmintViewModel = crossmintAuthViewModel,
                walletViewModel = walletViewModel,
                onCreateWalletClick = {
                    navController.navigate(Routes.CreateWallet)
                },
            )
        }

        composable<Routes.CreateWallet> {
            // Get user email from auth state for Crossmint auth, or from Deps for BYOA
            val crossmintAuthState = crossmintAuthViewModel?.uiState?.collectAsState()
            val userEmail = crossmintAuthState?.value?.userEmail ?: Deps.authEmail

            // Use the selected chain from WalletViewModel
            val selectedChain = walletViewModel.selectedChain.value

            CreateWalletScreen(
                chain = selectedChain.chain,
                chainDisplayName = selectedChain.displayName,
                userEmail = userEmail,
                viewModel = createWalletViewModel,
            )
        }

        composable<Routes.BringYourOwnAuth> {
            BringYourOwnAuthScreen(
                navController = navController,
                isStaging = isStaging,
            )
        }
    }

    DeviceSignerOTPDialog(
        walletViewModel = walletViewModel,
        createWalletViewModel = createWalletViewModel,
    )
}

@Composable
private fun DeviceSignerOTPDialog(
    walletViewModel: WalletViewModel,
    createWalletViewModel: CreateWalletViewModel,
) {
    val sdk = LocalCrossmintSDK.current
    val scope = rememberCoroutineScope()
    var shouldShowOTP by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sdk.isOTPRequired.collect { shouldShowOTP = it }
    }

    if (shouldShowOTP) {
        val walletUiState by walletViewModel.uiState.collectAsState()
        val createWalletUiState by createWalletViewModel.uiState.collectAsState()
        val signerType =
            createWalletUiState.pendingOTPSignerType
                ?: when (walletUiState.selectedSigner?.type?.lowercase()) {
                    "phone" -> OTPSignerType.PHONE
                    else -> OTPSignerType.EMAIL
                }
        OTPDialog(
            signerType = signerType,
            onOTPSubmit = { scope.launch { sdk.submit(it) } },
            onDismiss = {
                scope.launch { sdk.cancelTransaction() }
                walletViewModel.clearTransaction()
                shouldShowOTP = false
            },
        )
    }
}
