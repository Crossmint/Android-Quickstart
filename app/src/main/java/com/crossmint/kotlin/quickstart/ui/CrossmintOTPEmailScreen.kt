package com.crossmint.kotlin.quickstart.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.systemBarsPadding
import com.crossmint.kotlin.quickstart.R
import com.crossmint.kotlin.quickstart.auth.CrossmintAuthViewModel
import com.crossmint.kotlin.quickstart.ui.components.CrossmintPoweredView
import com.crossmint.kotlin.quickstart.ui.components.CustomTextField
import com.crossmint.kotlin.quickstart.ui.components.PrimaryButton
import com.crossmint.kotlin.quickstart.ui.theme.DimensionTokens
import com.crossmint.kotlin.quickstart.ui.theme.SemanticColors
import com.crossmint.kotlin.quickstart.ui.theme.SpacingTokens
import com.crossmint.kotlin.quickstart.ui.theme.TypographyTokens

@Composable
fun CrossmintOTPEmailScreen(
    viewModel: CrossmintAuthViewModel,
    onBackClick: () -> Unit,
    showBackButton: Boolean = true,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showErrorDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(uiState.hasError) {
        if (uiState.hasError) {
            showErrorDialog = true
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding(),
    ) {
        if (showBackButton) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                )
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = SpacingTokens.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.xl),
            ) {
                Spacer(modifier = Modifier.height(SpacingTokens.xxxxxxl))

                Image(
                    painter = painterResource(R.drawable.crossmint_splash_icon),
                    contentDescription = "Crossmint Logo",
                    modifier = Modifier
                        .size(DimensionTokens.Logo.sizeMedium)
                        .clip(RoundedCornerShape(DimensionTokens.CornerRadius.l)),
                )

                Text(
                    text = "Crossmint Quickstart",
                    fontSize = TypographyTokens.title2,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = "The easiest way to build onchain",
                    fontSize = TypographyTokens.subheadline,
                    color = SemanticColors.textPrimary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.l),
                ) {
                    CustomTextField(
                        value = uiState.email,
                        onValueChange = { viewModel.updateEmail(it) },
                        placeholder = "your@email.com",
                        keyboardType = KeyboardType.Email,
                        modifier = Modifier.fillMaxWidth()
                    )

                    PrimaryButton(
                        text = "Send OTP Code",
                        onClick = { viewModel.sendOTP() },
                        isLoading = uiState.isLoading,
                        isDisabled = !uiState.canSendOTP,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.weight(2f))

                CrossmintPoweredView(
                    modifier = Modifier.padding(bottom = SpacingTokens.xxxl)
                )
            }
        }
    }
}
