package com.crossmint.kotlin.checkoutdemo.ui.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crossmint.kotlin.checkoutdemo.model.PlaygroundOptions
import com.crossmint.kotlin.checkoutdemo.ui.SwitchRow

@Composable
fun PaymentSectionView(
    options: PlaygroundOptions,
    onOptionsChange: (PlaygroundOptions) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        SwitchRow(
            label = "Fiat payment",
            checked = options.fiatEnabled,
            onCheckedChange = { onOptionsChange(options.copy(fiatEnabled = it)) },
        )
        SwitchRow(
            label = "Allow card",
            checked = options.allowCard,
            onCheckedChange = { onOptionsChange(options.copy(allowCard = it)) },
        )
        SwitchRow(
            label = "Allow Google Pay",
            checked = options.allowGooglePay,
            onCheckedChange = { onOptionsChange(options.copy(allowGooglePay = it)) },
        )
        SwitchRow(
            label = "Crypto payment",
            checked = options.cryptoEnabled,
            onCheckedChange = { onOptionsChange(options.copy(cryptoEnabled = it)) },
        )
    }
}
