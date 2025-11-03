package com.crossmint.kotlin.quickstart.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.crossmint.kotlin.quickstart.R
import com.crossmint.kotlin.quickstart.ui.theme.SemanticColors
import com.crossmint.kotlin.quickstart.ui.theme.SpacingTokens
import com.crossmint.kotlin.quickstart.ui.theme.TypographyTokens

@Composable
fun CrossmintPoweredView(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SpacingTokens.xs),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.crossmint_icon_gray),
            contentDescription = null,
            modifier = Modifier.height(SpacingTokens.l)
        )

        Spacer(modifier = Modifier.width(SpacingTokens.xs))

        Text(
            text = "Powered by crossmint",
            fontSize = TypographyTokens.caption,
            color = SemanticColors.textPrimary.copy(alpha = 0.7f)
        )
    }
}
