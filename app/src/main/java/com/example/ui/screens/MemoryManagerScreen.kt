package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BoostReport
import com.example.data.LiveTelemetry
import com.example.ui.components.ArcMetricGauge
import com.example.ui.theme.LocalPulseStyle
import com.example.ui.theme.pulseCardBorder
import com.example.ui.theme.pulseCardContainerColor
import java.util.Locale

@Composable
fun MemoryManagerScreen(
    isWideLayout: Boolean,
    telemetry: LiveTelemetry,
    isBoosting: Boolean,
    lastBoostReport: BoostReport?,
    onTrimMemory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = LocalPulseStyle.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(style.sectionSpacing)
    ) {
        if (isWideLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RamPressureCard(
                        telemetry = telemetry,
                        isBoosting = isBoosting,
                        lastBoostReport = lastBoostReport,
                        onTrimMemory = onTrimMemory
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SystemProcessControlsCard(context = context, telemetry = telemetry)
                }
            }
        } else {
            RamPressureCard(
                telemetry = telemetry,
                isBoosting = isBoosting,
                lastBoostReport = lastBoostReport,
                onTrimMemory = onTrimMemory
            )
            SystemProcessControlsCard(context = context, telemetry = telemetry)
        }
    }
}

@Composable
private fun RamPressureCard(
    telemetry: LiveTelemetry,
    isBoosting: Boolean,
    lastBoostReport: BoostReport?,
    onTrimMemory: () -> Unit
) {
    val style = LocalPulseStyle.current
    val usedPct = (telemetry.ramUsageFraction * 100).toInt()
    val gaugeColor = when {
        telemetry.isLowMemory || usedPct >= 88 -> MaterialTheme.colorScheme.error
        usedPct >= 72 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = style.cardShape,
        colors = CardDefaults.cardColors(containerColor = pulseCardContainerColor()),
        border = pulseCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(style.cardPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Live Memory Manager",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArcMetricGauge(
                    progress = telemetry.ramUsageFraction,
                    valueText = "$usedPct%",
                    labelText = "RAM USED",
                    accentColor = gaugeColor,
                    gaugeSize = 132.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MemoryStatRow("Available RAM", "${telemetry.ramAvailMb} MB")
                    MemoryStatRow("Used RAM", "${telemetry.ramUsedMb} MB")
                    MemoryStatRow("Total RAM", "${telemetry.ramTotalMb} MB")
                    MemoryStatRow(
                        "PulseCore Footprint",
                        String.format(Locale.US, "%.1f MB", telemetry.appPssMb)
                    )
                    MemoryStatRow(
                        "Memory Pressure",
                        if (telemetry.isLowMemory) "High (Near Threshold)" else "Healthy"
                    )
                }
            }

            Button(
                onClick = onTrimMemory,
                enabled = !isBoosting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("trim_memory_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isBoosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trimming Memory Buffers...")
                } else {
                    Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trim Memory & Flush App Caches")
                }
            }

            lastBoostReport?.let { report ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "Last Trim: ${report.ramAvailBeforeMb} MB -> ${report.ramAvailAfterMb} MB free",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemProcessControlsCard(
    context: Context,
    telemetry: LiveTelemetry
) {
    val style = LocalPulseStyle.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = style.cardShape,
        colors = CardDefaults.cardColors(containerColor = pulseCardContainerColor()),
        border = pulseCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(style.cardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Background Process Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Text(
                    text = telemetry.processNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }

            SystemShortcutButton(
                title = "Manage Installed Apps & Force Stop",
                subtitle = "Inspect heavy background apps in Android Settings",
                icon = Icons.Default.Tune,
                testTag = "open_apps_settings_button",
                onClick = {
                    launchSafeSettings(context, Settings.ACTION_APPLICATION_SETTINGS)
                }
            )

            SystemShortcutButton(
                title = "Battery & Background Restrictions",
                subtitle = "Restrict background drain before heavy gaming",
                icon = Icons.Default.Memory,
                testTag = "open_battery_settings_button",
                onClick = {
                    launchSafeSettings(context, Settings.ACTION_BATTERY_SAVER_SETTINGS)
                }
            )

            SystemShortcutButton(
                title = "Developer Running Services",
                subtitle = "View live system RAM consumers (if Developer Options enabled)",
                icon = Icons.Default.DeveloperMode,
                testTag = "open_dev_settings_button",
                onClick = {
                    launchSafeSettings(context, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                }
            )
        }
    }
}

@Composable
private fun SystemShortcutButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = icon, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open system settings",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MemoryStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

fun launchSafeSettings(context: Context, action: String) {
    val primary = Intent(action).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(primary)
    } catch (_: Exception) {
        val fallback = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(fallback) }
    }
}
