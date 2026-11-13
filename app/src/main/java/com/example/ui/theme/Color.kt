package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class AppColorPalette(
    val id: String,
    val labelAr: String,
    val labelEn: String,
    val swatchDark: Color,
    val swatchLight: Color
) {
    AMBER_GOLD(
        id = "AMBER_GOLD",
        labelAr = "العنبر الذهبي",
        labelEn = "Amber Gold",
        swatchDark = Color(0xFFE8A855),
        swatchLight = Color(0xFF8C5308)
    ),
    ROYAL_SAPPHIRE(
        id = "ROYAL_SAPPHIRE",
        labelAr = "الياقوت الأزرق",
        labelEn = "Royal Sapphire",
        swatchDark = Color(0xFF7CC4FF),
        swatchLight = Color(0xFF005FAF)
    ),
    EMERALD_OASIS(
        id = "EMERALD_OASIS",
        labelAr = "الواحة الزمردية",
        labelEn = "Emerald Oasis",
        swatchDark = Color(0xFF56DFA8),
        swatchLight = Color(0xFF006C4C)
    ),
    AMETHYST_VELVET(
        id = "AMETHYST_VELVET",
        labelAr = "البنفسج المخملي",
        labelEn = "Amethyst Velvet",
        swatchDark = Color(0xFFD2BCFF),
        swatchLight = Color(0xFF6750A4)
    ),
    CRIMSON_ROSE(
        id = "CRIMSON_ROSE",
        labelAr = "الوردي القرمزي",
        labelEn = "Crimson Rose",
        swatchDark = Color(0xFFFFB2C1),
        swatchLight = Color(0xFF9C4057)
    ),
    TURQUOISE_LAGOON(
        id = "TURQUOISE_LAGOON",
        labelAr = "الفيروز البحري",
        labelEn = "Turquoise Lagoon",
        swatchDark = Color(0xFF4BE3E8),
        swatchLight = Color(0xFF00686C)
    ),
    SUNSET_CORAL(
        id = "SUNSET_CORAL",
        labelAr = "مرجان الغروب",
        labelEn = "Sunset Coral",
        swatchDark = Color(0xFFFF9E7D),
        swatchLight = Color(0xFF9E3E1E)
    ),
    MIDNIGHT_INDIGO(
        id = "MIDNIGHT_INDIGO",
        labelAr = "النيلي الملكي",
        labelEn = "Midnight Indigo",
        swatchDark = Color(0xFFA6B8FF),
        swatchLight = Color(0xFF36499E)
    ),
    LIME_PULSE(
        id = "LIME_PULSE",
        labelAr = "الليموني المشع",
        labelEn = "Lime Pulse",
        swatchDark = Color(0xFFB6F36B),
        swatchLight = Color(0xFF3E6A00)
    ),
    DYNAMIC_SYSTEM(
        id = "DYNAMIC_SYSTEM",
        labelAr = "ألوان النظام (Material You)",
        labelEn = "System Dynamic",
        swatchDark = Color(0xFF64D8CB),
        swatchLight = Color(0xFF006A62)
    );

    companion object {
        fun fromId(id: String): AppColorPalette =
            entries.firstOrNull { it.id == id } ?: AMBER_GOLD
    }
}

