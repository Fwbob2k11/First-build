package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pulseCoreDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pulsecore_prefs"
)

data class PulseCorePreferences(
    val targetTempC: Float = 39.0f,
    val autoFrameGuard: Boolean = true,
    val useFahrenheit: Boolean = false,
    val authorizedFolderUris: Set<String> = emptySet(),
    val bobAppearance: BobAppearance = BobAppearance(),
    val styleConfig: StyleConfig = StyleConfig()
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val TARGET_TEMP_C = floatPreferencesKey("target_temp_c")
        val AUTO_FRAME_GUARD = booleanPreferencesKey("auto_frame_guard")
        val USE_FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")
        val AUTHORIZED_FOLDERS = stringSetPreferencesKey("authorized_folder_uris")
        val BOB_SHELL = stringPreferencesKey("bob_shell_style")
        val BOB_ACCENT = stringPreferencesKey("bob_accent_color")
        val BOB_EYES = stringPreferencesKey("bob_eye_style")
        val BOB_ACCESSORY = stringPreferencesKey("bob_accessory")

        val THEME_MODE = stringPreferencesKey("style_theme_mode")
        val COLOR_PALETTE = stringPreferencesKey("style_color_palette")
        val CARD_DENSITY = stringPreferencesKey("style_card_density")
        val CARD_CORNER = stringPreferencesKey("style_card_corner")
        val CARD_SURFACE = stringPreferencesKey("style_card_surface")
        val NAV_STYLE = stringPreferencesKey("style_nav_style")
        val WIDE_GRID = booleanPreferencesKey("style_wide_grid")
        val BOOST_CARD_ORDER = stringPreferencesKey("style_boost_card_order")
        val HIDDEN_BOOST_SECTIONS = stringSetPreferencesKey("style_hidden_boost_sections")

        val BG_TYPE = stringPreferencesKey("bg_type")
        val BG_GRADIENT = stringPreferencesKey("bg_gradient_preset")
        val BG_PHOTO_PATH = stringPreferencesKey("bg_photo_path")
        val BG_PHOTO_DIM = floatPreferencesKey("bg_photo_dim")
        val BG_PHOTO_BLUR = floatPreferencesKey("bg_photo_blur")
        val BG_PHOTO_REV = longPreferencesKey("bg_photo_rev")
    }

    val preferencesFlow: Flow<PulseCorePreferences> = context.pulseCoreDataStore.data.map { prefs ->
        val shell = parseEnum(prefs[Keys.BOB_SHELL], BobShellStyle.CORE_BOT)
        val accent = parseEnum(prefs[Keys.BOB_ACCENT], BobAccentColor.COBALT)
        val eyes = parseEnum(prefs[Keys.BOB_EYES], BobEyeStyle.FRIENDLY_GLOW)
        val accessory = parseEnum(prefs[Keys.BOB_ACCESSORY], BobAccessory.COOLING_HALO)

        val defaultOrder = listOf(
            BoostSectionId.ENGINE,
            BoostSectionId.THERMAL_SLIDER,
            BoostSectionId.AI_OVERVIEW
        )
        val parsedOrder = prefs[Keys.BOOST_CARD_ORDER]
            ?.split(",")
            ?.mapNotNull { name -> runCatching { BoostSectionId.valueOf(name) }.getOrNull() }
            ?.distinct()
            ?.takeIf { it.size == defaultOrder.size }
            ?: defaultOrder

        val hiddenSet = (prefs[Keys.HIDDEN_BOOST_SECTIONS] ?: emptySet())
            .mapNotNull { runCatching { BoostSectionId.valueOf(it) }.getOrNull() }
            .toSet()

        val styleConfig = StyleConfig(
            themeMode = parseEnum(prefs[Keys.THEME_MODE], AppThemeMode.SYSTEM),
            colorPalette = parseEnum(prefs[Keys.COLOR_PALETTE], AppColorPalette.DYNAMIC),
            cardDensity = parseEnum(prefs[Keys.CARD_DENSITY], CardDensity.BALANCED),
            cardCornerStyle = parseEnum(prefs[Keys.CARD_CORNER], CardCornerStyle.ROUNDED),
            cardSurfaceStyle = parseEnum(prefs[Keys.CARD_SURFACE], CardSurfaceStyle.SOLID),
            navStyle = parseEnum(prefs[Keys.NAV_STYLE], AppNavStyle.AUTO),
            wideGridEnabled = prefs[Keys.WIDE_GRID] ?: true,
            boostCardOrder = parsedOrder,
            hiddenBoostSections = hiddenSet,
            backgroundConfig = BackgroundConfig(
                backgroundType = parseEnum(prefs[Keys.BG_TYPE], BackgroundType.ADAPTIVE),
                gradientPreset = parseEnum(prefs[Keys.BG_GRADIENT], GradientPreset.MIDNIGHT_PULSE),
                customPhotoPath = prefs[Keys.BG_PHOTO_PATH],
                photoDimAlpha = (prefs[Keys.BG_PHOTO_DIM] ?: 0.48f).coerceIn(0.10f, 0.85f),
                photoBlurDp = (prefs[Keys.BG_PHOTO_BLUR] ?: 0f).coerceIn(0f, 24f),
                photoRevision = prefs[Keys.BG_PHOTO_REV] ?: 0L
            )
        )

        PulseCorePreferences(
            targetTempC = (prefs[Keys.TARGET_TEMP_C] ?: 39.0f).coerceIn(34.0f, 45.0f),
            autoFrameGuard = prefs[Keys.AUTO_FRAME_GUARD] ?: true,
            useFahrenheit = prefs[Keys.USE_FAHRENHEIT] ?: false,
            authorizedFolderUris = prefs[Keys.AUTHORIZED_FOLDERS] ?: emptySet(),
            bobAppearance = BobAppearance(
                shellStyle = shell,
                accentColor = accent,
                eyeStyle = eyes,
                accessory = accessory
            ),
            styleConfig = styleConfig
        )
    }

    suspend fun setTargetTempC(tempC: Float) {
        context.pulseCoreDataStore.edit { it[Keys.TARGET_TEMP_C] = tempC.coerceIn(34.0f, 45.0f) }
    }

    suspend fun setAutoFrameGuard(enabled: Boolean) {
        context.pulseCoreDataStore.edit { it[Keys.AUTO_FRAME_GUARD] = enabled }
    }

    suspend fun setUseFahrenheit(useFahrenheit: Boolean) {
        context.pulseCoreDataStore.edit { it[Keys.USE_FAHRENHEIT] = useFahrenheit }
    }

    suspend fun addAuthorizedFolderUri(uriString: String) {
        context.pulseCoreDataStore.edit { prefs ->
            val current = prefs[Keys.AUTHORIZED_FOLDERS] ?: emptySet()
            prefs[Keys.AUTHORIZED_FOLDERS] = current + uriString
        }
    }

    suspend fun removeAuthorizedFolderUri(uriString: String) {
        context.pulseCoreDataStore.edit { prefs ->
            val current = prefs[Keys.AUTHORIZED_FOLDERS] ?: emptySet()
            prefs[Keys.AUTHORIZED_FOLDERS] = current - uriString
        }
    }

    suspend fun saveBobAppearance(appearance: BobAppearance) {
        context.pulseCoreDataStore.edit { prefs ->
            prefs[Keys.BOB_SHELL] = appearance.shellStyle.name
            prefs[Keys.BOB_ACCENT] = appearance.accentColor.name
            prefs[Keys.BOB_EYES] = appearance.eyeStyle.name
            prefs[Keys.BOB_ACCESSORY] = appearance.accessory.name
        }
    }

    suspend fun saveStyleConfig(config: StyleConfig) {
        context.pulseCoreDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = config.themeMode.name
            prefs[Keys.COLOR_PALETTE] = config.colorPalette.name
            prefs[Keys.CARD_DENSITY] = config.cardDensity.name
            prefs[Keys.CARD_CORNER] = config.cardCornerStyle.name
            prefs[Keys.CARD_SURFACE] = config.cardSurfaceStyle.name
            prefs[Keys.NAV_STYLE] = config.navStyle.name
            prefs[Keys.WIDE_GRID] = config.wideGridEnabled
            prefs[Keys.BOOST_CARD_ORDER] = config.boostCardOrder.joinToString(",") { it.name }
            prefs[Keys.HIDDEN_BOOST_SECTIONS] = config.hiddenBoostSections.map { it.name }.toSet()

            val bg = config.backgroundConfig
            prefs[Keys.BG_TYPE] = bg.backgroundType.name
            prefs[Keys.BG_GRADIENT] = bg.gradientPreset.name
            if (bg.customPhotoPath != null) {
                prefs[Keys.BG_PHOTO_PATH] = bg.customPhotoPath
            } else {
                prefs.remove(Keys.BG_PHOTO_PATH)
            }
            prefs[Keys.BG_PHOTO_DIM] = bg.photoDimAlpha
            prefs[Keys.BG_PHOTO_BLUR] = bg.photoBlurDp
            prefs[Keys.BG_PHOTO_REV] = bg.photoRevision
        }
    }

    private inline fun <reified T : Enum<T>> parseEnum(raw: String?, default: T): T {
        return raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
    }
}
