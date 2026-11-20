package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BoostReport
import com.example.data.BoostSectionId
import com.example.data.DeviceProfile
import com.example.data.LiveTelemetry
import com.example.data.ThermalAiState
import com.example.data.ThermalSeverity
import com.example.ui.components.ArcMetricGauge
import com.example.ui.components.FpsCadenceSparkline
import com.example.ui.theme.LocalPulseStyle
import com.example.ui.theme.pulseCardBorder
import com.example.ui.theme.pulseCardContainerColor
import java.util.Locale

@Composable
fun BoostThermalScreen(
    isWideLayout: Boolean,
    sectionOrder: List<BoostSectionId>,
    hiddenSections: Set<BoostSectionId>,
    profile: DeviceProfile,
    telemetry: LiveTelemetry,
    targetTempC: Float,
    autoFrameGuard: Boolean,
    useFahrenheit: Boolean,
    isBoosting: Boolean,
    lastBoostReport: BoostReport?,
    thermalAiState: ThermalAiState,
    onRunBoost: () -> Unit,
    onToggleAutoFrameGuard: (Boolean) -> Unit,
    onTargetTempChanged: (Float) -> Unit,
    onTargetTempCommitted: () -> Unit,
    onToggleUnit: (Boolean) -> Unit,
    onRefreshAiOverview: () -> Unit,
    onAskBobAboutThermal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = LocalPulseStyle.current
    val scrollState = rememberScrollState()
    val visibleSections = sectionOrder.filter { it !in hiddenSections }

    @Composable
    fun RenderSection(section: BoostSectionId) {
        when (section) {
            BoostSectionId.ENGINE -> GamingBoostCard(
                profile = profile,
                telemetry = telemetry,
                autoFrameGuard = autoFrameGuard,
                isBoosting = isBoosting,
                lastBoostReport = lastBoostReport,
                onRunBoost = onRunBoost,
                onToggleAutoFrameGuard = onToggleAutoFrameGuard
            )
            BoostSectionId.THERMAL_SLIDER -> ThermalSliderCard(
                telemetry = telemetry,
                targetTempC = targetTempC,
                useFahrenheit = useFahrenheit,
                onTargetTempChanged = onTargetTempChanged,
                onTargetTempCommitted = onTargetTempCommitted,
                onToggleUnit = onToggleUnit
            )
            BoostSectionId.AI_OVERVIEW -> ThermalAiOverviewCard(
                targetTempC = targetTempC,
                useFahrenheit = useFahrenheit,
                thermalAiState = thermalAiState,
                onRefresh = onRefreshAiOverview,
                onAskBob = onAskBobAboutThermal
            )
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(style.sectionSpacing)
    ) {
        if (isWideLayout && visibleSections.size > 1) {
            val leftSections = visibleSections.filterIndexed { index, _ -> index % 2 == 0 }
            val rightSections = visibleSections.filterIndexed { index, _ -> index % 2 == 1 }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(style.sectionSpacing),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(style.sectionSpacing)
                ) {
                    leftSections.forEach { RenderSection(it) }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(style.sectionSpacing)
                ) {
                    rightSections.forEach { RenderSection(it) }
                }
            }
        } else {
            visibleSections.forEach { RenderSection(it) }
        }
    }
}

