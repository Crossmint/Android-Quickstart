package com.crossmint.kotlin.checkoutdemo.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun CopyableRow(
    label: String,
    value: String,
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    ListItem(
        headlineContent = { Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        overlineContent = { Text(label) },
        trailingContent = {
            IconButton(onClick = { clipboardManager.setText(AnnotatedString(value)) }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy $label")
            }
        },
    )
}
