package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AppColorPalette
import com.example.data.AppThemeMode
import com.example.data.CardCornerStyle
import com.example.data.CardDensity
import com.example.data.CardSurfaceStyle
import com.example.data.StyleConfig

data class PulseStyleTokens(
    val cardCornerRadius: Dp = 24.dp,
    val cardShape: Shape = RoundedCornerShape(24.dp),
    val cardPadding: Dp = 18.dp,
    val sectionSpacing: Dp = 16.dp,
    val cardSurfaceAlpha: Float = 0.96f,
    val showCardBorder: Boolean = false
)

val LocalPulseStyle = staticCompositionLocalOf { PulseStyleTokens() }

@Composable
fun pulseCardContainerColor(base: Color = MaterialTheme.colorScheme.surface): Color {
    val style = LocalPulseStyle.current
    return base.copy(alpha = style.cardSurfaceAlpha)
}

@Composable
fun pulseCardBorder(): BorderStroke? {
    val style = LocalPulseStyle.current
    if (!style.showCardBorder) return null
    return BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
}

private fun darkPaletteScheme(palette: AppColorPalette): ColorScheme {
    val primary: Color
    val primaryContainer: Color
    val secondary: Color
    val tertiary: Color
    when (palette) {
        AppColorPalette.COBALT, AppColorPalette.DYNAMIC -> {
            primary = PulseCobaltLight
            primaryContainer = Color(0xFF173666)
            secondary = PulseCyan
            tertiary = PulseEmerald
        }
        AppColorPalette.EMERALD -> {
            primary = Color(0xFF34D399)
            primaryContainer = Color(0xFF064E3B)
            secondary = Color(0xFF2DD4BF)
            tertiary = PulseCobaltLight
        }
        AppColorPalette.AMBER -> {
            primary = Color(0xFFFBBF24)
            primaryContainer = Color(0xFF5C3404)
            secondary = Color(0xFFFB923C)
            tertiary = PulseEmerald
        }
        AppColorPalette.CRIMSON -> {
            primary = Color(0xFFFB7185)
            primaryContainer = Color(0xFF6B1027)
            secondary = Color(0xFFF97316)
            tertiary = Color(0xFF38BDF8)
        }
        AppColorPalette.VIOLET -> {
            primary = Color(0xFFA78BFA)
            primaryContainer = Color(0xFF3B1E7A)
            secondary = Color(0xFF38BDF8)
            tertiary = PulseEmerald
        }
    }
    return darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF06162E),
        primaryContainer = primaryContainer,
        onPrimaryContainer = Color(0xFFEBF2FF),
        secondary = secondary,
        onSecondary = Color(0xFF04262E),
        secondaryContainer = Color(0xFF103E4A),
        onSecondaryContainer = Color(0xFFC8F5FF),
        tertiary = tertiary,
        onTertiary = Color(0xFF04291C),
        tertiaryContainer = Color(0xFF0E3F2D),
        onTertiaryContainer = Color(0xFFC7FCE5),
        background = DarkObsidian,
        onBackground = Color(0xFFEAF0F7),
        surface = DarkSurface,
        onSurface = Color(0xFFEAF0F7),
        surfaceVariant = DarkSurfaceElevated,
        onSurfaceVariant = Color(0xFF9FB0C7),
        outline = DarkOutline,
        error = PulseCoral,
        onError = Color.White
    )
}

private fun lightPaletteScheme(palette: AppColorPalette): ColorScheme {
    val primary: Color
    val primaryContainer: Color
    val secondary: Color
    val tertiary: Color
    when (palette) {
        AppColorPalette.COBALT, AppColorPalette.DYNAMIC -> {
            primary = PulseCobalt
            primaryContainer = Color(0xFFDBE7FF)
            secondary = Color(0xFF0891B2)
            tertiary = Color(0xFF059669)
        }
        AppColorPalette.EMERALD -> {
            primary = Color(0xFF059669)
            primaryContainer = Color(0xFFD1FAE5)
            secondary = Color(0xFF0D9488)
            tertiary = PulseCobalt
        }
        AppColorPalette.AMBER -> {
            primary = Color(0xFFD97706)
            primaryContainer = Color(0xFFFEF3C7)
            secondary = Color(0xFFEA580C)
            tertiary = Color(0xFF059669)
        }
        AppColorPalette.CRIMSON -> {
            primary = Color(0xFFE11D48)
            primaryContainer = Color(0xFFFFE4E6)
            secondary = Color(0xFFEA580C)
            tertiary = Color(0xFF0284C7)
        }
        AppColorPalette.VIOLET -> {
            primary = Color(0xFF7C3AED)
            primaryContainer = Color(0xFFEDE9FE)
            secondary = Color(0xFF0284C7)
            tertiary = Color(0xFF059669)
        }
    }
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryContainer,
        onPrimaryContainer = Color(0xFF0F172A),
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFCFF4FC),
        onSecondaryContainer = Color(0xFF06323D),
        tertiary = tertiary,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD1FAE5),
        onTertiaryContainer = Color(0xFF063827),
        background = LightFrost,
        onBackground = Color(0xFF0F172A),
        surface = LightSurface,
        onSurface = Color(0xFF0F172A),
        surfaceVariant = LightSurfaceElevated,
        onSurfaceVariant = Color(0xFF475569),
        outline = LightOutline,
        error = Color(0xFFE11D48),
        onError = Color.White
    )
}

@Composable
fun PulseCoreTheme(
    styleConfig: StyleConfig = StyleConfig(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (styleConfig.themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val colorScheme = when {
        styleConfig.colorPalette == AppColorPalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDark -> darkPaletteScheme(styleConfig.colorPalette)
        else -> lightPaletteScheme(styleConfig.colorPalette)
    }

    val cornerDp = when (styleConfig.cardCornerStyle) {
        CardCornerStyle.SHARP -> 12.dp
        CardCornerStyle.ROUNDED -> 24.dp
        CardCornerStyle.PILL -> 30.dp
    }
    val paddingDp = when (styleConfig.cardDensity) {
        CardDensity.COMPACT -> 14.dp
        CardDensity.BALANCED -> 18.dp
        CardDensity.SPACIOUS -> 24.dp
    }
    val spacingDp = when (styleConfig.cardDensity) {
        CardDensity.COMPACT -> 10.dp
        CardDensity.BALANCED -> 16.dp
        CardDensity.SPACIOUS -> 22.dp
    }
    val surfaceAlpha = when (styleConfig.cardSurfaceStyle) {
        CardSurfaceStyle.SOLID -> 0.96f
        CardSurfaceStyle.GLASS -> 0.74f
        CardSurfaceStyle.OUTLINED -> 0.56f
    }
    val showBorder = styleConfig.cardSurfaceStyle == CardSurfaceStyle.OUTLINED ||
        styleConfig.cardSurfaceStyle == CardSurfaceStyle.GLASS

    val tokens = PulseStyleTokens(
        cardCornerRadius = cornerDp,
        cardShape = RoundedCornerShape(cornerDp),
        cardPadding = paddingDp,
        sectionSpacing = spacingDp,
        cardSurfaceAlpha = surfaceAlpha,
        showCardBorder = showBorder
    )

    CompositionLocalProvider(LocalPulseStyle provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PulseCoreTypography,
            content = content
        )
    }
}