enum class PlayerThemeStyle(
    val id: String,
    val labelAr: String,
    val labelEn: String,
    val descAr: String,
    val descEn: String,
    val previewPrimary: Color,
    val previewSecondary: Color
) {
    AURORA_RING(
        id = "AURORA_RING",
        labelAr = "الحلقة الزجاجية",
        labelEn = "Aurora Ring",
        descAr = "حلقة زجاجية متوهجة مع أقراص جانبية ومؤشر دائري",
        descEn = "Frosted concentric ring with arc progress & side discs",
        previewPrimary = Color(0xFFB67CFF),
        previewSecondary = Color(0xFF69D2FF)
    ),
    STUDIO_COVER(
        id = "STUDIO_COVER",
        labelAr = "غلاف الاستوديو",
        labelEn = "Studio Cover",
        descAr = "غلاف ألبوم سينمائي عريض بظلال عميقة وإضاءة محيطية",
        descEn = "Cinematic wide album frame with deep ambient glow",
        previewPrimary = Color(0xFFFF8A65),
        previewSecondary = Color(0xFFFFD180)
    ),
    VINYL_TURNTABLE(
        id = "VINYL_TURNTABLE",
        labelAr = "الأسطوانة الدوارة",
        labelEn = "Vinyl Turntable",
        descAr = "أسطوانة فينيل كلاسيكية دوارة مع ذراع إبرة استوديو",
        descEn = "Spinning classic LP vinyl platter with studio tonearm",
        previewPrimary = Color(0xFFE8A855),
        previewSecondary = Color(0xFF9C6ADE)
    ),
    WAVEFORM_IMMERSIVE(
        id = "WAVEFORM_IMMERSIVE",
        labelAr = "موجات غامرة",
        labelEn = "Immersive Waveform",
        descAr = "غلاف عائم مع شريط موجات صوتية تفاعلي متزامن",
        descEn = "Floating cover paired with interactive spectrum waveform",
        previewPrimary = Color(0xFF4BE3E8),
        previewSecondary = Color(0xFF56DFA8)
    ),
    MINIMAL_FLOATING(
        id = "MINIMAL_FLOATING",
        labelAr = "الكبسولة العائمة",
        labelEn = "Floating Capsule",
        descAr = "تصميم هادئ متمركز داخل بطاقة كبسولة زجاجية أنيقة",
        descEn = "Clean centered portrait inside a floating frosted card",
        previewPrimary = Color(0xFFFF9EB5),
        previewSecondary = Color(0xFF9FA8DA)
    ),
    RADIAL_SPECTRUM(
        id = "RADIAL_SPECTRUM",
        labelAr = "طيف الهالة المشع",
        labelEn = "Radial Spectrum",
        descAr = "غلاف دائري محاط بنبضات طيف صوتي مشعة متزامنة مع التقدم",
        descEn = "Circular cover ringed by radial equalizer pulses synced to progress",
        previewPrimary = Color(0xFF7CC4FF),
        previewSecondary = Color(0xFFD2BCFF)
    );

    companion object {
        fun fromId(id: String): PlayerThemeStyle =
            entries.firstOrNull { it.id == id } ?: AURORA_RING
    }
}

enum class PlayerVisualEffect(
    val id: String,
    val labelAr: String,
    val labelEn: String
) {
    VINYL_AURA(
        id = "VINYL_AURA",
        labelAr = "الأسطوانة الدوارة والهالة",
        labelEn = "Rotating Vinyl & Aura"
    ),
    SPECTRUM_BARS(
        id = "SPECTRUM_BARS",
        labelAr = "موجات الطيف الموسيقي",
        labelEn = "Spectrum Waves"
    ),
    LIQUID_WAVE(
        id = "LIQUID_WAVE",
        labelAr = "أمواج الأثير المتدفقة",
        labelEn = "Liquid Soundwave"
    );

    companion object {
        fun fromId(id: String): PlayerVisualEffect =
            entries.firstOrNull { it.id == id } ?: VINYL_AURA
    }
}

// Base Dark Surfaces
val NightBackground = Color(0xFF0E1116)
val NightSurface = Color(0xFF141820)
val NightSurfaceContainerLowest = Color(0xFF0A0D12)
val NightSurfaceContainerLow = Color(0xFF181D26)
val NightSurfaceContainer = Color(0xFF1E2530)
val NightSurfaceHigh = Color(0xFF283140)
val NightSurfaceHighest = Color(0xFF333E50)
val NightOutline = Color(0xFF938F86)
val NightOutlineVariant = Color(0xFF474640)

// Base Light Surfaces
val WarmLightBackground = Color(0xFFFCF9F4)
val WarmLightSurface = Color(0xFFF7F2EA)
val WarmLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val WarmLightSurfaceContainerLow = Color(0xFFF4EEE4)
val WarmLightSurfaceContainer = Color(0xFFECE4D7)
val WarmLightSurfaceHigh = Color(0xFFE4DACB)
val WarmLightSurfaceHighest = Color(0xFFDBD0BF)
val WarmLightOutline = Color(0xFF7D756B)
val WarmLightOutlineVariant = Color(0xFFCFC5B8)

