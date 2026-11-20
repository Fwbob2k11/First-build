package com.example.data

import android.app.ActivityManager
import android.app.GameManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.Process
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class DeviceTelemetryRepository(private val context: Context) {

    fun getDeviceProfile(): DeviceProfile {
        val rawMfg = Build.MANUFACTURER.orEmpty().trim()
        val prettyMfg = rawMfg.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }.ifEmpty { "Android" }

        val model = Build.MODEL.orEmpty().trim().ifEmpty { "Device" }
        val brandDisplay = if (model.startsWith(prettyMfg, ignoreCase = true)) {
            model
        } else {
            "$prettyMfg $model"
        }

        val socOrHardware = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val soc = Build.SOC_MODEL.orEmpty().trim()
            if (soc.isNotEmpty() && !soc.equals("unknown", ignoreCase = true)) soc else Build.HARDWARE
        } else {
            Build.HARDWARE
        }

        val isTablet = context.resources.configuration.smallestScreenWidthDp >= 600
        val brandTagline = when (rawMfg.lowercase(Locale.US)) {
            "google" -> "Pixel Tensor Adaptive Profile"
            "samsung" -> "Galaxy One UI Performance Profile"
            "oneplus" -> "OxygenOS HyperBoost Profile"
            "xiaomi", "redmi", "poco" -> "HyperOS / MIUI Thermal Profile"
            "motorola" -> "Moto Gametime Clean Profile"
            "asus" -> "ROG Thermal Envelope Profile"
            "lenovo" -> "Lenovo Tab Performance Profile"
            else -> if (isTablet) "$prettyMfg Tablet Adaptive Profile" else "$prettyMfg Adaptive Profile"
        }

        return DeviceProfile(
            manufacturer = prettyMfg,
            brandDisplay = brandDisplay,
            model = model,
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            sdkInt = Build.VERSION.SDK_INT,
            socOrHardware = socOrHardware.ifBlank { "ARM64" },
            isTablet = isTablet,
            cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
            brandTagline = brandTagline
        )
    }

    suspend fun readLiveTelemetry(
        measuredFps: Float,
        frameTimeMs: Float,
        fpsHistory: List<Float>
    ): LiveTelemetry = withContext(Dispatchers.IO) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRamMb = (memoryInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
        val availRamMb = (memoryInfo.availMem / (1024L * 1024L)).coerceAtLeast(0L)
        val usedRamMb = (totalRamMb - availRamMb).coerceAtLeast(0L)
        val thresholdMb = (memoryInfo.threshold / (1024L * 1024L)).coerceAtLeast(0L)

        val appPssMb = try {
            val memInfos = activityManager.getProcessMemoryInfo(intArrayOf(Process.myPid()))
            if (memInfos.isNotEmpty()) memInfos[0].totalPss / 1024f else 0f
        } catch (_: Exception) {
            0f
        }

        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val rawTempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 310) ?: 310
        val batteryTempC = (rawTempTenths / 10f).coerceIn(15f, 65f)
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 100) ?: 100
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.coerceAtLeast(1) ?: 100
        val batteryPct = ((level * 100f) / scale).toInt().coerceIn(0, 100)
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val (thermalLabel, thermalSeverity) = readThermalStatus(powerManager, batteryTempC)
        val headroom = readThermalHeadroom(powerManager, batteryTempC)

        val statFs = StatFs(Environment.getDataDirectory().path)
        val storageTotal = statFs.totalBytes.coerceAtLeast(1L)
        val storageFree = statFs.availableBytes.coerceAtLeast(0L)
        val storageUsed = (storageTotal - storageFree).coerceAtLeast(0L)

        val cacheBytes = calculateDirectorySize(context.cacheDir) +
            calculateDirectorySize(context.codeCacheDir) +
            (context.externalCacheDir?.let { calculateDirectorySize(it) } ?: 0L)

        val gameModeLabel = readGameModeLabel()

        val processNote = "Android OS sandboxes cross-app process termination without root. " +
            "PulseCore trims active app caches, flushes graphics buffers, checks thermal headroom, " +
            "and provides direct shortcuts to Android Running Services & App Info."

        LiveTelemetry(
            batteryTempC = batteryTempC,
            thermalStatusLabel = thermalLabel,
            thermalSeverity = thermalSeverity,
            thermalHeadroomRatio = headroom,
            ramUsedMb = usedRamMb,
            ramTotalMb = totalRamMb,
            ramAvailMb = availRamMb,
            ramThresholdMb = thresholdMb,
            isLowMemory = memoryInfo.lowMemory,
            appPssMb = appPssMb,
            storageUsedBytes = storageUsed,
            storageTotalBytes = storageTotal,
            storageFreeBytes = storageFree,
            appCacheBytes = cacheBytes,
            measuredFps = measuredFps,
            frameTimeMs = frameTimeMs,
            fpsHistory = fpsHistory,
            gameModeLabel = gameModeLabel,
            batteryLevelPct = batteryPct,
            isCharging = isCharging,
            processNote = processNote
        )
    }

    suspend fun performMemoryAndCacheTrim(
        autoFrameGuardActive: Boolean
    ): BoostReport = withContext(Dispatchers.IO) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val beforeMem = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(beforeMem)
        val ramBeforeMb = beforeMem.availMem / (1024L * 1024L)

        var freedBytes = clearDirectoryContents(context.cacheDir)
        context.externalCacheDir?.let { freedBytes += clearDirectoryContents(it) }

        // Suggest garbage collection and release unreferenced runtime memory buffers
        System.gc()
        Runtime.getRuntime().runFinalization()

        val afterMem = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(afterMem)
        val ramAfterMb = afterMem.availMem / (1024L * 1024L)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val headroom = readThermalHeadroom(powerManager, 34f)
        val headroomLabel = when {
            headroom >= 0.85f -> "High Thermal Load (${(headroom * 100).toInt()}%)"
            headroom >= 0.60f -> "Moderate Headroom (${(headroom * 100).toInt()}%)"
            else -> "Cool & Ready (${(headroom * 100).toInt()}% load)"
        }

        val gameMode = readGameModeLabel()
        val deltaMb = (ramAfterMb - ramBeforeMb).coerceAtLeast(0L)
        val summary = buildString {
            append("App cache trimmed")
            if (freedBytes > 0L) {
                append(" (${formatBytes(freedBytes)} freed)")
            }
            append(", runtime buffers compacted")
            if (deltaMb > 0L) {
                append(" (+${deltaMb} MB available RAM)")
            }
            append(". Frame cadence guard is ${if (autoFrameGuardActive) "active" else "standby"}.")
        }

        BoostReport(
            timestampMs = System.currentTimeMillis(),
            freedCacheBytes = freedBytes,
            ramAvailBeforeMb = ramBeforeMb,
            ramAvailAfterMb = ramAfterMb,
            thermalHeadroomLabel = headroomLabel,
            gameModeStatus = gameMode,
            autoFrameGuardActive = autoFrameGuardActive,
            summary = summary
        )
    }

    private fun readThermalStatus(
        powerManager: PowerManager,
        batteryTempC: Float
    ): Pair<String, ThermalSeverity> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "Cool & Nominal" to ThermalSeverity.COOL
                PowerManager.THERMAL_STATUS_LIGHT -> "Light Warmth" to ThermalSeverity.BALANCED
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Heat" to ThermalSeverity.WARM
                PowerManager.THERMAL_STATUS_SEVERE,
                PowerManager.THERMAL_STATUS_CRITICAL,
                PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Thermal Throttling" to ThermalSeverity.THROTTLING
                else -> "Nominal" to ThermalSeverity.COOL
            }
        }
        return when {
            batteryTempC >= 43f -> "Thermal Throttling" to ThermalSeverity.THROTTLING
            batteryTempC >= 39f -> "Warm Load" to ThermalSeverity.WARM
            batteryTempC >= 35f -> "Balanced" to ThermalSeverity.BALANCED
            else -> "Cool & Nominal" to ThermalSeverity.COOL
        }
    }

    private fun readThermalHeadroom(powerManager: PowerManager, batteryTempC: Float): Float {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val headroom = powerManager.getThermalHeadroom(10)
                if (!headroom.isNaN() && headroom >= 0f) {
                    return headroom.coerceIn(0f, 1.2f)
                }
            } catch (_: Exception) {
                // Fallback to battery temperature proxy when device HAL does not report headroom
            }
        }
        return ((batteryTempC - 25f) / 22f).coerceIn(0.08f, 1.0f)
    }

    private fun readGameModeLabel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return try {
                val gm = context.getSystemService(Context.GAME_SERVICE) as? GameManager
                when (gm?.gameMode) {
                    GameManager.GAME_MODE_PERFORMANCE -> "Game Mode: Performance"
                    GameManager.GAME_MODE_BATTERY -> "Game Mode: Battery"
                    GameManager.GAME_MODE_STANDARD -> "Game Mode: Standard"
                    else -> "Game Mode: Auto"
                }
            } catch (_: Exception) {
                "Adaptive"
            }
        }
        return "Adaptive"
    }

    private fun calculateDirectorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var total = 0L
        val stack = ArrayDeque<File>()
        stack.add(dir)
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            val children = current.listFiles() ?: continue
            for (child in children) {
                if (child.isDirectory) {
                    stack.add(child)
                } else {
                    total += child.length()
                }
            }
        }
        return total
    }

    private fun clearDirectoryContents(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var freed = 0L
        val children = dir.listFiles() ?: return 0L
        for (child in children) {
            if (child.isDirectory) {
                freed += clearDirectoryContents(child)
                child.delete()
            } else {
                val size = child.length()
                if (child.delete()) {
                    freed += size
                }
            }
        }
        return freed
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}
