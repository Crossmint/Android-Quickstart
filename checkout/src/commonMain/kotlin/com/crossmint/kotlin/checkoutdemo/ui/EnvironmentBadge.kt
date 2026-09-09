package com.crossmint.kotlin.checkoutdemo.ui

import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.crossmint.kotlin.core.ApiKey

@Composable
fun EnvironmentBadge(apiKey: String) {
    val environment = runCatching { ApiKey.fromString(apiKey).environment }.getOrNull() ?: return
    AssistChip(onClick = {}, enabled = false, label = { Text(environment.value) })
}
