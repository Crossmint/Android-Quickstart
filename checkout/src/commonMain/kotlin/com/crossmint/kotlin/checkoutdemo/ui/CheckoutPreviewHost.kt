package com.crossmint.kotlin.checkoutdemo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crossmint.crossmintcheckoutdemoapp.generated.resources.Res
import com.crossmint.crossmintcheckoutdemoapp.generated.resources.ic_crossmint
import com.crossmint.kotlin.checkout.CheckoutEnvironment
import com.crossmint.kotlin.checkout.CrossmintEmbeddedCheckout
import com.crossmint.kotlin.checkoutdemo.PlaygroundUiState
import com.crossmint.kotlin.checkoutdemo.data.checkoutEnvironment
import com.crossmint.kotlin.checkoutdemo.model.OrderSession
import com.crossmint.kotlin.checkoutdemo.model.PlaygroundSection
import com.crossmint.kotlin.checkoutdemo.utility.formatTimestamp
import com.crossmint.kotlin.core.ApiKey
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutPreviewHost(
    section: PlaygroundSection,
    uiState: PlaygroundUiState,
    apiKey: String,
    onEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDetails by remember { mutableStateOf(false) }
    val session = uiState.session

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                actions = {
                    if (section != PlaygroundSection.IDENTITY && session != null) {
                        IconButton(onClick = { showDetails = true }) {
                            Icon(Icons.Filled.Info, contentDescription = "Order details")
                        }
                    }
                },
            )
        },
    ) { padding ->
        PreviewBody(
            section = section,
            uiState = uiState,
            apiKey = apiKey,
            onEvent = onEvent,
            modifier = Modifier.padding(padding).fillMaxSize(),
        )
    }

    if (showDetails && session != null) {
        OrderDetailsSheet(uiState = uiState, session = session, onDismissRequest = { showDetails = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewSheetContent(
    section: PlaygroundSection,
    uiState: PlaygroundUiState,
    apiKey: String,
    onEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDetails by remember { mutableStateOf(false) }
    val session = uiState.session

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Preview", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (section != PlaygroundSection.IDENTITY && session != null) {
                IconButton(onClick = { showDetails = true }) {
                    Icon(Icons.Filled.Info, contentDescription = "Order details")
                }
            }
        }
        HorizontalDivider()
        PreviewBody(
            section = section,
            uiState = uiState,
            apiKey = apiKey,
            onEvent = onEvent,
            modifier = Modifier.fillMaxWidth().height(640.dp),
        )
    }

    if (showDetails && session != null) {
        OrderDetailsSheet(uiState = uiState, session = session, onDismissRequest = { showDetails = false })
    }
}

@Composable
private fun PreviewBody(
    section: PlaygroundSection,
    uiState: PlaygroundUiState,
    apiKey: String,
    onEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = uiState.session

    Box(modifier = modifier) {
        when {
            section == PlaygroundSection.IDENTITY && uiState.isPreviewingIdentity ->
                IdentityPreview(
                    apiKey = apiKey,
                    inquiryId = uiState.confirmedIdentityInquiryId,
                    sessionToken = uiState.confirmedIdentitySessionToken,
                    modifier = Modifier.fillMaxSize(),
                    onEvent = onEvent,
                )

            section == PlaygroundSection.IDENTITY || session == null ->
                EmptyPreviewState(modifier = Modifier.fillMaxSize())

            else -> {
                val environment =
                    runCatching { ApiKey.fromString(apiKey).checkoutEnvironment }
                        .getOrDefault(CheckoutEnvironment.STAGING)

                CrossmintEmbeddedCheckout(
                    orderId = session.orderId,
                    clientSecret = session.clientSecret,
                    payment = uiState.options.toCheckoutPayment(uiState.draft.receiptEmail),
                    appearance = uiState.options.toCheckoutAppearance(),
                    environment = environment,
                    identityVerificationHandling = uiState.options.toIdentityVerificationHandling(),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderDetailsSheet(
    uiState: PlaygroundUiState,
    session: OrderSession,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            CopyableRow(label = "Order ID", value = session.orderId)
            CopyableRow(label = "Client secret", value = session.clientSecret)

            if (uiState.events.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    "Events",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(16.dp),
                )
                for (event in uiState.events.asReversed()) {
                    ListItem(
                        headlineContent = { Text(event.message) },
                        overlineContent = { Text(formatTimestamp(event.timestamp)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPreviewState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                painter = painterResource(Res.drawable.ic_crossmint),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                modifier = Modifier.size(72.dp),
            )
            Text("No order yet", textAlign = TextAlign.Center)
            Text(
                "Create an order or use an existing one from the Order section.",
                textAlign = TextAlign.Center,
            )
        }
    }
}
