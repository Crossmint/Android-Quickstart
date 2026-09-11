package com.crossmint.kotlin.checkoutdemo.model

data class OrderDraft(
    val tokenLocator: String = "",
    val amount: String = "1",
    val recipientWalletAddress: String = "",
    val receiptEmail: String = "",
)
