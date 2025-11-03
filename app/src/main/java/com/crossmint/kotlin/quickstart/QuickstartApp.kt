package com.crossmint.kotlin.quickstart

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crossmint.kotlin.compose.CrossmintSDKProvider
import com.crossmint.kotlin.compose.LocalCrossmintSDK
import com.crossmint.kotlin.quickstart.auth.CrossmintAuthViewModel
import com.crossmint.kotlin.quickstart.ui.*
import kotlinx.coroutines.delay

@Composable
fun QuickstartApp() {
    CrossmintSDKProvider
        .Builder(BuildConfig.CROSSMINT_API_KEY)
        .developmentMode()
        .onTEERequired { onOTPSubmit, onDismiss ->
            OTPDialog(
                onOTPSubmit = onOTPSubmit,
                onDismiss = onDismiss,
            )
        }.build {
            QuickstartContent()
        }
}

enum class AppScreen {
    SPLASH, SIGN_IN, DASHBOARD
}

@Composable
private fun QuickstartContent() {
    val sdk = LocalCrossmintSDK.current
    val crossmintAuthViewModel: CrossmintAuthViewModel =
        viewModel {
            CrossmintAuthViewModel(sdk.authManager)
        }
    val dashboardViewModel: DashboardViewModel =
        viewModel {
            DashboardViewModel(sdk.crossmintWallets, sdk.authManager)
        }

    val crossmintUiState by crossmintAuthViewModel.uiState.collectAsState()
    val dashboardUiState by dashboardViewModel.uiState.collectAsState()

    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
    var showSplash by remember { mutableStateOf(true) }
    var showOTPBottomSheet by remember { mutableStateOf(false) }
    var showTransactionSigningModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500)
        showSplash = false
    }

    LaunchedEffect(crossmintUiState.otpSent) {
        showOTPBottomSheet = crossmintUiState.otpSent
    }

    LaunchedEffect(dashboardUiState.hasTransaction) {
        showTransactionSigningModal = dashboardUiState.hasTransaction
    }

    LaunchedEffect(showSplash, crossmintUiState.isAuthenticated) {
        currentScreen = when {
            showSplash -> AppScreen.SPLASH
            crossmintUiState.isAuthenticated -> AppScreen.DASHBOARD
            else -> AppScreen.SIGN_IN
        }
    }

    BackHandler(enabled = showOTPBottomSheet) {
        crossmintAuthViewModel.changeEmail()
        showOTPBottomSheet = false
    }

    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                AppScreen.SPLASH -> {
                    SplashScreen(
                        isLoading = true
                    )
                }

                AppScreen.SIGN_IN -> {
                    CrossmintOTPEmailScreen(
                        viewModel = crossmintAuthViewModel,
                        onBackClick = {},
                        showBackButton = false,
                    )
                }

                AppScreen.DASHBOARD -> {
                    DashboardScreen(
                        dashboardViewModel = dashboardViewModel,
                        onLogout = {
                            crossmintAuthViewModel.signOut()
                            dashboardViewModel.reset()
                        },
                        onCreateWallet = {
                            // Wallet creation is handled by DashboardViewModel
                            // This callback is for parent notification (e.g., analytics, logging)
                        }
                    )
                }
            }
        }

        if (showOTPBottomSheet) {
            CrossmintOTPVerificationBottomSheet(
                viewModel = crossmintAuthViewModel,
                onDismiss = {
                    showOTPBottomSheet = false
                }
            )
        }

        if (showTransactionSigningModal) {
            TransactionSigningBottomSheet(
                viewModel = dashboardViewModel,
                onDismiss = {
                    showTransactionSigningModal = false
                    dashboardViewModel.clearTransaction()
                }
            )
        }
    }
}
