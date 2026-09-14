package com.crossmint.kotlin.checkoutdemo.ui.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crossmint.kotlin.checkoutdemo.model.PlaygroundOptions
import com.crossmint.kotlin.checkoutdemo.ui.SwitchRow

@Composable
fun FieldsSectionView(
    options: PlaygroundOptions,
    onOptionsChange: (PlaygroundOptions) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        SwitchRow(
            label = "Show destination input",
            checked = options.showsDestinationInput,
            onCheckedChange = { onOptionsChange(options.copy(showsDestinationInput = it)) },
        )
        SwitchRow(
            label = "Show receipt email input",
            checked = options.showsReceiptEmailInput,
            onCheckedChange = { onOptionsChange(options.copy(showsReceiptEmailInput = it)) },
        )
        SwitchRow(
            label = "Show status message",
            checked = options.showsStatusMessage,
            onCheckedChange = { onOptionsChange(options.copy(showsStatusMessage = it)) },
        )
        SwitchRow(
            label = "Render identity verification externally",
            checked = options.externalIdentityVerification,
            onCheckedChange = { onOptionsChange(options.copy(externalIdentityVerification = it)) },
        )
    }
}
