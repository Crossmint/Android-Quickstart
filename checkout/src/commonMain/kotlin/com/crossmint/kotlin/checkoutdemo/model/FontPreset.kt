package com.crossmint.kotlin.checkoutdemo.model

import com.crossmint.kotlin.checkout.models.CheckoutFontSource
import com.crossmint.kotlin.checkout.models.CheckoutFontWeight

enum class FontPreset(
    val label: String,
    val family: String?,
    val source: CheckoutFontSource?,
) {
    DEFAULT("Default", null, null),
    ROBOTO_MONO(
        "Roboto Mono",
        "'Roboto Mono', monospace",
        CheckoutFontSource.googleFonts(
            "Roboto Mono",
            weights = listOf(CheckoutFontWeight.Normal, CheckoutFontWeight.Medium, CheckoutFontWeight.SemiBold),
        ),
    ),
    PLAYFAIR(
        "Playfair Display",
        "'Playfair Display', serif",
        CheckoutFontSource.googleFonts(
            "Playfair Display",
            weights = listOf(CheckoutFontWeight.Normal, CheckoutFontWeight.Medium, CheckoutFontWeight.SemiBold),
        ),
    ),
    LOBSTER("Lobster", "Lobster, cursive", CheckoutFontSource.googleFonts("Lobster")),
}
