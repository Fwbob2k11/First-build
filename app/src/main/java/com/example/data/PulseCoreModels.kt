package com.example.data

enum class AppTab(val label: String) {
    BOOST("Boost"),
    MEMORY("Memory"),
    STORAGE("Storage"),
    BOB("Bob AI")
}

enum class AppThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light")
}

enum class AppColorPalette(val label: String, val previewHex: Long) {
    DYNAMIC("Material You", 0xFF3B82F6),
    COBALT("Cobalt", 0xFF2563EB),
    EMERALD("Emerald", 0xFF10B981),
    AMBER("Amber", 0xFFF59E0B),
    CRIMSON("Crimson", 0xFFE11D48),
    VIOLET("Violet", 0xFF7C3AED)
}

enum class CardDensity(val label: String) {
    COMPACT("Compact"),
    BALANCED("Balanced"),
    SPACIOUS("Spacious")
}

enum class CardCornerStyle(val label: String) {
    SHARP("Crisp"),
    ROUNDED("Rounded"),
    PILL("Soft")
}

enum class CardSurfaceStyle(val label: String) {
    SOLID("Solid"),
    GLASS("Frosted"),
    OUTLINED("Outlined")
}

enum class AppNavStyle(val label: String) {
    AUTO("Auto"),
    BOTTOM_BAR("Bottom Bar"),
    FLOATING_PILL("Floating Pill"),
    SIDE_RAIL("Side Rail")
}

enum class BoostSectionId(val label: String) {
    ENGINE("Gaming Performance Engine"),
    THERMAL_SLIDER("Thermal Target Slider"),
    AI_OVERVIEW("Gemini Thermal AI Overview")
}

enum class BackgroundType(val label: String) {
    ADAPTIVE("Adaptive"),
    GRADIENT("Gradient"),
    CUSTOM_PHOTO("Photo")
}

enum class GradientPreset(
    val label: String,
    val startHex: Long,
    val midHex: Long,
    val endHex: Long
) {
    MIDNIGHT_PULSE("Midnight Pulse", 0xFF0B132B, 0xFF1C2541, 0xFF1E3A8A),
    CYBER_AURORA("Cyber Aurora", 0xFF091E2C, 0xFF0F4C5C, 0xFF312E81),
    SUNSET_EMBER("Sunset Ember", 0xFF2A0E1E, 0xFF6B1D2F, 0xFF9A3412),
    EMERALD_MATRIX("Emerald Matrix", 0xFF051914, 0xFF0B3B2E, 0xFF0F766E)
}

data class BackgroundConfig(
    val backgroundType: BackgroundType = BackgroundType.ADAPTIVE,
    val gradientPreset: GradientPreset = GradientPreset.MIDNIGHT_PULSE,
    val customPhotoPath: String? = null,
    val photoDimAlpha: Float = 0.48f,
    val photoBlurDp: Float = 0f,
    val photoRevision: Long = 0L
)

data class StyleConfig(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val colorPalette: AppColorPalette = AppColorPalette.DYNAMIC,
    val cardDensity: CardDensity = CardDensity.BALANCED,
    val cardCornerStyle: CardCornerStyle = CardCornerStyle.ROUNDED,
    val cardSurfaceStyle: CardSurfaceStyle = CardSurfaceStyle.SOLID,
    val navStyle: AppNavStyle = AppNavStyle.AUTO,
    val wideGridEnabled: Boolean = true,
    val boostCardOrder: List<BoostSectionId> = listOf(
        BoostSectionId.ENGINE,
        BoostSectionId.THERMAL_SLIDER,
        BoostSectionId.AI_OVERVIEW
    ),
    val hiddenBoostSections: Set<BoostSectionId> = emptySet(),
    val backgroundConfig: BackgroundConfig = BackgroundConfig()
)

data class DeviceProfile(
    val manufacturer: String,
    val brandDisplay: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val socOrHardware: String,
    val isTablet: Boolean,
    val cpuCores: Int,
    val brandTagline: String
)

enum class ThermalSeverity {
    COOL,
    BALANCED,
    WARM,
    THROTTLING
}