// Shared Brand & Quran Isolation Constants
val AmberGoldPrimary = Color(0xFFE8A855)
val AmberGoldOnPrimary = Color(0xFF261700)
val AmberGoldContainer = Color(0xFF4A320C)
val AmberGoldOnContainer = Color(0xFFFFDDAE)

val WarmBronzePrimary = Color(0xFF8C5308)
val WarmBronzeOnPrimary = Color(0xFFFFFFFF)
val WarmBronzeContainer = Color(0xFFFFDDB2)
val WarmBronzeOnContainer = Color(0xFF2C1700)

val EmeraldQuran = Color(0xFF38B28B)
val EmeraldOnQuran = Color(0xFF002116)
val EmeraldQuranContainer = Color(0xFF11382B)
val EmeraldOnQuranContainer = Color(0xFFA7F2D6)

val EmeraldLightQuran = Color(0xFF136B4F)
val EmeraldLightOnQuran = Color(0xFFFFFFFF)
val EmeraldLightContainer = Color(0xFFBFF2DE)
val EmeraldLightOnContainer = Color(0xFF002116)

val TextIvory = Color(0xFFF3EFEA)
val TextMutedSand = Color(0xFFB8B0A4)
val TextDarkCharcoal = Color(0xFF1D1B18)
val TextMutedCharcoal = Color(0xFF534C44)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

fun buildPaletteDarkScheme(palette: AppColorPalette): ColorScheme {
    val (primary, onPrimary, primaryCont, onPrimaryCont, secondary, secondaryCont) = when (palette) {
        AppColorPalette.AMBER_GOLD -> PaletteTuple(
            Color(0xFFE8A855), Color(0xFF261700), Color(0xFF4A320C), Color(0xFFFFDDAE),
            Color(0xFFD98B5F), Color(0xFF532B12)
        )
        AppColorPalette.ROYAL_SAPPHIRE -> PaletteTuple(
            Color(0xFF7CC4FF), Color(0xFF003258), Color(0xFF00497D), Color(0xFFD1E4FF),
            Color(0xFF8BD0F0), Color(0xFF134A63)
        )
        AppColorPalette.EMERALD_OASIS -> PaletteTuple(
            Color(0xFF56DFA8), Color(0xFF003825), Color(0xFF005138), Color(0xFF75FBBF),
            Color(0xFF8CD5B5), Color(0xFF194E39)
        )
        AppColorPalette.AMETHYST_VELVET -> PaletteTuple(
            Color(0xFFD2BCFF), Color(0xFF381E72), Color(0xFF4F378B), Color(0xFFEADDFF),
            Color(0xFFCCC2DC), Color(0xFF4A4458)
        )
        AppColorPalette.CRIMSON_ROSE -> PaletteTuple(
            Color(0xFFFFB2C1), Color(0xFF5E1128), Color(0xFF7D293E), Color(0xFFFFD9DF),
            Color(0xFFE6BDC4), Color(0xFF5C3F45)
        )
        AppColorPalette.TURQUOISE_LAGOON -> PaletteTuple(
            Color(0xFF4BE3E8), Color(0xFF00363A), Color(0xFF004F54), Color(0xFFA6F7FA),
            Color(0xFF80D8FF), Color(0xFF0E4961)
        )
        AppColorPalette.SUNSET_CORAL -> PaletteTuple(
            Color(0xFFFF9E7D), Color(0xFF561E07), Color(0xFF773217), Color(0xFFFFDBCF),
            Color(0xFFFFB786), Color(0xFF5D381E)
        )
        AppColorPalette.MIDNIGHT_INDIGO -> PaletteTuple(
            Color(0xFFA6B8FF), Color(0xFF0E2472), Color(0xFF283C8A), Color(0xFFDCE1FF),
            Color(0xFFC0C6DC), Color(0xFF404659)
        )
        AppColorPalette.LIME_PULSE -> PaletteTuple(
            Color(0xFFB6F36B), Color(0xFF1E3700), Color(0xFF2E4F00), Color(0xFFD2FF96),
            Color(0xFFC2CEAB), Color(0xFF424B32)
        )
        AppColorPalette.DYNAMIC_SYSTEM -> PaletteTuple(
            Color(0xFF64D8CB), Color(0xFF003732), Color(0xFF005049), Color(0xFF84F5E7),
            Color(0xFFB1CCC8), Color(0xFF324B48)
        )
    }

    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryCont,
        onPrimaryContainer = onPrimaryCont,
        secondary = secondary,
        onSecondary = onPrimary,
        secondaryContainer = secondaryCont,
        onSecondaryContainer = onPrimaryCont,
        tertiary = EmeraldQuran,
        onTertiary = EmeraldOnQuran,
        tertiaryContainer = EmeraldQuranContainer,
        onTertiaryContainer = EmeraldOnQuranContainer,
        background = NightBackground,
        onBackground = TextIvory,
        surface = NightSurface,
        onSurface = TextIvory,
        surfaceVariant = NightSurfaceContainer,
        onSurfaceVariant = TextMutedSand,
        surfaceContainerLowest = NightSurfaceContainerLowest,
        surfaceContainerLow = NightSurfaceContainerLow,
        surfaceContainer = NightSurfaceContainer,
        surfaceContainerHigh = NightSurfaceHigh,
        surfaceContainerHighest = NightSurfaceHighest,
        outline = NightOutline,
        outlineVariant = NightOutlineVariant,
        inverseSurface = WarmLightSurface,
        inverseOnSurface = TextDarkCharcoal,
        inversePrimary = palette.swatchLight,
        error = ErrorDark,
        onError = OnErrorDark,
        errorContainer = ErrorContainerDark,
        onErrorContainer = OnErrorContainerDark
    )
}

