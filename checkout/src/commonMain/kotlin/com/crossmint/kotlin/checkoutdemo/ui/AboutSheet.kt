package com.crossmint.kotlin.checkoutdemo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.crossmint.kotlin.core.ApiKey
import com.crossmint.kotlin.core.Environment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(
    apiKey: String,
    onDismissRequest: () -> Unit,
) {
    val environment = runCatching { ApiKey.fromString(apiKey).environment }.getOrNull()
    val uriHandler = LocalUriHandler.current

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                    "Environment",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    environment?.value?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = environmentNote(environment),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
            }

            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Documentation") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                modifier = Modifier.clickable { uriHandler.openUri("https://docs.crossmint.com") },
            )
            ListItem(
                headlineContent = { Text("Crossmint Console") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                modifier = Modifier.clickable { uriHandler.openUri("https://console.crossmint.com") },
            )
        }
    }
}

private fun environmentNote(environment: Environment?): String =
    when (environment) {
        Environment.STAGING, Environment.DEVELOPMENT ->
            "This build points at ${environment.value}. Orders use testnet tokens, so a checkout here cannot move real funds."
        Environment.PRODUCTION -> "This build points at production. A checkout here moves real funds."
        null ->
            "The demo cannot tell which environment this key belongs to. Use a key that starts with ck_staging_ or ck_production_."
    }
