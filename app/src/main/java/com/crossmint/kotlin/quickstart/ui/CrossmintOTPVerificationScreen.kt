package com.crossmint.kotlin.quickstart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.crossmint.kotlin.quickstart.auth.CrossmintAuthViewModel
import com.crossmint.kotlin.quickstart.ui.components.CustomTextField
import com.crossmint.kotlin.quickstart.ui.components.IconPosition
import com.crossmint.kotlin.quickstart.ui.components.PrimaryButton
import com.crossmint.kotlin.quickstart.ui.components.SecondaryButton
import com.crossmint.kotlin.quickstart.ui.theme.DimensionTokens
import com.crossmint.kotlin.quickstart.ui.theme.SemanticColors
import com.crossmint.kotlin.quickstart.ui.theme.SpacingTokens
import com.crossmint.kotlin.quickstart.ui.theme.TypographyTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossmintOTPVerificationBottomSheet(
    viewModel: CrossmintAuthViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showErrorDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    LaunchedEffect(uiState.hasError) {
        if (uiState.hasError) {
            showErrorDialog = true
        }
    }

    LaunchedEffect(uiState.isResendSuccessMessage) {
        if (uiState.isResendSuccessMessage) {
            showSuccessDialog = true
        }
    }

    LaunchedEffect(uiState.isOTPComplete) {
        if (uiState.isOTPComplete) {
            viewModel.verifyOTP()
        }
    }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            sheetState.hide()
            onDismiss()
        }
    }

    if (showErrorDialog && uiState.hasError) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                viewModel.clearMessages()
            },
            title = { Text("Error") },
            text = { Text(uiState.errorMessage ?: "") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showErrorDialog = false
                        viewModel.clearMessages()
                    },
                ) {
                    Text("OK")
                }
            },
        )
    }

    if (showSuccessDialog && uiState.hasSuccessMessage) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                viewModel.clearMessages()
            },
            title = { Text("Success") },
            text = { Text(uiState.successMessage ?: "") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        viewModel.clearMessages()
                    },
                ) {
                    Text("OK")
                }
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!uiState.isLoading) {
                viewModel.changeEmail()
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.xxl)
                .padding(top = SpacingTokens.xl)
                .padding(bottom = SpacingTokens.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OTP Verification",
                    fontSize = TypographyTokens.title3,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .size(SpacingTokens.xxxl)
                        .background(SemanticColors.backgroundSystem, CircleShape)
                        .clickable {
                            if (!uiState.isLoading) {
                                viewModel.changeEmail()
                                onDismiss()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(DimensionTokens.Icon.sizeLarge),
                        tint = SemanticColors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.xxl))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.s),
            ) {
                Text(
                    text = "We've sent a 6-digit code to",
                    fontSize = TypographyTokens.subheadline,
                    color = SemanticColors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = uiState.email,
                    fontSize = TypographyTokens.subheadline,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Change email",
                    fontSize = TypographyTokens.subheadline,
                    color = SemanticColors.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable {
                        if (!uiState.isLoading) {
                            viewModel.changeEmail()
                            onDismiss()
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(SpacingTokens.xxl))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CustomTextField(
                    value = uiState.otpCode ?: "",
                    onValueChange = { viewModel.updateOtpCode(it) },
                    placeholder = "000000",
                    keyboardType = KeyboardType.Number,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                PrimaryButton(
                    text = "Verify Code",
                    onClick = { viewModel.verifyOTP() },
                    isLoading = uiState.isLoading,
                    isDisabled = !uiState.canVerifyOTP,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!uiState.canResendOTP) {
                        Text(
                            text = "Resend in ${uiState.resendCooldown}s",
                            fontSize = TypographyTokens.caption,
                            color = SemanticColors.textPrimary,
                        )
                    } else {
                        SecondaryButton(
                            text = "Resend Code",
                            onClick = { viewModel.resendOTP() },
                            icon = Icons.Default.Refresh,
                            iconPosition = IconPosition.START
                        )
                    }
                }
            }
        }
    }
}
