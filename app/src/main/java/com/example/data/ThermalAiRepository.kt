package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import java.util.Locale

class ThermalAiRepository {

    private val thermalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are PulseCore's thermal engineering advisor. " +
                        "Provide crisp, accurate, plain-English mobile hardware guidance. " +
                        "Explain what the user's selected temperature target does for sustained gaming FPS, " +
                        "thermal throttling, and battery longevity on their specific phone or tablet. " +
                        "Use 3 short sections with bold headings: Frame-Rate & Clock Impact, " +
                        "Battery & Chassis Safety, and Recommended Action. Keep total output under 140 words."
                )
            }
        )
    }

    private val bobModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are Bob, a friendly, sharp, customizable device-care assistant inside the PulseCore Android app. " +
                        "Your job is two-fold: " +
                        "1) Help the user understand PulseCore's features: Boost & Thermal tab (gaming optimization, Choreographer FPS meter, Thermal Target Slider 34-45C, Auto Frame Guard), " +
                        "Memory tab (live RAM monitor, app cache & buffer trim, plus honest shortcuts to Android Running Services/App Settings because Android prevents non-root apps from killing third-party apps), " +
                        "Storage tab (real internal volume metrics and Storage Access Framework folder cleaner for user-selected folders), " +
                        "Style Studio (tap the palette button in the top header to customize theme mode, color palette, card density, dashboard section order, navigation bar style, gradient backgrounds, or custom photo wallpapers), and Bob Studio (customizing Bob's shell, color, eyes, and gear). " +
                        "2) Help diagnose why the user's phone or tablet is running slow, lagging in games, overheating, or low on storage using the live telemetry context provided in each prompt. " +
                        "Be encouraging, practical, concise (under 160 words), and honest about Android OS capabilities. " +
                        "When helpful, suggest one concrete next step inside PulseCore."
                )
            }
        )
    }

    fun streamThermalOverview(
        profile: DeviceProfile,
        telemetry: LiveTelemetry,
        targetTempC: Float,
        autoFrameGuard: Boolean
    ): Flow<String> = flow {
        val prompt = buildString {
            appendLine("Device: ${profile.brandDisplay} (${profile.androidVersion}, ${profile.socOrHardware}, ${if (profile.isTablet) "Tablet" else "Phone"}, ${profile.cpuCores} cores)")
            appendLine("Live Battery/Chassis Temp: ${String.format(Locale.US, "%.1f", telemetry.batteryTempC)}°C (${telemetry.thermalStatusLabel})")
            appendLine("Selected Thermal Target Ceiling: ${String.format(Locale.US, "%.1f", targetTempC)}°C")
            appendLine("Available RAM: ${telemetry.ramAvailMb} MB / ${telemetry.ramTotalMb} MB")
            appendLine("Current Frame Cadence: ${String.format(Locale.US, "%.0f", telemetry.measuredFps)} FPS (${String.format(Locale.US, "%.1f", telemetry.frameTimeMs)} ms)")
            appendLine("Auto Frame-Rate Guard: ${if (autoFrameGuard) "Enabled" else "Disabled"}")
            appendLine()
            append("Summarize exactly what setting the thermal target to ${String.format(Locale.US, "%.1f", targetTempC)}°C does to help this device during heavy gaming.")
        }

        withTimeout(60_000L) {
            thermalModel.generateContentStream(prompt).collect { chunk ->
                val chunkText = chunk.text
                if (!chunkText.isNullOrEmpty()) {
                    emit(chunkText)
                }
            }
        }
    }

    fun streamBobReply(
        userMessage: String,
        priorHistory: List<BobMessage>,
        profile: DeviceProfile,
        telemetry: LiveTelemetry,
        targetTempC: Float,
        autoFrameGuard: Boolean,
        authorizedFolderCount: Int
    ): Flow<String> = flow {
        val historyContent: List<Content> = priorHistory
            .filter { it.text.isNotBlank() && !it.isStreaming }
            .takeLast(10)
            .map { msg ->
                content(if (msg.isFromUser) "user" else "model") {
                    text(msg.text)
                }
            }

        val liveSnapshotPrefix = buildString {
            appendLine("[Live Device Telemetry]")
            appendLine("Device: ${profile.brandDisplay} (${if (profile.isTablet) "Tablet" else "Phone"}, ${profile.androidVersion}, ${profile.socOrHardware})")
            appendLine("Temp: ${String.format(Locale.US, "%.1f", telemetry.batteryTempC)}°C (${telemetry.thermalStatusLabel}), Target Ceiling: ${String.format(Locale.US, "%.1f", targetTempC)}°C")
            appendLine("RAM: ${telemetry.ramUsedMb} MB used / ${telemetry.ramTotalMb} MB total (${telemetry.ramAvailMb} MB free, lowMemory=${telemetry.isLowMemory})")
            appendLine("Internal Storage Free: ${formatBytes(telemetry.storageFreeBytes)} / ${formatBytes(telemetry.storageTotalBytes)}, App Cache: ${formatBytes(telemetry.appCacheBytes)}, SAF Folders Linked: $authorizedFolderCount")
            appendLine("FPS Cadence: ${String.format(Locale.US, "%.0f", telemetry.measuredFps)} FPS, Auto Frame Guard: $autoFrameGuard, Battery: ${telemetry.batteryLevelPct}% (charging=${telemetry.isCharging})")
            appendLine()
            append("User asks: $userMessage")
        }

        withTimeout(60_000L) {
            val chat = bobModel.startChat(history = historyContent)
            chat.sendMessageStream(liveSnapshotPrefix).collect { chunk ->
                val chunkText = chunk.text
                if (!chunkText.isNullOrEmpty()) {
                    emit(chunkText)
                }
            }
        }
    }
}
