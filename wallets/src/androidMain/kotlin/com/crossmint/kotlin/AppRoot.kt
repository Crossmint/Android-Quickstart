package com.crossmint.kotlin

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.crossmint.demo.BuildKonfig
import com.crossmint.kotlin.auth.AuthMode
import com.crossmint.kotlin.auth.AuthModePreferences
import com.crossmint.kotlin.compose.CrossmintNonCustodialSignerProvider
import com.crossmint.kotlin.core.LogLevel
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun AppRoot() {
    val context = LocalContext.current
    val authMode = remember { AuthModePreferences.getAuthMode(context) }

    CrossmintSDK.configure(
        apiKey = BuildKonfig.CROSSMINT_API_KEY,
        appContext = context,
        logLevel = LogLevel.DEBUG,
    )

    CrossmintNonCustodialSignerProvider {
        val isStaging = BuildKonfig.CROSSMINT_API_KEY.contains("_staging_")
        val isDark = isSystemInDarkTheme()
        MaterialTheme(
            colorScheme =
                if (isDark) {
                    darkColorScheme(
                        primary = Color(0xFF30D158),
                        onPrimary = Color(0xFF002112),
                        primaryContainer = Color(0xFF004D29),
                        onPrimaryContainer = Color(0xFFB9F6CA),
                        secondary = Color(0xFF30D158),
                        onSecondary = Color(0xFF002112),
                        secondaryContainer = Color(0xFF004D29),
                        onSecondaryContainer = Color(0xFFB9F6CA),
                        tertiary = Color(0xFF40C4AA),
                        onTertiary = Color(0xFF003730),
                        background = Color(0xFF000000),
                        surface = Color(0xFF1C1C1E),
                        surfaceVariant = Color(0xFF2C2C2E),
                        onSurface = Color(0xFFF2F2F7),
                        onSurfaceVariant = Color(0xFF8E8E93),
                        outlineVariant = Color(0xFF38383A),
                        error = Color(0xFFFF453A),
                        onError = Color.Black,
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF00C853),
                        onPrimary = Color.White,
                        primaryContainer = Color(0xFFB9F6CA),
                        onPrimaryContainer = Color(0xFF002112),
                        secondary = Color(0xFF00C853),
                        onSecondary = Color.White,
                        secondaryContainer = Color(0xFFB9F6CA),
                        onSecondaryContainer = Color(0xFF002112),
                        tertiary = Color(0xFF00BFA5),
                        onTertiary = Color.White,
                        background = Color(0xFFF2F2F7),
                        surface = Color.White,
                        surfaceVariant = Color(0xFFF5F5F5),
                        onSurface = Color(0xFF1C1C1E),
                        onSurfaceVariant = Color(0xFF6C6C70),
                        outlineVariant = Color(0xFFE5E5EA),
                    )
                },
        ) {
            Box(modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                DemoApp(
                    authMode = authMode,
                    isStaging = isStaging,
                    onAuthModeChange = { isBYOA ->
                        val newMode = if (isBYOA) AuthMode.BYOA else AuthMode.CROSSMINT
                        AuthModePreferences.setAuthModeAndRestart(context, newMode)
                    },
                )
            }
        }
    }
}
