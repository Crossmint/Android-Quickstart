package com.crossmint.kotlin.utility

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EnvironmentIndicator(isStaging: Boolean) {
    if (isStaging) {
        Card(
            modifier = Modifier.padding(8.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3CD),
                ),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = "STAGING ENVIRONMENT",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF856404),
            )
        }
    }
}
