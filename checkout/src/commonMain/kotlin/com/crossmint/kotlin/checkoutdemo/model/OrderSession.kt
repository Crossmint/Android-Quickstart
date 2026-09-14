package com.crossmint.kotlin.checkoutdemo.model

data class OrderSession(
    val orderId: String,
    val clientSecret: String,
    val source: Source,
) {
    enum class Source {
        CREATED_IN_APP,
        EXISTING,
    }
}