fun buildPaletteLightScheme(palette: AppColorPalette): ColorScheme {
    val (primary, onPrimary, primaryCont, onPrimaryCont, secondary, secondaryCont) = when (palette) {
        AppColorPalette.AMBER_GOLD -> PaletteTuple(
            Color(0xFF8C5308), Color.White, Color(0xFFFFDDB2), Color(0xFF2C1700),
            Color(0xFF844B27), Color(0xFFFFDBCA)
        )
        AppColorPalette.ROYAL_SAPPHIRE -> PaletteTuple(
            Color(0xFF005FAF), Color.White, Color(0xFFD4E6FF), Color(0xFF001C3A),
            Color(0xFF2A638A), Color(0xFFCDE5FF)
        )
        AppColorPalette.EMERALD_OASIS -> PaletteTuple(
            Color(0xFF006C4C), Color.White, Color(0xFF89F8C7), Color(0xFF002114),
            Color(0xFF376752), Color(0xFFBAF0D6)
        )
        AppColorPalette.AMETHYST_VELVET -> PaletteTuple(
            Color(0xFF6750A4), Color.White, Color(0xFFEADDFF), Color(0xFF21005D),
            Color(0xFF625B71), Color(0xFFE8DEF8)
        )
        AppColorPalette.CRIMSON_ROSE -> PaletteTuple(
            Color(0xFF9C4057), Color.White, Color(0xFFFFD9DF), Color(0xFF3F0016),
            Color(0xFF75565C), Color(0xFFFFD9DF)
        )
        AppColorPalette.TURQUOISE_LAGOON -> PaletteTuple(
            Color(0xFF00686C), Color.White, Color(0xFF9CF1F5), Color(0xFF002022),
            Color(0xFF4A6365), Color(0xFFCCE8EA)
        )
        AppColorPalette.SUNSET_CORAL -> PaletteTuple(
            Color(0xFF9E3E1E), Color.White, Color(0xFFFFDBCF), Color(0xFF380D00),
            Color(0xFF77574C), Color(0xFFFFDBCF)
        )
        AppColorPalette.MIDNIGHT_INDIGO -> PaletteTuple(
            Color(0xFF36499E), Color.White, Color(0xFFDCE1FF), Color(0xFF001552),
            Color(0xFF585E72), Color(0xFFDCE2F9)
        )
        AppColorPalette.LIME_PULSE -> PaletteTuple(
            Color(0xFF3E6A00), Color.White, Color(0xFFBDF873), Color(0xFF0F2000),
            Color(0xFF576249), Color(0xFFDBE7C8)
        )
        AppColorPalette.DYNAMIC_SYSTEM -> PaletteTuple(
            Color(0xFF006A62), Color.White, Color(0xFF88F5E8), Color(0xFF00201D),
            Color(0xFF4A635F), Color(0xFFCCE8E3)
        )
    }

    val lightBg = when (palette) {
        AppColorPalette.ROYAL_SAPPHIRE -> Color(0xFFF6FAFE)
        AppColorPalette.EMERALD_OASIS -> Color(0xFFF4FBF7)
        AppColorPalette.AMETHYST_VELVET -> Color(0xFFFAF7FF)
        AppColorPalette.CRIMSON_ROSE -> Color(0xFFFFF8F8)
        AppColorPalette.TURQUOISE_LAGOON -> Color(0xFFF3FBFD)
        AppColorPalette.SUNSET_CORAL -> Color(0xFFFFF8F6)
        AppColorPalette.MIDNIGHT_INDIGO -> Color(0xFFF7F8FF)
        AppColorPalette.LIME_PULSE -> Color(0xFFF7FCF2)
        else -> WarmLightBackground
    }
    val lightSurface = when (palette) {
        AppColorPalette.ROYAL_SAPPHIRE -> Color(0xFFEEF4FA)
        AppColorPalette.EMERALD_OASIS -> Color(0xFFECF6F0)
        AppColorPalette.AMETHYST_VELVET -> Color(0xFFF3EEFA)
        AppColorPalette.CRIMSON_ROSE -> Color(0xFFFAEEF0)
        AppColorPalette.TURQUOISE_LAGOON -> Color(0xFFE9F6F8)
        AppColorPalette.SUNSET_CORAL -> Color(0xFFFAEEE9)
        AppColorPalette.MIDNIGHT_INDIGO -> Color(0xFFEEF0FB)
        AppColorPalette.LIME_PULSE -> Color(0xFFEDF6E4)
        else -> WarmLightSurface
    }
    val lightContainer = when (palette) {
        AppColorPalette.ROYAL_SAPPHIRE -> Color(0xFFE1ECF6)
        AppColorPalette.EMERALD_OASIS -> Color(0xFFDDEEE5)
        AppColorPalette.AMETHYST_VELVET -> Color(0xFFE8E0F4)
        AppColorPalette.CRIMSON_ROSE -> Color(0xFFF3E0E4)
        AppColorPalette.TURQUOISE_LAGOON -> Color(0xFFD9EEF2)
        AppColorPalette.SUNSET_CORAL -> Color(0xFFF4E1D9)
        AppColorPalette.MIDNIGHT_INDIGO -> Color(0xFFE1E5F7)
        AppColorPalette.LIME_PULSE -> Color(0xFFDFEDD1)
        else -> WarmLightSurfaceContainer
    }

    return lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryCont,
        onPrimaryContainer = onPrimaryCont,
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondaryCont,
        onSecondaryContainer = onPrimaryCont,
        tertiary = EmeraldLightQuran,
        onTertiary = EmeraldLightOnQuran,
        tertiaryContainer = EmeraldLightContainer,
        onTertiaryContainer = EmeraldLightOnContainer,
        background = lightBg,
        onBackground = TextDarkCharcoal,
        surface = lightSurface,
        onSurface = TextDarkCharcoal,
        surfaceVariant = lightContainer,
        onSurfaceVariant = TextMutedCharcoal,
        surfaceContainerLowest = WarmLightSurfaceContainerLowest,
        surfaceContainerLow = lightSurface,
        surfaceContainer = lightContainer,
        surfaceContainerHigh = lightContainer,
        surfaceContainerHighest = WarmLightSurfaceHighest,
        outline = WarmLightOutline,
        outlineVariant = WarmLightOutlineVariant,
        inverseSurface = NightSurface,
        inverseOnSurface = TextIvory,
        inversePrimary = palette.swatchDark,
        error = ErrorLight,
        onError = OnErrorLight,
        errorContainer = ErrorContainerLight,
        onErrorContainer = OnErrorContainerLight
    )
}

private data class PaletteTuple(
    val primary: Color,
    val onPrimary: Color,
    val primaryCont: Color,
    val onPrimaryCont: Color,
    val secondary: Color,
    val secondaryCont: Color
)
