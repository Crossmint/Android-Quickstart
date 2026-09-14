package com.crossmint.kotlin

import kotlinx.serialization.Serializable

class Routes {
    @Serializable
    object AuthMethodSelection

    @Serializable
    object CrossmintOTPEmail

    @Serializable
    object CrossmintOTPVerification

    @Serializable
    object BringYourOwnAuth

    @Serializable
    object Wallet

    @Serializable
    object CreateWallet
}
