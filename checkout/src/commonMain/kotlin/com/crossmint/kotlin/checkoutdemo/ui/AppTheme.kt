package com.crossmint.kotlin.checkoutdemo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val CrossmintGreen = Color(0xFF00E6AC)

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF00997A),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB6FFEA),
        onPrimaryContainer = Color(0xFF00201A),
        secondary = Color(0xFF00997A),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFB6FFEA),
        onSecondaryContainer = Color(0xFF00201A),
        background = Color.White,
        surface = Color.White,
        surfaceVariant = Color(0xFFF5F5F5),
        onSurface = Color(0xFF1C1C1E),
        onSurfaceVariant = Color(0xFF6C6C70),
        outlineVariant = Color(0xFFE5E5EA),
    )

private val DarkColors =
    darkColorScheme(
        primary = CrossmintGreen,
        onPrimary = Color(0xFF00382B),
        primaryContainer = Color(0xFF00513E),
        onPrimaryContainer = Color(0xFFB6FFEA),
        secondary = CrossmintGreen,
        onSecondary = Color(0xFF00382B),
        secondaryContainer = Color(0xFF00513E),
        onSecondaryContainer = Color(0xFFB6FFEA),
        background = Color.Black,
        surface = Color(0xFF1C1C1E),
        surfaceVariant = Color(0xFF2C2C2E),
        onSurface = Color(0xFFF2F2F7),
        onSurfaceVariant = Color(0xFF8E8E93),
        outlineVariant = Color(0xFF38383A),
    )

@Composable
fun CheckoutDemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

val ColorScheme.brandMark: Color
    @Composable
    @ReadOnlyComposable
    get() = CrossmintGreen
