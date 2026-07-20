package com.crossmint.kotlin.auth.bringyourown

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.crossmint.crossmintdemoapp.generated.resources.Res
import com.crossmint.crossmintdemoapp.generated.resources.crossmint_logo
import com.crossmint.kotlin.Routes
import com.crossmint.kotlin.compose.LocalCrossmintSDK
import com.crossmint.kotlin.utility.EnvironmentIndicator
import com.crossmint.kotlin.utility.Footer
import org.jetbrains.compose.resources.painterResource

@Composable
fun BringYourOwnAuthScreen(
    navController: NavController,
    isStaging: Boolean = false,
) {
    val sdk = LocalCrossmintSDK.current

    val viewModel =
        remember {
            BringYourOwnAuthViewModel(
                goToWallets = {
                    navController.navigate(Routes.Wallet) {
                        popUpTo(Routes.AuthMethodSelection) {
                            inclusive = true
                        }
                    }
                },
                sdk.authManager,
            )
        }
    val uiState by viewModel.uiState.collectAsState()
    var showErrorDialog by remember { mutableStateOf(false) }

    // Show error dialog when there's an error
    LaunchedEffect(uiState.hasError) {
        if (uiState.hasError) {
            showErrorDialog = true
        }
    }

    // Error Dialog
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = {
                    navController.popBackStack()
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
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Spacer(modifier = Modifier.height(72.dp))

                Image(
                    painter = painterResource(Res.drawable.crossmint_logo),
                    contentDescription = "Crossmint Logo",
                    modifier =
                        Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp)),
                )

                Text(
                    text = "Bring your own auth",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text =
                        """
                        Specify your JWKS endpoint for validating JWT tokens in the Crossmint admin panel. Then paste your JWT token for a given user below
                        """.trimIndent(),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TextField(
                        value = uiState.jwt,
                        onValueChange = { viewModel.updateJWT(it) },
                        label = { Text("Paste JWT token here") },
                        placeholder = { Text("your-jwt-token-here") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
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
                        onClick = { viewModel.submitJWT() },
                        enabled = true,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp),
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
                            Text(
                                "Submit JWT",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(2f))

                EnvironmentIndicator(isStaging)

                Footer()
            }
        }
    }
}
