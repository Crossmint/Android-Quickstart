package com.crossmint.kotlin.checkoutdemo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.crossmint.kotlin.checkoutdemo.data.OrdersApi
import com.crossmint.kotlin.checkoutdemo.data.createHttpClient
import com.crossmint.kotlin.checkoutdemo.ui.CheckoutDemoApp
import com.crossmint.kotlin.checkoutdemo.ui.CheckoutDemoTheme

@Composable
fun AppRoot(apiKey: String) {
    CheckoutDemoTheme {
        val viewModel =
            remember {
                PlaygroundViewModel(
                    ordersApi = OrdersApi(createHttpClient()),
                    apiKey = apiKey,
                )
            }
        CheckoutDemoApp(viewModel = viewModel, apiKey = apiKey)
    }
}
