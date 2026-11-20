package com.example.ui.components

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppColorPalette
import com.example.data.AppNavStyle
import com.example.data.AppThemeMode
import com.example.data.BackgroundType
import com.example.data.BoostSectionId
import com.example.data.CardCornerStyle
import com.example.data.CardDensity
import com.example.data.CardSurfaceStyle
import com.example.data.GradientPreset
import com.example.data.StyleConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleStudioSheet(
    styleConfig: StyleConfig,
    onUpdateStyle: ((StyleConfig) -> StyleConfig) -> Unit,
    onMoveBoostSection: (BoostSectionId, Boolean) -> Unit,
    onToggleBoostSectionVisibility: (BoostSectionId) -> Unit,
    onImportBackgroundPhoto: (Uri?) -> Unit,
    onClearBackgroundPhoto: () -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStudioTab by rememberSaveable { mutableIntStateOf(0) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = onImportBackgroundPhoto
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PulseCore Style Studio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize layout, look, and background live",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = onResetDefaults,
                    modifier = Modifier.testTag("style_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }

            TabRow(selectedTabIndex = selectedStudioTab) {
                Tab(
                    selected = selectedStudioTab == 0,
                    onClick = { selectedStudioTab = 0 },
                    text = { Text("Layout") },
                    icon = { Icon(Icons.Default.ViewAgenda, contentDescription = null) },
                    modifier = Modifier.testTag("studio_tab_layout")
                )
                Tab(
                    selected = selectedStudioTab == 1,
                    onClick = { selectedStudioTab = 1 },
                    text = { Text("Look") },
                    icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                    modifier = Modifier.testTag("studio_tab_look")
                )
                Tab(
                    selected = selectedStudioTab == 2,
                    onClick = { selectedStudioTab = 2 },
                    text = { Text("Background") },
                    icon = { Icon(Icons.Default.Wallpaper, contentDescription = null) },
                    modifier = Modifier.testTag("studio_tab_background")
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedStudioTab) {
                    0 -> LayoutStudioTab(
                        styleConfig = styleConfig,
                        onUpdateStyle = onUpdateStyle,
                        onMoveBoostSection = onMoveBoostSection,
                        onToggleBoostSectionVisibility = onToggleBoostSectionVisibility
                    )
                    1 -> LookStudioTab(
                        styleConfig = styleConfig,
                        onUpdateStyle = onUpdateStyle
                    )
                    else -> BackgroundStudioTab(
                        styleConfig = styleConfig,
                        onUpdateStyle = onUpdateStyle,
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onClearPhoto = onClearBackgroundPhoto
                    )
                }

                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("style_studio_done_button")
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun LayoutStudioTab(
    styleConfig: StyleConfig,
    onUpdateStyle: ((StyleConfig) -> StyleConfig) -> Unit,
    onMoveBoostSection: (BoostSectionId, Boolean) -> Unit,
    onToggleBoostSectionVisibility: (BoostSectionId) -> Unit
) {
    StudioSectionTitle("Card Density")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CardDensity.entries.forEach { density ->
            FilterChip(
                selected = styleConfig.cardDensity == density,
                onClick = { onUpdateStyle { it.copy(cardDensity = density) } },
                label = { Text(density.label) },
                modifier = Modifier.testTag("density_${density.name.lowercase()}")
            )
        }
    }

    StudioSectionTitle("Card Corner Shape")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CardCornerStyle.entries.forEach { corner ->
            FilterChip(
                selected = styleConfig.cardCornerStyle == corner,
                onClick = { onUpdateStyle { it.copy(cardCornerStyle = corner) } },
                label = { Text(corner.label) },
                modifier = Modifier.testTag("corner_${corner.name.lowercase()}")
            )
        }
    }

    StudioSectionTitle("Navigation Bar Style")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppNavStyle.entries.forEach { nav ->
            FilterChip(
                selected = styleConfig.navStyle == nav,
                onClick = { onUpdateStyle { it.copy(navStyle = nav) } },
                label = { Text(nav.label) },
                modifier = Modifier.testTag("navstyle_${nav.name.lowercase()}")
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Two-Column Tablet Grid",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Split cards side-by-side on wide screens",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = styleConfig.wideGridEnabled,
                onCheckedChange = { enabled ->
                    onUpdateStyle { it.copy(wideGridEnabled = enabled) }
                },
                modifier = Modifier.testTag("wide_grid_switch")
            )
        }
    }

    StudioSectionTitle("Boost Dashboard Section Order")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        styleConfig.boostCardOrder.forEachIndexed { index, section ->
            val isHidden = section in styleConfig.hiddenBoostSections
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}. ${section.label}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isHidden) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onMoveBoostSection(section, true) },
                        enabled = index > 0,
                        modifier = Modifier.testTag("move_up_${section.name.lowercase()}")
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move ${section.label} up")
                    }
                    IconButton(
                        onClick = { onMoveBoostSection(section, false) },
                        enabled = index < styleConfig.boostCardOrder.lastIndex,
                        modifier = Modifier.testTag("move_down_${section.name.lowercase()}")
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move ${section.label} down")
                    }
                    IconButton(
                        onClick = { onToggleBoostSectionVisibility(section) },
                        modifier = Modifier.testTag("toggle_vis_${section.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isHidden) "Show ${section.label}" else "Hide ${section.label}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LookStudioTab(
    styleConfig: StyleConfig,
    onUpdateStyle: ((StyleConfig) -> StyleConfig) -> Unit
) {
    StudioSectionTitle("Theme Mode")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppThemeMode.entries.forEach { mode ->
            FilterChip(
                selected = styleConfig.themeMode == mode,
                onClick = { onUpdateStyle { it.copy(themeMode = mode) } },
                label = { Text(mode.label) },
                modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
            )
        }
    }

    StudioSectionTitle("Accent Color Palette")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppColorPalette.entries.forEach { palette ->
            val selected = styleConfig.colorPalette == palette
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(palette.previewHex))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { onUpdateStyle { it.copy(colorPalette = palette) } }
                        .testTag("palette_${palette.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "${palette.label} selected",
                            tint = Color.White
                        )
                    }
                }
                Text(
                    text = palette.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    StudioSectionTitle("Card Surface Finish")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CardSurfaceStyle.entries.forEach { surface ->
            FilterChip(
                selected = styleConfig.cardSurfaceStyle == surface,
                onClick = { onUpdateStyle { it.copy(cardSurfaceStyle = surface) } },
                label = { Text(surface.label) },
                modifier = Modifier.testTag("surface_${surface.name.lowercase()}")
            )
        }
    }
}

