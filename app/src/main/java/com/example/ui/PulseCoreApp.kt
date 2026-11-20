package com.example.ui

import android.graphics.BitmapFactory
import android.os.Build
import android.view.Choreographer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppNavStyle
import com.example.data.AppTab
import com.example.data.BackgroundConfig
import com.example.data.BackgroundType
import com.example.ui.components.BobAvatar
import com.example.ui.components.DeviceIdentityHeader
import com.example.ui.components.StyleStudioSheet
import com.example.ui.screens.BobAssistantScreen
import com.example.ui.screens.BoostThermalScreen
import com.example.ui.screens.MemoryManagerScreen
import com.example.ui.screens.StorageManagerScreen
import com.example.ui.theme.LocalPulseStyle
import com.example.ui.theme.PulseCoreTheme
import com.example.ui.theme.pulseCardBorder
import com.example.ui.theme.pulseCardContainerColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PulseCoreApp(
    viewModel: PulseCoreViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val isBoosting by viewModel.isBoosting.collectAsStateWithLifecycle()
    val lastBoostReport by viewModel.lastBoostReport.collectAsStateWithLifecycle()
    val thermalAiState by viewModel.thermalAiState.collectAsStateWithLifecycle()
    val scannedFolders by viewModel.scannedFolders.collectAsStateWithLifecycle()
    val scannedFiles by viewModel.scannedFiles.collectAsStateWithLifecycle()
    val storageFilter by viewModel.storageFilter.collectAsStateWithLifecycle()
    val selectedFileUris by viewModel.selectedFileUris.collectAsStateWithLifecycle()
    val storageScanState by viewModel.storageScanState.collectAsStateWithLifecycle()
    val storageBanner by viewModel.storageStatusBanner.collectAsStateWithLifecycle()
    val bobChatState by viewModel.bobChatState.collectAsStateWithLifecycle()

    var showStyleStudio by rememberSaveable { mutableStateOf(false) }

    // Live Choreographer frame cadence sampler
    DisposableEffect(Unit) {
        val choreographer = Choreographer.getInstance()
        var frameCount = 0
        var windowStartNs = 0L
        var running = true

        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!running) return
                if (windowStartNs == 0L) {
                    windowStartNs = frameTimeNanos
                } else {
                    frameCount++
                    val elapsedNs = frameTimeNanos - windowStartNs
                    if (elapsedNs >= 1_000_000_000L) {
                        val fps = (frameCount * 1_000_000_000f) / elapsedNs.toFloat()
                        val avgFrameMs = (elapsedNs / 1_000_000f) / frameCount.coerceAtLeast(1)
                        viewModel.onFrameMetricsSample(fps, avgFrameMs)
                        frameCount = 0
                        windowStartNs = frameTimeNanos
                    }
                }
                choreographer.postFrameCallback(this)
            }
        }
        choreographer.postFrameCallback(callback)

        onDispose {
            running = false
            choreographer.removeFrameCallback(callback)
        }
    }

    if (selectedTab != AppTab.BOOST) {
        BackHandler {
            viewModel.selectTab(AppTab.BOOST)
        }
    }

    val styleConfig = prefs.styleConfig

    PulseCoreTheme(styleConfig = styleConfig) {
        val pulseStyle = LocalPulseStyle.current

        if (showStyleStudio) {
            StyleStudioSheet(
                styleConfig = styleConfig,
                onUpdateStyle = viewModel::updateStyleConfig,
                onMoveBoostSection = viewModel::moveBoostSection,
                onToggleBoostSectionVisibility = viewModel::toggleBoostSectionVisibility,
                onImportBackgroundPhoto = viewModel::importCustomBackgroundPhoto,
                onClearBackgroundPhoto = viewModel::clearCustomBackgroundPhoto,
                onResetDefaults = viewModel::resetStyleToDefaults,
                onDismiss = { showStyleStudio = false }
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTabletWidth = maxWidth >= 600.dp
            val isWideLayout = isTabletWidth && styleConfig.wideGridEnabled
            val useSideRail = when (styleConfig.navStyle) {
                AppNavStyle.AUTO -> isTabletWidth
                AppNavStyle.SIDE_RAIL -> true
                AppNavStyle.BOTTOM_BAR, AppNavStyle.FLOATING_PILL -> false
            }

            PulseBackgroundLayer(backgroundConfig = styleConfig.backgroundConfig)

            if (useSideRail) {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavigationRail(
                            modifier = Modifier.fillMaxHeight(),
                            containerColor = pulseCardContainerColor(MaterialTheme.colorScheme.surface)
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            AppTab.entries.forEach { tab ->
                                val selected = selectedTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        TabIcon(
                                            tab = tab,
                                            selected = selected,
                                            bobAppearance = prefs.bobAppearance,
                                            bobActive = bobChatState.isGenerating
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 1120.dp),
                                verticalArrangement = Arrangement.spacedBy(pulseStyle.sectionSpacing)
                            ) {
                                DeviceIdentityHeader(
                                    profile = viewModel.deviceProfile,
                                    telemetry = telemetry,
                                    useFahrenheit = prefs.useFahrenheit,
                                    bobAppearance = prefs.bobAppearance,
                                    onStyleClick = { showStyleStudio = true },
                                    onBobClick = { viewModel.selectTab(AppTab.BOB) }
                                )

                                PulseCoreTabContent(
                                    selectedTab = selectedTab,
                                    isWideLayout = isWideLayout,
                                    viewModel = viewModel,
                                    prefs = prefs,
                                    telemetry = telemetry,
                                    isBoosting = isBoosting,
                                    lastBoostReport = lastBoostReport,
                                    thermalAiState = thermalAiState,
                                    scannedFolders = scannedFolders,
                                    scannedFiles = scannedFiles,
                                    storageFilter = storageFilter,
                                    selectedFileUris = selectedFileUris,
                                    storageScanState = storageScanState,
                                    storageBanner = storageBanner,
                                    bobChatState = bobChatState,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } else {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        if (styleConfig.navStyle == AppNavStyle.FLOATING_PILL) {
                            FloatingPillBottomNav(
                                selectedTab = selectedTab,
                                bobAppearance = prefs.bobAppearance,
                                bobActive = bobChatState.isGenerating,
                                onSelectTab = viewModel::selectTab
                            )
                        } else {
                            NavigationBar(
                                containerColor = pulseCardContainerColor(MaterialTheme.colorScheme.surface)
                            ) {
                                AppTab.entries.forEach { tab ->
                                    val selected = selectedTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { viewModel.selectTab(tab) },
                                        icon = {
                                            TabIcon(
                                                tab = tab,
                                                selected = selected,
                                                bobAppearance = prefs.bobAppearance,
                                                bobActive = bobChatState.isGenerating
                                            )
                                        },
                                        label = { Text(tab.label) },
                                        modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(pulseStyle.sectionSpacing)
                    ) {
                        DeviceIdentityHeader(
                            profile = viewModel.deviceProfile,
                            telemetry = telemetry,
                            useFahrenheit = prefs.useFahrenheit,
                            bobAppearance = prefs.bobAppearance,
                            onStyleClick = { showStyleStudio = true },
                            onBobClick = { viewModel.selectTab(AppTab.BOB) }
                        )

                        PulseCoreTabContent(
                            selectedTab = selectedTab,
                            isWideLayout = isWideLayout,
                            viewModel = viewModel,
                            prefs = prefs,
                            telemetry = telemetry,
                            isBoosting = isBoosting,
                            lastBoostReport = lastBoostReport,
                            thermalAiState = thermalAiState,
                            scannedFolders = scannedFolders,
                            scannedFiles = scannedFiles,
                            storageFilter = storageFilter,
                            selectedFileUris = selectedFileUris,
                            storageScanState = storageScanState,
                            storageBanner = storageBanner,
                            bobChatState = bobChatState,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PulseBackgroundLayer(backgroundConfig: BackgroundConfig) {
    when (backgroundConfig.backgroundType) {
        BackgroundType.ADAPTIVE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }
        BackgroundType.GRADIENT -> {
            val preset = backgroundConfig.gradientPreset
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(preset.startHex),
                                Color(preset.midHex),
                                Color(preset.endHex)
                            )
                        )
                    )
            )
        }
        BackgroundType.CUSTOM_PHOTO -> {
            val bitmap by produceState<ImageBitmap?>(
                initialValue = null,
                key1 = backgroundConfig.customPhotoPath,
                key2 = backgroundConfig.photoRevision
            ) {
                value = withContext(Dispatchers.IO) {
                    val path = backgroundConfig.customPhotoPath ?: return@withContext null
                    val file = File(path)
                    if (!file.exists()) return@withContext null
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val loaded = bitmap
                if (loaded != null) {
                    val blurModifier = if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        backgroundConfig.photoBlurDp > 0.5f
                    ) {
                        Modifier.blur(backgroundConfig.photoBlurDp.dp)
                    } else {
                        Modifier
                    }
                    Image(
                        bitmap = loaded,
                        contentDescription = "Custom app background wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(blurModifier)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = backgroundConfig.photoDimAlpha))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingPillBottomNav(
    selectedTab: AppTab,
    bobAppearance: com.example.data.BobAppearance,
    bobActive: Boolean,
    onSelectTab: (AppTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            shape = CircleShape,
            color = pulseCardContainerColor(MaterialTheme.colorScheme.surface),
            border = pulseCardBorder(),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    val bgColor = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    }
                    val contentColor = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Row(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(bgColor)
                            .clickable { onSelectTab(tab) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("nav_${tab.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TabIcon(
                            tab = tab,
                            selected = selected,
                            bobAppearance = bobAppearance,
                            bobActive = bobActive
                        )
                        if (selected) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = contentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PulseCoreTabContent(
    selectedTab: AppTab,
    isWideLayout: Boolean,
    viewModel: PulseCoreViewModel,
    prefs: com.example.data.PulseCorePreferences,
    telemetry: com.example.data.LiveTelemetry,
    isBoosting: Boolean,
    lastBoostReport: com.example.data.BoostReport?,
    thermalAiState: com.example.data.ThermalAiState,
    scannedFolders: List<com.example.data.ScannedFolder>,
    scannedFiles: List<com.example.data.ScannedFileItem>,
    storageFilter: com.example.data.StorageCategory,
    selectedFileUris: Set<String>,
    storageScanState: com.example.data.StorageScanState,
    storageBanner: String?,
    bobChatState: com.example.data.BobChatState,
    modifier: Modifier = Modifier
) {
    when (selectedTab) {
        AppTab.BOOST -> BoostThermalScreen(
            isWideLayout = isWideLayout,
            sectionOrder = prefs.styleConfig.boostCardOrder,
            hiddenSections = prefs.styleConfig.hiddenBoostSections,
            profile = viewModel.deviceProfile,
            telemetry = telemetry,
            targetTempC = prefs.targetTempC,
            autoFrameGuard = prefs.autoFrameGuard,
            useFahrenheit = prefs.useFahrenheit,
            isBoosting = isBoosting,
            lastBoostReport = lastBoostReport,
            thermalAiState = thermalAiState,
            onRunBoost = viewModel::runGamingBoost,
            onToggleAutoFrameGuard = viewModel::toggleAutoFrameGuard,
            onTargetTempChanged = viewModel::onTargetTempSliderChanged,
            onTargetTempCommitted = viewModel::onTargetTempCommitted,
            onToggleUnit = viewModel::toggleTemperatureUnit,
            onRefreshAiOverview = viewModel::refreshThermalOverviewNow,
            onAskBobAboutThermal = {
                viewModel.selectTab(AppTab.BOB)
                viewModel.sendBobMessage("Explain how my thermal target and current temperature affect gaming performance.")
            },
            modifier = modifier.fillMaxWidth()
        )
        AppTab.MEMORY -> MemoryManagerScreen(
            isWideLayout = isWideLayout,
            telemetry = telemetry,
            isBoosting = isBoosting,
            lastBoostReport = lastBoostReport,
            onTrimMemory = viewModel::runGamingBoost,
            modifier = modifier.fillMaxWidth()
        )
        AppTab.STORAGE -> StorageManagerScreen(
            isWideLayout = isWideLayout,
            telemetry = telemetry,
            scannedFolders = scannedFolders,
            scannedFiles = scannedFiles,
            activeFilter = storageFilter,
            selectedUris = selectedFileUris,
            scanState = storageScanState,
            statusBanner = storageBanner,
            onFolderPicked = viewModel::onFolderPicked,
            onRemoveFolder = viewModel::removeFolder,
            onRescan = viewModel::rescanAuthorizedFolders,
            onSelectFilter = viewModel::setStorageFilter,
            onToggleFileSelection = viewModel::toggleFileSelection,
            onSelectAllFiltered = viewModel::selectAllFilteredFiles,
            onDeleteSelected = viewModel::deleteSelectedFiles,
            onClearAppCache = viewModel::clearAppCacheOnly,
            onDismissBanner = viewModel::dismissStorageBanner,
            modifier = modifier.fillMaxWidth()
        )
        AppTab.BOB -> BobAssistantScreen(
            isWideLayout = isWideLayout,
            appearance = prefs.bobAppearance,
            chatState = bobChatState,
            onUpdateAppearance = viewModel::updateBobAppearance,
            onSendMessage = viewModel::sendBobMessage,
            onClearChat = viewModel::clearBobChat,
            onNavigateToTab = viewModel::selectTab,
            onRunBoost = viewModel::runGamingBoost,
            modifier = modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TabIcon(
    tab: AppTab,
    selected: Boolean,
    bobAppearance: com.example.data.BobAppearance,
    bobActive: Boolean
) {
    if (tab == AppTab.BOB) {
        BobAvatar(
            appearance = bobAppearance,
            isActive = bobActive,
            modifier = Modifier.size(26.dp)
        )
        return
    }
    val icon: ImageVector = when (tab) {
        AppTab.BOOST -> if (selected) Icons.Filled.Bolt else Icons.Outlined.Bolt
        AppTab.MEMORY -> if (selected) Icons.Filled.Memory else Icons.Outlined.Memory
        AppTab.STORAGE -> if (selected) Icons.Filled.SdStorage else Icons.Outlined.SdStorage
        AppTab.BOB -> if (selected) Icons.Filled.SmartToy else Icons.Outlined.SmartToy
    }
    Icon(imageVector = icon, contentDescription = tab.label)
}
