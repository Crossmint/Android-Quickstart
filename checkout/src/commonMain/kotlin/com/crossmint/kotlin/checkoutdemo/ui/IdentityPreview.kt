package com.crossmint.kotlin.checkoutdemo.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun IdentityPreview(
    apiKey: String,
    usesSession: Boolean,
    id: String,
    secret: String,
    modifier: Modifier,
    onEvent: (String) -> Unit,
)
