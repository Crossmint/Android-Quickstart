package com.crossmint.kotlin.quickstart.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crossmint.kotlin.quickstart.ui.theme.DimensionTokens
import com.crossmint.kotlin.quickstart.ui.theme.SemanticColors
import com.crossmint.kotlin.quickstart.ui.theme.SpacingTokens
import com.crossmint.kotlin.quickstart.ui.theme.TypographyTokens

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isDisabled: Boolean = false,
    backgroundColor: Color = SemanticColors.primary,
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(DimensionTokens.Button.heightLarge),
        enabled = !isDisabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.7f),
            disabledContentColor = contentColor.copy(alpha = 0.7f)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(DimensionTokens.CornerRadius.s),
        contentPadding = PaddingValues(SpacingTokens.l)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontSize = TypographyTokens.body,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