data class LiveTelemetry(
    val batteryTempC: Float = 32.0f,
    val thermalStatusLabel: String = "Nominal",
    val thermalSeverity: ThermalSeverity = ThermalSeverity.COOL,
    val thermalHeadroomRatio: Float = 0.25f,
    val ramUsedMb: Long = 0L,
    val ramTotalMb: Long = 1L,
    val ramAvailMb: Long = 0L,
    val ramThresholdMb: Long = 0L,
    val isLowMemory: Boolean = false,
    val appPssMb: Float = 0f,
    val storageUsedBytes: Long = 0L,
    val storageTotalBytes: Long = 1L,
    val storageFreeBytes: Long = 0L,
    val appCacheBytes: Long = 0L,
    val measuredFps: Float = 60.0f,
    val frameTimeMs: Float = 16.6f,
    val fpsHistory: List<Float> = emptyList(),
    val gameModeLabel: String = "Standard",
    val batteryLevelPct: Int = 100,
    val isCharging: Boolean = false,
    val processNote: String = ""
) {
    val ramUsageFraction: Float
        get() = if (ramTotalMb > 0L) (ramUsedMb.toFloat() / ramTotalMb.toFloat()).coerceIn(0f, 1f) else 0f

    val storageUsageFraction: Float
        get() = if (storageTotalBytes > 0L) (storageUsedBytes.toFloat() / storageTotalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class BoostReport(
    val timestampMs: Long,
    val freedCacheBytes: Long,
    val ramAvailBeforeMb: Long,
    val ramAvailAfterMb: Long,
    val thermalHeadroomLabel: String,
    val gameModeStatus: String,
    val autoFrameGuardActive: Boolean,
    val summary: String
)

sealed interface ThermalAiState {
    data object Idle : ThermalAiState
    data object Loading : ThermalAiState
    data class Streaming(val text: String, val targetTempC: Float) : ThermalAiState
    data class Success(val text: String, val targetTempC: Float) : ThermalAiState
    data class Error(val message: String) : ThermalAiState
}

enum class StorageCategory(val label: String) {
    ALL("All Files"),
    LARGE("Large (>25 MB)"),
    MEDIA("Media"),
    ARCHIVES_TEMP("Archives & Temp")
}

data class ScannedFolder(
    val uriString: String,
    val displayName: String,
    val itemCount: Int,
    val totalBytes: Long
)

data class ScannedFileItem(
    val uriString: String,
    val folderUriString: String,
    val name: String,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
    val mimeType: String,
    val category: StorageCategory
)

sealed interface StorageScanState {
    data object Idle : StorageScanState
    data class Scanning(val folderName: String, val scannedCount: Int) : StorageScanState
    data object Ready : StorageScanState
    data class Error(val message: String) : StorageScanState
}

enum class BobShellStyle(val label: String) {
    CORE_BOT("Core Bot"),
    TACTICAL_MECH("Tactical Mech"),
    SLEEK_VISOR("Sleek Visor"),
    FRIENDLY_ORB("Friendly Orb")
}

enum class BobAccentColor(val label: String, val colorHex: Long) {
    COBALT("Electric Cobalt", 0xFF2563EB),
    EMERALD("Signal Emerald", 0xFF10B981),
    AMBER("Thermal Amber", 0xFFF59E0B),
    CORAL("Turbo Coral", 0xFFF43F5E),
    VIOLET("Pulse Violet", 0xFF8B5CF6),
    CYAN("Cryo Cyan", 0xFF06B6D4)
}

enum class BobEyeStyle(val label: String) {
    FRIENDLY_GLOW("Friendly Glow"),
    CYBER_VISOR("Cyber Visor"),
    PIXEL_WINK("Pixel Wink"),
    CHILL_OPTICS("Chill Optics")
}

enum class BobAccessory(val label: String) {
    NONE("Clean Chassis"),
    COOLING_HALO("Cooling Halo"),
    GAMING_HEADSET("Gaming Headset"),
    ANTENNA_BEACON("Signal Antenna"),
    TECH_GLASSES("HUD Glasses")
}

data class BobAppearance(
    val shellStyle: BobShellStyle = BobShellStyle.CORE_BOT,
    val accentColor: BobAccentColor = BobAccentColor.COBALT,
    val eyeStyle: BobEyeStyle = BobEyeStyle.FRIENDLY_GLOW,
    val accessory: BobAccessory = BobAccessory.COOLING_HALO
)

enum class BobQuickAction(val label: String) {
    RUN_BOOST("Run Gaming Boost"),
    OPEN_THERMAL("Tune Temp Slider"),
    OPEN_MEMORY("Open Memory Manager"),
    OPEN_STORAGE("Open Storage Cleaner"),
    OPEN_SYSTEM_APPS("System App Settings")
}

data class BobMessage(
    val id: String,
    val isFromUser: Boolean,
    val text: String,
    val timestampMs: Long,
    val isStreaming: Boolean = false,
    val suggestedAction: BobQuickAction? = null
)

data class BobChatState(
    val messages: List<BobMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val errorMessage: String? = null
)