@Composable
private fun GamingBoostCard(
    profile: DeviceProfile,
    telemetry: LiveTelemetry,
    autoFrameGuard: Boolean,
    isBoosting: Boolean,
    lastBoostReport: BoostReport?,
    onRunBoost: () -> Unit,
    onToggleAutoFrameGuard: (Boolean) -> Unit
) {
    val style = LocalPulseStyle.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = style.cardShape,
        colors = CardDefaults.cardColors(containerColor = pulseCardContainerColor()),
        border = pulseCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(style.cardPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Gaming Performance Engine",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${profile.cpuCores} CPU cores • ${telemetry.gameModeLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArcMetricGauge(
                    progress = (telemetry.measuredFps / 120f).coerceIn(0.1f, 1f),
                    valueText = "${telemetry.measuredFps.toInt()}",
                    labelText = "LIVE FPS",
                    accentColor = MaterialTheme.colorScheme.primary,
                    gaugeSize = 114.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Frame Pacing (${String.format(Locale.US, "%.1f", telemetry.frameTimeMs)} ms)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FpsCadenceSparkline(
                        samples = telemetry.fpsHistory,
                        lineColor = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "RAM Ready: ${telemetry.ramAvailMb} MB free",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                            text = "Auto Frame-Rate Guard",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Prioritizes frame stability & trims render buffers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoFrameGuard,
                        onCheckedChange = onToggleAutoFrameGuard,
                        modifier = Modifier.testTag("auto_frame_guard_switch")
                    )
                }
            }

            Button(
                onClick = onRunBoost,
                enabled = !isBoosting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("boost_now_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isBoosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Optimizing Cache & Frame Buffers...")
                } else {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Optimize for Gaming")
                }
            }

            AnimatedVisibility(visible = lastBoostReport != null) {
                lastBoostReport?.let { report ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Optimization Complete • ${report.thermalHeadroomLabel}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = report.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThermalSliderCard(
    telemetry: LiveTelemetry,
    targetTempC: Float,
    useFahrenheit: Boolean,
    onTargetTempChanged: (Float) -> Unit,
    onTargetTempCommitted: () -> Unit,
    onToggleUnit: (Boolean) -> Unit
) {
    val style = LocalPulseStyle.current
    val haptics = LocalHapticFeedback.current
    val targetLabel = formatTemp(targetTempC, useFahrenheit)
    val liveLabel = formatTemp(telemetry.batteryTempC, useFahrenheit)

    val (profileBadge, accentColor) = when {
        targetTempC <= 36.5f -> "Cool Endurance (Long Sessions)" to MaterialTheme.colorScheme.tertiary
        targetTempC <= 40.5f -> "Balanced Gaming (Steady 60–90 FPS)" to MaterialTheme.colorScheme.primary
        else -> "Peak Frame Rate (Max Boost Envelope)" to MaterialTheme.colorScheme.error
    }

    val liveSeverityColor = when (telemetry.thermalSeverity) {
        ThermalSeverity.COOL -> MaterialTheme.colorScheme.tertiary
        ThermalSeverity.BALANCED -> MaterialTheme.colorScheme.primary
        ThermalSeverity.WARM -> MaterialTheme.colorScheme.secondary
        ThermalSeverity.THROTTLING -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = style.cardShape,
        colors = CardDefaults.cardColors(containerColor = pulseCardContainerColor()),
        border = pulseCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(style.cardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = accentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thermal Target Slider",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                FilterChip(
                    selected = useFahrenheit,
                    onClick = { onToggleUnit(!useFahrenheit) },
                    label = { Text(if (useFahrenheit) "°F" else "°C") },
                    modifier = Modifier.testTag("temp_unit_toggle")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Target Ceiling",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = targetLabel,
                        style = MaterialTheme.typography.displayLarge,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Live Sensor",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$liveLabel • ${telemetry.thermalStatusLabel}",
                        style = MaterialTheme.typography.labelLarge,
                        color = liveSeverityColor
                    )
                }
            }

            Slider(
                value = targetTempC,
                onValueChange = { newValue ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTargetTempChanged(newValue)
                },
                onValueChangeFinished = onTargetTempCommitted,
                valueRange = 34.0f..45.0f,
                steps = 21,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor
                ),
                modifier = Modifier.testTag("thermal_target_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTemp(34f, useFahrenheit),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = profileBadge,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatTemp(45f, useFahrenheit),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThermalAiOverviewCard(
    targetTempC: Float,
    useFahrenheit: Boolean,
    thermalAiState: ThermalAiState,
    onRefresh: () -> Unit,
    onAskBob: () -> Unit
) {
    val style = LocalPulseStyle.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = style.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = pulseCardContainerColor(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            )
        ),
        border = pulseCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(style.cardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Gemini Thermal AI Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Target: ${formatTemp(targetTempC, useFahrenheit)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_thermal_ai_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh AI Overview",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refresh")
                }
            }

            when (thermalAiState) {
                ThermalAiState.Idle -> {
                    Text(
                        text = "Move the temperature slider above to generate a live Gemini breakdown of how that target helps your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ThermalAiState.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Analyzing device thermals and frame-rate headroom with Gemini...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is ThermalAiState.Streaming -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = thermalAiState.text,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                is ThermalAiState.Success -> {
                    Text(
                        text = thermalAiState.text,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                is ThermalAiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = thermalAiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = onAskBob,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ask_bob_thermal_button")
            ) {
                Text("Ask Bob for Deeper Troubleshooting")
            }
        }
    }
}

private fun formatTemp(tempC: Float, useFahrenheit: Boolean): String {
    return if (useFahrenheit) {
        String.format(Locale.US, "%.1f°F", tempC * 9f / 5f + 32f)
    } else {
        String.format(Locale.US, "%.1f°C", tempC)
    }
}
