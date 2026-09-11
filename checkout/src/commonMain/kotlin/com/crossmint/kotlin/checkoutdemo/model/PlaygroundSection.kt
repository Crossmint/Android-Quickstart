package com.crossmint.kotlin.checkoutdemo.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.graphics.vector.ImageVector

enum class PlaygroundSection(
    val title: String,
    val navLabel: String,
    val icon: ImageVector,
) {
    ORDER("Order", "Order", Icons.Filled.ShoppingBag),
    PAYMENT("Payment", "Payment", Icons.Filled.CreditCard),
    APPEARANCE("Appearance", "Appearance", Icons.Filled.Palette),
    FIELDS("Fields", "Fields", Icons.Filled.TextFields),
    IDENTITY("Identity verification", "Identity", Icons.Filled.Person),
}
