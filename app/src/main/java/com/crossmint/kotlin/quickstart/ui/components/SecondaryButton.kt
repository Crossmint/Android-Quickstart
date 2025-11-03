package com.crossmint.kotlin.quickstart.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.crossmint.kotlin.quickstart.ui.theme.DimensionTokens
import com.crossmint.kotlin.quickstart.ui.theme.SemanticColors
import com.crossmint.kotlin.quickstart.ui.theme.SpacingTokens
import com.crossmint.kotlin.quickstart.ui.theme.TypographyTokens

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconPosition: IconPosition = IconPosition.END
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null && iconPosition == IconPosition.START) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(DimensionTokens.Icon.sizeMedium),
                tint = SemanticColors.textPrimary
            )
        }

        Text(
            text = text,
            fontSize = TypographyTokens.caption,
            fontWeight = FontWeight.Medium,
            color = SemanticColors.textPrimary
        )

        if (icon != null && iconPosition == IconPosition.END) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(DimensionTokens.Icon.sizeMedium),
                tint = SemanticColors.textPrimary
            )
        }
    }
}

enum class IconPosition {
    START, END
}
