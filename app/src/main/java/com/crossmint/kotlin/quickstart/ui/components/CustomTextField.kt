package com.crossmint.kotlin.quickstart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crossmint.kotlin.quickstart.ui.theme.DimensionTokens
import com.crossmint.kotlin.quickstart.ui.theme.SemanticColors
import com.crossmint.kotlin.quickstart.ui.theme.SpacingTokens
import com.crossmint.kotlin.quickstart.ui.theme.TypographyTokens

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    textAlign: TextAlign = TextAlign.Start
) {
    val shape = RoundedCornerShape(DimensionTokens.CornerRadius.s)

    Box(
        modifier = modifier
            .height(DimensionTokens.TextField.height)
            .shadow(
                elevation = DimensionTokens.Shadow.light,
                shape = shape,
                spotColor = SemanticColors.shadow
            )
            .background(Color.White, shape)
            .border(1.dp, SemanticColors.border, shape)
            .padding(horizontal = SpacingTokens.l),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontSize = TypographyTokens.body,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                textAlign = textAlign
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = TypographyTokens.body,
                        color = SemanticColors.textPrimary.copy(alpha = 0.5f),
                        textAlign = textAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                innerTextField()
            }
        )
    }
}
