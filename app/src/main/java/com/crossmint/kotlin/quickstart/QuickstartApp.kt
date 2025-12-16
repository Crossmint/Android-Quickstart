package com.crossmint.kotlin.quickstart

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import co.touchlab.kermit.Severity
import com.crossmint.kotlin.Crossmint
import com.crossmint.kotlin.auth.CrossmintAuthManager
import com.crossmint.kotlin.auth.EncryptedSharedPreferenceStorage
import com.crossmint.kotlin.auth.InsecurePersistentSessionStore
import com.crossmint.kotlin.auth.TinkWithFallbackSessionStore
import com.crossmint.kotlin.auth.createAuthManager
import com.crossmint.kotlin.compose.CrossmintNonCustodialSignerProvider
import com.crossmint.kotlin.compose.LocalCrossmintSDK
import com.crossmint.kotlin.quickstart.auth.CrossmintAuthViewModel
import com.crossmint.kotlin.quickstart.ui.CrossmintOTPEmailScreen
import com.crossmint.kotlin.quickstart.ui.CrossmintOTPVerificationBottomSheet
import com.crossmint.kotlin.quickstart.ui.DashboardScreen
import com.crossmint.kotlin.quickstart.ui.DashboardViewModel
import com.crossmint.kotlin.quickstart.ui.OTPDialog
import com.crossmint.kotlin.quickstart.ui.SplashScreen
import com.crossmint.kotlin.quickstart.ui.TransactionSigningBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QuickstartApp() {
   val context = LocalContext.current

   val crossmintAuthManager =
      remember {
         createAuthManager(
            apiKey = BuildConfig.CROSSMINT_API_KEY,
            secureStorage = TinkWithFallbackSessionStore(context),
            appContext = context,
            logLevel = Severity.Verbose
         )
      }

   CrossmintNonCustodialSignerProvider(
      sdk = Crossmint.shared(
         apiKey = BuildConfig.CROSSMINT_API_KEY,
         appContext = context,
         authManager = crossmintAuthManager,
         logLevel = Severity.Verbose
      )
   ) {
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
            CrossmintAuthViewModel(sdk.authManager as CrossmintAuthManager)
        }
    val dashboardViewModel: DashboardViewModel =
        viewModel {
            DashboardViewModel(sdk.crossmintWallets, sdk.authManager as CrossmintAuthManager)
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

       val shouldShowOTP = remember { mutableStateOf(false) }
       val scope = rememberCoroutineScope()

       LaunchedEffect(Unit) {
          Crossmint.instance.isOTPRequired.collect { isOTPRequired ->
             shouldShowOTP.value = isOTPRequired
          }
       }

       if (shouldShowOTP.value) {
          OTPDialog(
             onOTPSubmit = {
                scope.launch {
                   Crossmint.instance.submit(it)
                }
             },
             onDismiss = {
                scope.launch {
                   Crossmint.instance.cancelTransaction()
                }

                dashboardViewModel.clearTransaction()

                shouldShowOTP.value = false
             },
          )
       }
    }
}
