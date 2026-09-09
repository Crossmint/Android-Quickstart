package com.crossmint.kotlin.checkoutdemo.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
expect fun <T> PaneBackHandler(navigator: ThreePaneScaffoldNavigator<T>)
