package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BobAppearance
import com.example.data.DeviceProfile
import com.example.data.LiveTelemetry
import com.example.data.ThermalSeverity
import com.example.ui.theme.LocalPulseStyle
import com.example.ui.theme.pulseCardBorder
import com.example.ui.theme.pulseCardContainerColor
import java.util.Locale

@Composable
fun DeviceIdentityHeader(
    profile: DeviceProfile,
    telemetry: LiveTelemetry,
    useFahrenheit: Boolean,
    bobAppearance: BobAppearance,
    onStyleClick: () -> Unit,
    onBobClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = LocalPulseStyle.current
    val tempDisplay = if (useFahrenheit) {
        String.format(Locale.US, "%.1f°F", telemetry.batteryTempC * 9f / 5f + 32f)
    } else {
        String.format(Locale.US, "%.1f°C", telemetry.batteryTempC)
    }

    val thermalColor = when (telemetry.thermalSeverity) {
        ThermalSeverity.COOL -> MaterialTheme.colorScheme.tertiary
        ThermalSeverity.BALANCED -> MaterialTheme.colorScheme.primary
        ThermalSeverity.WARM -> MaterialTheme.colorScheme.secondary
        ThermalSeverity.THROTTLING -> MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = style.cardShape,
        color = pulseCardContainerColor(MaterialTheme.colorScheme.surfaceVariant),
        border = pulseCardBorder(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (profile.isTablet) {
                            Icons.Default.TabletAndroid
                        } else {
                            Icons.Default.PhoneAndroid
                        },
                        contentDescription = if (profile.isTablet) "Tablet form factor" else "Phone form factor",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.brandDisplay,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${profile.brandTagline} • ${profile.androidVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        StatusChip(
                            icon = Icons.Default.Thermostat,
                            text = tempDisplay,
                            tint = thermalColor
                        )
                        StatusChip(
                            icon = Icons.Default.Bolt,
                            text = "${telemetry.measuredFps.toInt()} FPS",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(onClick = onStyleClick)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("open_style_studio_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Open Style Studio",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Style",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(onClick = onBobClick)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("header_bob_shortcut")
                ) {
                    BobAvatar(
                        appearance = bobAppearance,
                        isActive = false,
                        modifier = Modifier.size(30.dp)
                    )
                    Text(
                        text = "Ask Bob",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: androidx.compose.ui.graphics.Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            fontWeight = FontWeight.SemiBold
        )
    }
}
