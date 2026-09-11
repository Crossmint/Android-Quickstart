package com.crossmint.kotlin.checkoutdemo.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldPredictiveBackHandler
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
actual fun <T> PaneBackHandler(navigator: ThreePaneScaffoldNavigator<T>) {
    ThreePaneScaffoldPredictiveBackHandler(navigator, BackNavigationBehavior.PopUntilScaffoldValueChange)
}
