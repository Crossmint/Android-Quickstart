package com.crossmint.kotlin.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.crossmint.kotlin.checkout.models.CheckoutAllowedMethods
import com.crossmint.kotlin.checkout.models.CheckoutAppearance
import com.crossmint.kotlin.checkout.models.CheckoutAppearanceRules
import com.crossmint.kotlin.checkout.models.CheckoutCryptoPayment
import com.crossmint.kotlin.checkout.models.CheckoutDestinationInputRule
import com.crossmint.kotlin.checkout.models.CheckoutFiatPayment
import com.crossmint.kotlin.checkout.models.CheckoutPayment
import com.crossmint.kotlin.checkout.models.CheckoutReceiptEmailInputRule

/**
 * Demo screen showcasing the CrossmintEmbeddedCheckout component.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(navController: NavController) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.White),
    ) {
        TopAppBar(
            title = { Text("Checkout") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            CrossmintEmbeddedCheckout(
                orderId = "your-order-id",
                clientSecret = "your-client-secret",
                payment =
                    CheckoutPayment(
                        crypto = CheckoutCryptoPayment(enabled = false),
                        fiat =
                            CheckoutFiatPayment(
                                enabled = true,
                                allowedMethods =
                                    CheckoutAllowedMethods(
                                        googlePay = true,
                                        applePay = true,
                                        card = true,
                                    ),
                            ),
                        receiptEmail = "robin+kotlin@crossmint.com",
                    ),
                appearance =
                    CheckoutAppearance(
                        rules =
                            CheckoutAppearanceRules(
                                destinationInput = CheckoutDestinationInputRule(display = "hidden"),
                                receiptEmailInput = CheckoutReceiptEmailInputRule(display = "hidden"),
                            ),
                    ),
                environment = CheckoutEnvironment.STAGING,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