@Composable
private fun BackgroundStudioTab(
    styleConfig: StyleConfig,
    onUpdateStyle: ((StyleConfig) -> StyleConfig) -> Unit,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit
) {
    val bg = styleConfig.backgroundConfig

    StudioSectionTitle("Background Mode")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BackgroundType.entries.forEach { type ->
            FilterChip(
                selected = bg.backgroundType == type,
                onClick = {
                    if (type == BackgroundType.CUSTOM_PHOTO && bg.customPhotoPath == null) {
                        onPickPhoto()
                    } else {
                        onUpdateStyle {
                            it.copy(backgroundConfig = it.backgroundConfig.copy(backgroundType = type))
                        }
                    }
                },
                label = { Text(type.label) },
                modifier = Modifier.testTag("bg_type_${type.name.lowercase()}")
            )
        }
    }

    StudioSectionTitle("Gradient Presets")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GradientPreset.entries.forEach { preset ->
            val selected = bg.backgroundType == BackgroundType.GRADIENT && bg.gradientPreset == preset
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 88.dp, height = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(preset.startHex),
                                    Color(preset.midHex),
                                    Color(preset.endHex)
                                )
                            )
                        )
                        .border(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            onUpdateStyle {
                                it.copy(
                                    backgroundConfig = it.backgroundConfig.copy(
                                        backgroundType = BackgroundType.GRADIENT,
                                        gradientPreset = preset
                                    )
                                )
                            }
                        }
                        .testTag("gradient_${preset.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "${preset.label} selected",
                            tint = Color.White
                        )
                    }
                }
                Text(
                    text = preset.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    StudioSectionTitle("Custom Photo Wallpaper")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onPickPhoto,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("pick_bg_photo_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(imageVector = Icons.Default.Image, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (bg.customPhotoPath != null) "Change Photo" else "Choose Photo")
        }

        if (bg.customPhotoPath != null) {
            FilledTonalButton(
                onClick = onClearPhoto,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("clear_bg_photo_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Remove custom photo")
            }
        }
    }

    if (bg.backgroundType == BackgroundType.CUSTOM_PHOTO && bg.customPhotoPath != null) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Readability Dim Overlay (${(bg.photoDimAlpha * 100).toInt()}%)",
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = bg.photoDimAlpha,
                onValueChange = { alpha ->
                    onUpdateStyle {
                        it.copy(backgroundConfig = it.backgroundConfig.copy(photoDimAlpha = alpha))
                    }
                },
                valueRange = 0.10f..0.85f,
                modifier = Modifier.testTag("bg_dim_slider")
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Wallpaper Blur (${bg.photoBlurDp.toInt()} dp)",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = bg.photoBlurDp,
                    onValueChange = { blur ->
                        onUpdateStyle {
                            it.copy(backgroundConfig = it.backgroundConfig.copy(photoBlurDp = blur))
                        }
                    },
                    valueRange = 0f..24f,
                    modifier = Modifier.testTag("bg_blur_slider")
                )
            }
        }
    }
}

@Composable
private fun StudioSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}
