package com.crossmint.kotlin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.crossmint.crossmintdemoapp.generated.resources.Res
import com.crossmint.crossmintdemoapp.generated.resources.crossmint_logo
import com.crossmint.kotlin.utility.EnvironmentIndicator
import org.jetbrains.compose.resources.painterResource

@Composable
fun AuthMethodSelectionScreen(
    navController: NavController,
    isStaging: Boolean = false,
    isBYOAMode: Boolean = false,
    onAuthModeChange: ((Boolean) -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Image(
                painter = painterResource(Res.drawable.crossmint_logo),
                contentDescription = "Crossmint Logo",
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp)),
            )

            Text(
                text = "Crossmint SDK Demo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Select authentication mode",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )

            // Auth Mode Selection Radio Buttons
            AuthModeSelector(
                isBYOAMode = isBYOAMode,
                onAuthModeChange = onAuthModeChange,
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Dynamic Sign In button based on auth mode
                SignInButton(
                    isBYOAMode = isBYOAMode,
                    onClick = {
                        if (isBYOAMode) {
                            navController.navigate(Routes.BringYourOwnAuth)
                        } else {
                            navController.navigate(Routes.CrossmintOTPEmail)
                        }
                    },
                )

                OrDivider()

                CheckoutButton(onClick = {
                    navController.navigate(Routes.Checkout)
                })
            }

            Spacer(modifier = Modifier.weight(1f))

            EnvironmentIndicator(isStaging)

            // Footer
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Powered by crossmint",
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
fun AuthModeSelector(
    isBYOAMode: Boolean,
    onAuthModeChange: ((Boolean) -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Authentication Mode",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = !isBYOAMode,
                    onClick = { onAuthModeChange?.invoke(false) },
                    colors =
                        RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                        ),
                )
                Text(
                    text = "Crossmint Auth",
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = isBYOAMode,
                    onClick = { onAuthModeChange?.invoke(true) },
                    colors =
                        RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                        ),
                )
                Text(
                    text = "Bring Your Own Auth",
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun SignInButton(
    isBYOAMode: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .semantics { testTag = "signin-with-crossmint-button" },
        shape = RoundedCornerShape(12.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (isBYOAMode) "Sign in with Your JWT" else "Sign in with Crossmint",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                if (isBYOAMode) "Paste JWT" else "Email + OTP",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            text = "OR",
            modifier = Modifier.padding(horizontal = 16.dp),
            fontSize = 14.sp,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
fun CheckoutButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = { onClick() },
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Embedded Checkout",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "Payment Flow",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}
