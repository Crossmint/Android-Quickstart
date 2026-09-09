package com.crossmint.kotlin.auth.crossmint

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossmint.crossmintdemoapp.generated.resources.Res
import com.crossmint.crossmintdemoapp.generated.resources.crossmint_logo
import com.crossmint.kotlin.utility.EnvironmentIndicator
import org.jetbrains.compose.resources.painterResource

@Composable
fun CrossmintOTPVerificationScreen(
    viewModel: CrossmintAuthViewModel,
    isStaging: Boolean = false,
    onBackClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var showErrorDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

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
                ) { Text("OK") }
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
                ) { Text("OK") }
            },
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = {
                    viewModel.changeEmail()
                    onBackClick()
                },
                modifier = Modifier.statusBarsPadding(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Spacer(modifier = Modifier.height(72.dp))
                Image(
                    painter = painterResource(Res.drawable.crossmint_logo),
                    contentDescription = "Crossmint Logo",
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)),
                )
                Text(text = "Enter Verification Code", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "We've sent a 6-digit code to", fontSize = 14.sp, textAlign = TextAlign.Center)
                    Text(
                        text = uiState.email,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Change email",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier.clickable {
                                viewModel.changeEmail()
                                onBackClick()
                            },
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextField(
                        value = uiState.otpCode ?: "",
                        onValueChange = { viewModel.updateOtpCode(it) },
                        label = { Text("Verification Code") },
                        placeholder = { Text("000000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .semantics { testTag = "otp-input" },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        colors =
                            TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                    )
                    Button(
                        onClick = { viewModel.verifyOTP() },
                        enabled = uiState.canVerifyOTP,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .semantics { testTag = "verify-button" },
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            ),
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Verify Code", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Didn't receive the code? ", fontSize = 14.sp)
                        if (!uiState.canResendOTP) {
                            Text(
                                text = "Resend in ${uiState.resendCooldown}s",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = "Resend Code",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(enabled = uiState.canResendOTP) { viewModel.resendOTP() },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(2f))

                EnvironmentIndicator(isStaging)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Powered by crossmint",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
