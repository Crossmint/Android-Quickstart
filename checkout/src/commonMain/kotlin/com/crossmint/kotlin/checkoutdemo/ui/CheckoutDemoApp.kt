package com.crossmint.kotlin.checkoutdemo.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crossmint.crossmintcheckoutdemoapp.generated.resources.Res
import com.crossmint.crossmintcheckoutdemoapp.generated.resources.ic_crossmint
import com.crossmint.kotlin.checkoutdemo.PlaygroundUiState
import com.crossmint.kotlin.checkoutdemo.PlaygroundViewModel
import com.crossmint.kotlin.checkoutdemo.model.PlaygroundSection
import com.crossmint.kotlin.checkoutdemo.ui.sections.AppearanceSectionView
import com.crossmint.kotlin.checkoutdemo.ui.sections.EventsSectionView
import com.crossmint.kotlin.checkoutdemo.ui.sections.FieldsSectionView
import com.crossmint.kotlin.checkoutdemo.ui.sections.IdentitySectionView
import com.crossmint.kotlin.checkoutdemo.ui.sections.OrderSectionView
import com.crossmint.kotlin.checkoutdemo.ui.sections.PaymentSectionView
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CheckoutDemoApp(
    viewModel: PlaygroundViewModel,
    apiKey: String,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAbout by remember { mutableStateOf(false) }
    var showEvents by remember { mutableStateOf(false) }

    val directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
    if (directive.maxHorizontalPartitions >= 2) {
        WideScaffold(
            uiState = uiState,
            viewModel = viewModel,
            apiKey = apiKey,
            onShowAbout = { showAbout = true },
            onShowEvents = { showEvents = true },
        )
    } else {
        CompactScaffold(
            uiState = uiState,
            viewModel = viewModel,
            apiKey = apiKey,
            onShowAbout = { showAbout = true },
            onShowEvents = { showEvents = true },
        )
    }

    if (showAbout) {
        AboutSheet(apiKey = apiKey, onDismissRequest = { showAbout = false })
    }

    if (showEvents) {
        ModalBottomSheet(onDismissRequest = { showEvents = false }) {
            EventsSectionView(events = uiState.events, modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaygroundTopBar(
    apiKey: String,
    onShowAbout: () -> Unit,
    onShowEvents: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(Res.drawable.ic_crossmint),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.brandMark,
                    modifier = Modifier.size(24.dp),
                )
                Text("Playground", modifier = Modifier.padding(start = 12.dp))
            }
        },
        navigationIcon = {
            IconButton(onClick = onShowAbout) {
                Icon(Icons.Filled.Info, contentDescription = "About")
            }
        },
        actions = {
            IconButton(onClick = onShowEvents) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Events")
            }
            EnvironmentBadge(apiKey)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactScaffold(
    uiState: PlaygroundUiState,
    viewModel: PlaygroundViewModel,
    apiKey: String,
    onShowAbout: () -> Unit,
    onShowEvents: () -> Unit,
) {
    Scaffold(
        topBar = { PlaygroundTopBar(apiKey = apiKey, onShowAbout = onShowAbout, onShowEvents = onShowEvents) },
        bottomBar = {
            NavigationBar {
                PlaygroundSection.entries.forEach { section ->
                    NavigationBarItem(
                        selected = uiState.section == section,
                        onClick = { viewModel.selectSection(section) },
                        icon = { Icon(section.icon, contentDescription = null) },
                        label = { Text(section.navLabel) },
                    )
                }
            }
        },
    ) { padding ->
        val isPreviewRunning = uiState.isPreviewingCheckout || uiState.isPreviewingIdentity

        if (isPreviewRunning) {
            val scaffoldState = rememberBottomSheetScaffoldState()
            val scope = rememberCoroutineScope()

            BottomSheetScaffold(
                modifier = Modifier.padding(padding).fillMaxSize(),
                scaffoldState = scaffoldState,
                sheetPeekHeight = 112.dp,
                sheetContent = {
                    PreviewSheetContent(
                        section = uiState.section,
                        uiState = uiState,
                        apiKey = apiKey,
                        onEvent = viewModel::logExternalEvent,
                    )
                },
            ) { sheetPadding ->
                SectionConfigPane(
                    section = uiState.section,
                    uiState = uiState,
                    viewModel = viewModel,
                    apiKey = apiKey,
                    onOpenPreview = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                    modifier = Modifier.padding(sheetPadding).fillMaxSize(),
                )
            }
        } else {
            SectionConfigPane(
                section = uiState.section,
                uiState = uiState,
                viewModel = viewModel,
                apiKey = apiKey,
                onOpenPreview = {},
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
@Composable
private fun WideScaffold(
    uiState: PlaygroundUiState,
    viewModel: PlaygroundViewModel,
    apiKey: String,
    onShowAbout: () -> Unit,
    onShowEvents: () -> Unit,
) {
    Scaffold(
        topBar = { PlaygroundTopBar(apiKey = apiKey, onShowAbout = onShowAbout, onShowEvents = onShowEvents) },
    ) { padding ->
        NavigationSuiteScaffold(
            modifier = Modifier.padding(padding).fillMaxSize(),
            navigationSuiteItems = {
                PlaygroundSection.entries.forEach { section ->
                    item(
                        selected = uiState.section == section,
                        onClick = { viewModel.selectSection(section) },
                        icon = { Icon(section.icon, contentDescription = null) },
                        label = { Text(section.navLabel) },
                    )
                }
            },
        ) {
            WidePlaygroundPanes(uiState = uiState, viewModel = viewModel, apiKey = apiKey)
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun WidePlaygroundPanes(
    uiState: PlaygroundUiState,
    viewModel: PlaygroundViewModel,
    apiKey: String,
) {
    val navigator = rememberSupportingPaneScaffoldNavigator<Unit>()

    PaneBackHandler(navigator)

    SupportingPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        mainPane = {
            AnimatedPane {
                SectionConfigPane(
                    section = uiState.section,
                    uiState = uiState,
                    viewModel = viewModel,
                    apiKey = apiKey,
                    onOpenPreview = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
        supportingPane = {
            AnimatedPane {
                CheckoutPreviewHost(
                    section = uiState.section,
                    uiState = uiState,
                    apiKey = apiKey,
                    onEvent = viewModel::logExternalEvent,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
    )
}

@Composable
private fun SectionConfigPane(
    section: PlaygroundSection,
    uiState: PlaygroundUiState,
    viewModel: PlaygroundViewModel,
    apiKey: String,
    onOpenPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (section) {
        PlaygroundSection.ORDER ->
            OrderSectionView(
                uiState = uiState,
                apiKey = apiKey,
                onDraftChange = viewModel::updateDraft,
                onCreateOrder = viewModel::createOrder,
                onExistingOrderChange = viewModel::updateExistingOrder,
                onUseExistingOrder = viewModel::useExistingOrder,
                onOpenPreview = onOpenPreview,
                modifier = modifier,
            )

        PlaygroundSection.PAYMENT ->
            PaymentSectionView(
                options = uiState.options,
                onOptionsChange = viewModel::updateOptions,
                modifier = modifier,
            )

        PlaygroundSection.APPEARANCE ->
            AppearanceSectionView(
                options = uiState.options,
                onOptionsChange = viewModel::updateOptions,
                modifier = modifier,
            )

        PlaygroundSection.FIELDS ->
            FieldsSectionView(
                options = uiState.options,
                onOptionsChange = viewModel::updateOptions,
                modifier = modifier,
            )

        PlaygroundSection.IDENTITY ->
            IdentitySectionView(
                uiState = uiState,
                onCredentialsChange = viewModel::updateIdentityCredentials,
                onOpenPreview = {
                    viewModel.startIdentityPreview()
                    onOpenPreview()
                },
                modifier = modifier,
            )
    }
}
