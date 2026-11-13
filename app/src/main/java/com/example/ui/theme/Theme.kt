package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.ui.localization.AppStrings
import com.example.ui.localization.ArabicStrings
import com.example.ui.localization.EnglishStrings
import com.example.ui.localization.LocalAppStrings

@Immutable
data class NaghamExtendedColors(
    val isDark: Boolean,
    val quranAccent: Color,
    val onQuranAccent: Color,
    val quranContainer: Color,
    val onQuranContainer: Color,
    val cardBorder: Color,
    val subtleGlow: Color,
    val heroOverlayStart: Color,
    val heroOverlayEnd: Color,
    val onHeroOverlay: Color,
    val defaultTrackAccent: Color
)

val LocalNaghamColors = staticCompositionLocalOf {
    NaghamExtendedColors(
        isDark = true,
        quranAccent = EmeraldQuran,
        onQuranAccent = EmeraldOnQuran,
        quranContainer = EmeraldQuranContainer,
        onQuranContainer = EmeraldOnQuranContainer,
        cardBorder = Color(0x26FFFFFF),
        subtleGlow = Color(0x33E8A855),
        heroOverlayStart = Color(0xDD0E1116),
        heroOverlayEnd = Color(0x880E1116),
        onHeroOverlay = TextIvory,
        defaultTrackAccent = AmberGoldPrimary
    )
}

val NaghamDarkColorScheme: ColorScheme = buildPaletteDarkScheme(AppColorPalette.AMBER_GOLD)
val NaghamLightColorScheme: ColorScheme = buildPaletteLightScheme(AppColorPalette.AMBER_GOLD)

val NaghamShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun NaghamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    eyeComfortMode: Boolean = false,
    colorPalette: AppColorPalette = AppColorPalette.AMBER_GOLD,
    designStyle: AppDesignStyle = AppDesignStyle.MATERIAL,
    dynamicColor: Boolean = false,
    languageCode: String = "ar",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isArabic = languageCode == "ar"
    val useDynamic = dynamicColor || colorPalette == AppColorPalette.DYNAMIC_SYSTEM

    val targetColorScheme = remember(darkTheme, colorPalette, designStyle, useDynamic, context) {
        val baseScheme = when {
            useDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> buildPaletteDarkScheme(colorPalette)
            else -> buildPaletteLightScheme(colorPalette)
        }
        applyDesignStyleToColorScheme(
            base = baseScheme,
            style = designStyle,
            darkTheme = darkTheme
        )
    }

    val designTokens = remember(designStyle, targetColorScheme.primary, darkTheme) {
        buildDesignTokens(
            style = designStyle,
            primary = targetColorScheme.primary,
            isDark = darkTheme
        )
    }

    val activeShapes = remember(designStyle) {
        buildDesignShapes(designStyle)
    }

    val typography = remember(isArabic) {
        buildAppTypography(isArabic)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            try {
                val activity = view.context as? Activity
                activity?.window?.let { window ->
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = !darkTheme
                    insetsController.isAppearanceLightNavigationBars = !darkTheme
                }
            } catch (_: Throwable) {
            }
        }
    }

    val extendedColors = remember(darkTheme, designStyle, targetColorScheme.primary) {
        val dynamicCardBorder = when (designStyle) {
            AppDesignStyle.GLASS -> Color.White.copy(alpha = if (darkTheme) 0.36f else 0.72f)
            AppDesignStyle.OBSIDIAN_AMOLED -> targetColorScheme.primary.copy(alpha = 0.42f)
            AppDesignStyle.VELVET_SUNSET -> Color(0xFFFF8FA3).copy(alpha = if (darkTheme) 0.45f else 0.38f)
            AppDesignStyle.CRYSTAL_MINIMAL -> if (darkTheme) Color.White.copy(alpha = 0.22f) else Color(0xFF90CAF9).copy(alpha = 0.55f)
            AppDesignStyle.NEON_CYBER -> Color(0xFF00F0FF).copy(alpha = if (darkTheme) 0.58f else 0.45f)
            AppDesignStyle.ROYAL_ARABESQUE -> Color(0xFFF3C669).copy(alpha = 0.55f)
            AppDesignStyle.NEUMORPHIC_STUDIO -> if (darkTheme) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.78f)
            AppDesignStyle.MATERIAL -> targetColorScheme.primary.copy(alpha = if (darkTheme) 0.22f else 0.20f)
        }
        if (darkTheme) {
            NaghamExtendedColors(
                isDark = true,
                quranAccent = EmeraldQuran,
                onQuranAccent = EmeraldOnQuran,
                quranContainer = EmeraldQuranContainer,
                onQuranContainer = EmeraldOnQuranContainer,
                cardBorder = dynamicCardBorder,
                subtleGlow = targetColorScheme.primary.copy(alpha = 0.20f),
                heroOverlayStart = Color(0xDD0E1116),
                heroOverlayEnd = Color(0x880E1116),
                onHeroOverlay = TextIvory,
                defaultTrackAccent = targetColorScheme.primary
            )
        } else {
            NaghamExtendedColors(
                isDark = false,
                quranAccent = EmeraldLightQuran,
                onQuranAccent = EmeraldLightOnQuran,
                quranContainer = EmeraldLightContainer,
                onQuranContainer = EmeraldLightOnContainer,
                cardBorder = dynamicCardBorder,
                subtleGlow = targetColorScheme.primary.copy(alpha = 0.14f),
                heroOverlayStart = Color(0xCC1E1812),
                heroOverlayEnd = Color(0x661E1812),
                onHeroOverlay = TextIvory,
                defaultTrackAccent = targetColorScheme.primary
            )
        }
    }

    val strings: AppStrings = if (isArabic) ArabicStrings else EnglishStrings
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalNaghamColors provides extendedColors,
        LocalNaghamDesign provides designTokens,
        LocalAppStrings provides strings,
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = targetColorScheme,
            typography = typography,
            shapes = activeShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    NaghamTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
