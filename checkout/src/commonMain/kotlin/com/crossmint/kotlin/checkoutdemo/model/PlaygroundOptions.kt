package com.crossmint.kotlin.checkoutdemo.model

import com.crossmint.kotlin.checkout.IdentityVerificationHandling
import com.crossmint.kotlin.checkout.models.CheckoutAllowedMethods
import com.crossmint.kotlin.checkout.models.CheckoutAppearance
import com.crossmint.kotlin.checkout.models.CheckoutAppearanceRules
import com.crossmint.kotlin.checkout.models.CheckoutAppearanceVariables
import com.crossmint.kotlin.checkout.models.CheckoutColor
import com.crossmint.kotlin.checkout.models.CheckoutCryptoPayment
import com.crossmint.kotlin.checkout.models.CheckoutDestinationInputRule
import com.crossmint.kotlin.checkout.models.CheckoutFiatPayment
import com.crossmint.kotlin.checkout.models.CheckoutGlobalMessageRule
import com.crossmint.kotlin.checkout.models.CheckoutInputRule
import com.crossmint.kotlin.checkout.models.CheckoutLength
import com.crossmint.kotlin.checkout.models.CheckoutPayment
import com.crossmint.kotlin.checkout.models.CheckoutPrimaryButtonRule
import com.crossmint.kotlin.checkout.models.CheckoutReceiptEmailInputRule
import com.crossmint.kotlin.checkout.models.CheckoutTabRule
import com.crossmint.kotlin.checkout.models.CheckoutVariablesColorStyle

data class PlaygroundOptions(
    val cryptoEnabled: Boolean = false,
    val fiatEnabled: Boolean = true,
    val allowCard: Boolean = true,
    val allowGooglePay: Boolean = true,
    val showsDestinationInput: Boolean = true,
    val showsReceiptEmailInput: Boolean = true,
    val showsStatusMessage: Boolean = true,
    val textColor: String = "",
    val backgroundColor: String = "",
    val accentColor: String = "",
    val dangerColor: String = "",
    val font: FontPreset = FontPreset.DEFAULT,
    val inputRadius: Float = DEFAULT_BORDER_RADIUS,
    val tabRadius: Float = DEFAULT_BORDER_RADIUS,
    val buttonRadius: Float = DEFAULT_BORDER_RADIUS,
    val externalIdentityVerification: Boolean = true,
) {
    fun toCheckoutPayment(receiptEmail: String): CheckoutPayment =
        CheckoutPayment(
            crypto = CheckoutCryptoPayment(enabled = cryptoEnabled),
            fiat =
                CheckoutFiatPayment(
                    enabled = fiatEnabled,
                    allowedMethods =
                        CheckoutAllowedMethods(
                            googlePay = allowGooglePay,
                            applePay = false,
                            card = allowCard,
                        ),
                ),
            receiptEmail = receiptEmail.ifBlank { null },
        )

    fun toCheckoutAppearance(): CheckoutAppearance? {
        val colors =
            if (textColor.isBlank() && backgroundColor.isBlank() && accentColor.isBlank() && dangerColor.isBlank()) {
                null
            } else {
                CheckoutVariablesColorStyle(
                    textPrimaryColor = textColor.ifBlank { null }?.let(CheckoutColor::hex),
                    backgroundPrimaryColor = backgroundColor.ifBlank { null }?.let(CheckoutColor::hex),
                    accentColor = accentColor.ifBlank { null }?.let(CheckoutColor::hex),
                    dangerColor = dangerColor.ifBlank { null }?.let(CheckoutColor::hex),
                )
            }

        val variables =
            if (colors == null && font.family == null) {
                null
            } else {
                CheckoutAppearanceVariables(fontFamily = font.family, colors = colors)
            }
        val fonts = font.source?.let { listOf(it) }

        val hidesElement = !showsDestinationInput || !showsReceiptEmailInput || !showsStatusMessage
        val hasCustomInputRadius = inputRadius != DEFAULT_BORDER_RADIUS
        val hasCustomTabRadius = tabRadius != DEFAULT_BORDER_RADIUS
        val hasCustomButtonRadius = buttonRadius != DEFAULT_BORDER_RADIUS

        val rules =
            if (hidesElement || hasCustomInputRadius || hasCustomTabRadius || hasCustomButtonRadius) {
                CheckoutAppearanceRules(
                    destinationInput =
                        if (showsDestinationInput) {
                            null
                        } else {
                            CheckoutDestinationInputRule(
                                display = "hidden",
                            )
                        },
                    receiptEmailInput =
                        if (showsReceiptEmailInput) {
                            null
                        } else {
                            CheckoutReceiptEmailInputRule(
                                display = "hidden",
                            )
                        },
                    globalMessage =
                        if (showsStatusMessage) {
                            null
                        } else {
                            CheckoutGlobalMessageRule(
                                display = "hidden",
                            )
                        },
                    input =
                        if (hasCustomInputRadius) {
                            CheckoutInputRule(
                                cornerRadius = CheckoutLength.px(inputRadius.toInt()),
                            )
                        } else {
                            null
                        },
                    tab =
                        if (hasCustomTabRadius) {
                            CheckoutTabRule(
                                cornerRadius = CheckoutLength.px(tabRadius.toInt()),
                            )
                        } else {
                            null
                        },
                    primaryButton =
                        if (hasCustomButtonRadius) {
                            CheckoutPrimaryButtonRule(cornerRadius = CheckoutLength.px(buttonRadius.toInt()))
                        } else {
                            null
                        },
                )
            } else {
                null
            }

        if (variables == null && rules == null && fonts == null) return null
        return CheckoutAppearance(rules = rules, variables = variables, fonts = fonts)
    }

    fun toIdentityVerificationHandling(): IdentityVerificationHandling? =
        if (externalIdentityVerification) IdentityVerificationHandling.EXTERNAL else null

    companion object {
        const val DEFAULT_BORDER_RADIUS = 8f
    }
}
