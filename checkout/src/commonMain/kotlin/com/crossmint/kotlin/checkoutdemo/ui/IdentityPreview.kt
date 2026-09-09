package com.crossmint.kotlin.checkoutdemo.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun IdentityPreview(
    apiKey: String,
    inquiryId: String,
    sessionToken: String,
    modifier: Modifier,
    onEvent: (String) -> Unit,
)
