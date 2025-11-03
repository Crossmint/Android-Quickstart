package com.crossmint.kotlin.quickstart.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crossmint.kotlin.quickstart.R
import com.crossmint.kotlin.quickstart.ui.components.*
import com.crossmint.kotlin.quickstart.ui.theme.DimensionTokens
import com.crossmint.kotlin.quickstart.ui.theme.SemanticColors
import com.crossmint.kotlin.quickstart.ui.theme.SpacingTokens
import com.crossmint.kotlin.quickstart.ui.theme.TypographyTokens
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel = viewModel(),
    onLogout: () -> Unit,
    onCreateWallet: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var isShaking by remember { mutableStateOf(false) }
    val shakeOffset by animateFloatAsState(
        targetValue = if (isShaking) 1f else 0f,
        animationSpec = tween(500, easing = LinearEasing),
        finishedListener = { isShaking = false }, label = "shake"
    )

    val shakeRotation = if (isShaking) {
        sin(shakeOffset * 6 * 2 * PI) * 5
    } else {
        0.0
    }

    val dashboardUiState by dashboardViewModel.uiState.collectAsState()
    val walletAddress = dashboardUiState.walletAddress

    LaunchedEffect(Unit) {
        dashboardViewModel.fetchWallet()
    }

    var wasCreatingWallet by remember { mutableStateOf(false) }
    LaunchedEffect(dashboardUiState.isCreatingWallet, dashboardUiState.hasWallet) {
        if (wasCreatingWallet && !dashboardUiState.isCreatingWallet && dashboardUiState.hasWallet) {
            onCreateWallet()
        }
        wasCreatingWallet = dashboardUiState.isCreatingWallet
    }

    var showErrorDialog by remember { mutableStateOf(false) }
    LaunchedEffect(dashboardUiState.hasError) {
        if (dashboardUiState.hasError) {
            showErrorDialog = true
        }
    }

    if (showErrorDialog && dashboardUiState.hasError) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                dashboardViewModel.clearError()
            },
            title = { Text("Error") },
            text = { Text(dashboardUiState.errorMessage ?: "An error occurred") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showErrorDialog = false
                        dashboardViewModel.clearError()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = SpacingTokens.xxl,
                        vertical = SpacingTokens.l
                    )
                    .padding(top = SpacingTokens.l, bottom = SpacingTokens.xxxl),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.crossmint_logo),
                    contentDescription = "Crossmint",
                    modifier = Modifier.size(DimensionTokens.Logo.sizeSmall)
                )

                SecondaryButton(
                    text = "Logout",
                    onClick = onLogout,
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    iconPosition = IconPosition.END
                )
            }

            if (walletAddress != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingTokens.xxl)
                        .padding(bottom = SpacingTokens.xxl)
                ) {
                    Text(
                        text = "Your wallet",
                        fontSize = TypographyTokens.subheadline,
                        color = SemanticColors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(SpacingTokens.s))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.s),
                        modifier = Modifier.graphicsLayer(rotationZ = shakeRotation.toFloat())
                    ) {
                        Text(
                            text = middleEllipsisText(walletAddress, 12),
                            fontSize = TypographyTokens.headline,
                            fontWeight = FontWeight.Bold
                        )

                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier
                                .size(DimensionTokens.Icon.sizeMedium)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(walletAddress))
                                    haptic.performHapticFeedback(
                                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                                    )
                                    isShaking = true
                                },
                            tint = SemanticColors.textPrimary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .shadow(
                        elevation = DimensionTokens.Shadow.medium,
                        shape = RoundedCornerShape(
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp,
                            topStart = DimensionTokens.CornerRadius.xl,
                            topEnd = DimensionTokens.CornerRadius.xl
                        ),
                        spotColor = SemanticColors.shadow
                    )
                    .background(
                        Color.White,
                        RoundedCornerShape(
                            topStart = DimensionTokens.CornerRadius.xl,
                            topEnd = DimensionTokens.CornerRadius.xl
                        )
                    )
            ) {
                if (walletAddress == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(SpacingTokens.xxxl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(SpacingTokens.xxxxxl),
                            tint = SemanticColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.l))
                        Text(
                            text = "No Wallet Available",
                            fontSize = TypographyTokens.title2,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.s))
                        Text(
                            text = "Create a wallet to start managing your assets",
                            fontSize = TypographyTokens.subheadline,
                            color = SemanticColors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.xxl))
                        PrimaryButton(
                            text = "Create Wallet",
                            onClick = { dashboardViewModel.createWallet() }
                        )
                    }
                } else {
                    TransferView(dashboardViewModel = dashboardViewModel)
                }
            }

            CrossmintPoweredView(
                modifier = Modifier.padding(bottom = SpacingTokens.l)
            )
        }
    }
}
