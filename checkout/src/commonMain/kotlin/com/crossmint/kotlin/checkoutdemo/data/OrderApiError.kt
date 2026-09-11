package com.crossmint.kotlin.checkoutdemo.data

sealed interface OrderApiError {
    val message: String

    data class Network(
        val cause: Throwable,
    ) : OrderApiError {
        override val message: String = cause.message ?: "The Orders API request failed."
    }

    data class Api(
        override val message: String,
    ) : OrderApiError

    data class UnexpectedResponse(
        override val message: String = "The Orders API returned a response the demo could not read.",
    ) : OrderApiError
}
