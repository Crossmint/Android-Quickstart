package com.crossmint.kotlin.quickstart.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.systemBarsPadding
import com.crossmint.kotlin.quickstart.R
import com.crossmint.kotlin.quickstart.ui.components.SecondaryButton
import com.crossmint.kotlin.quickstart.ui.theme.DimensionTokens
import com.crossmint.kotlin.quickstart.ui.theme.SemanticColors
import com.crossmint.kotlin.quickstart.ui.theme.SpacingTokens
import com.crossmint.kotlin.quickstart.ui.theme.TypographyTokens

@Composable
fun SplashScreen(
    isLoading: Boolean,
    error: String? = null,
    onRetry: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.crossmint_splash_icon),
                contentDescription = "Crossmint Logo",
                modifier = Modifier.size(DimensionTokens.Logo.sizeLarge)
            )

            Spacer(modifier = Modifier.height(SpacingTokens.xxxxxxl))

            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer(scaleX = 1.5f, scaleY = 1.5f),
                        color = SemanticColors.primary
                    )
                }
                error != null -> {
                    Text(
                        text = error,
                        fontSize = TypographyTokens.subheadline,
                        color = SemanticColors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = SpacingTokens.xxxl)
                    )
                    Spacer(modifier = Modifier.height(SpacingTokens.xxl))
                    SecondaryButton(
                        text = "Retry",
                        onClick = onRetry
                    )
                }
            }
        }
    }
}
