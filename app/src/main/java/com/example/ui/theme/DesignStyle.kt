package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class AppDesignStyle(
    val id: String,
    val labelAr: String,
    val labelEn: String,
    val descAr: String,
    val descEn: String,
    val badgeColor: Color,
    val secondaryBadgeColor: Color
) {
    GLASS(
        id = "GLASS",
        labelAr = "زجاجي هالة بنفسجية",
        labelEn = "Frosted Aurora Glass",
        descAr = "تصميم زجاجي ضبابي داكن مع إشراقة علوية ناعمة وشريط كبسولة عائم",
        descEn = "Obsidian frosted glass surfaces over a soft top violet-aurora glow & floating capsule bar",
        badgeColor = Color(0xFFA864F7),
        secondaryBadgeColor = Color(0xFF72E4FF)
    ),
    MATERIAL(
        id = "MATERIAL",
        labelAr = "متريال عصري",
        labelEn = "Material 3",
        descAr = "تصميم متريال الأصيل بأسطح متدرجة نظيفة وحواف متناسقة",
        descEn = "Clean Material You tonal surfaces with balanced organic curves",
        badgeColor = Color(0xFFE8A855),
        secondaryBadgeColor = Color(0xFF38B28B)
    ),
    OBSIDIAN_AMOLED(
        id = "OBSIDIAN_AMOLED",
        labelAr = "أموليد فاحم",
        labelEn = "Obsidian AMOLED",
        descAr = "خلفية سوداء نقية عالية التباين موفرة للطاقة مع حواف ضوئية دقيقة",
        descEn = "Pure pitch-black AMOLED surfaces with razor-sharp contrast & luminous trims",
        badgeColor = Color(0xFF7C4DFF),
        secondaryBadgeColor = Color(0xFF00E5FF)
    ),
    VELVET_SUNSET(
        id = "VELVET_SUNSET",
        labelAr = "غروب مخملي",
        labelEn = "Velvet Sunset",
        descAr = "تدرجات شفق دافئة تمزج الأرجواني والمرجاني مع بطاقات مخملية عائمة",
        descEn = "Warm twilight gradient blending plum & coral dusk over plush floating cards",
        badgeColor = Color(0xFFFF758C),
        secondaryBadgeColor = Color(0xFFFFB199)
    ),
    CRYSTAL_MINIMAL(
        id = "CRYSTAL_MINIMAL",
        labelAr = "كريستال نقي",
        labelEn = "Crystal Minimal",
        descAr = "تصميم هادئ خفيف بحواف كريستالية رفيعة وتنسيق انسيابي مريح",
        descEn = "Airy Nordic minimal geometry with crisp hairline crystal borders",
        badgeColor = Color(0xFF64B5F6),
        secondaryBadgeColor = Color(0xFF80DEEA)
    ),
    NEON_CYBER(
        id = "NEON_CYBER",
        labelAr = "نيون مستقبلي",
        labelEn = "Cyber Neon",
        descAr = "استوديو مستقبلي بزوايا هندسية حادة وإطارات نيون مشعة",
        descEn = "Futuristic synth studio with chamfered geometric edges & neon rims",
        badgeColor = Color(0xFF00F0FF),
        secondaryBadgeColor = Color(0xFFFF2E93)
    ),
    ROYAL_ARABESQUE(
        id = "ROYAL_ARABESQUE",
        labelAr = "فخامة شرقية",
        labelEn = "Royal Arabesque",
        descAr = "طابع شرقي ملكي بزخارف نجمية هادئة وإطارات مذهبة وحواف مقوسة",
        descEn = "Regal Middle Eastern salon with subtle star filigree & gilded arches",
        badgeColor = Color(0xFFF3C669),
        secondaryBadgeColor = Color(0xFF9E5A24)
    ),
    NEUMORPHIC_STUDIO(
        id = "NEUMORPHIC_STUDIO",
        labelAr = "نيومورفيك بارز",
        labelEn = "Soft Neumorphic",
        descAr = "تصميم مجسّم ناعم يحاكي أجهزة الصوت الفاخرة بظلال وإضاءة ثلاثية الأبعاد",
        descEn = "Tactile 3D studio cushion aesthetic with sculpted light & shadow bevels",
        badgeColor = Color(0xFF8AA4C8),
        secondaryBadgeColor = Color(0xFF4C6382)
    );

    companion object {
        fun fromId(id: String): AppDesignStyle =
            entries.firstOrNull { it.id == id } ?: MATERIAL
    }
}

@Immutable
data class NaghamDesignTokens(
    val style: AppDesignStyle,
    val isDark: Boolean,
    val cardShape: Shape,
    val heroShape: Shape,
    val pillShape: Shape,
    val navBarShape: Shape,
    val isFloatingNavBar: Boolean,
    val cardAlpha: Float,
    val highlightedCardAlpha: Float,
    val navBarAlpha: Float,
    val borderWidth: Dp,
    val cardShadowElevation: Dp,
    val defaultBorderBrush: Brush?
) {
    fun cardContainerColor(base: Color, isHighlighted: Boolean = false): Color {
        val alpha = if (isHighlighted) highlightedCardAlpha else cardAlpha
        return if (alpha >= 0.99f) base else base.copy(alpha = alpha)
    }

    fun borderBrushFor(accent: Color, isHighlighted: Boolean = false): Brush? {
        return when (style) {
            AppDesignStyle.MATERIAL -> if (isHighlighted) {
                Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.65f)))
            } else {
                null
            }
            AppDesignStyle.GLASS -> Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDark) 0.46f else 0.82f),
                    accent.copy(alpha = if (isHighlighted) 0.68f else 0.30f),
                    Color.White.copy(alpha = if (isDark) 0.10f else 0.28f)
                )
            )
            AppDesignStyle.OBSIDIAN_AMOLED -> Brush.linearGradient(
                colors = listOf(
                    accent.copy(alpha = if (isHighlighted) 0.90f else 0.38f),
                    Color(0xFF7C4DFF).copy(alpha = if (isHighlighted) 0.65f else 0.20f)
                )
            )
            AppDesignStyle.VELVET_SUNSET -> Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFF8FA3).copy(alpha = if (isHighlighted) 0.85f else 0.44f),
                    accent.copy(alpha = if (isHighlighted) 0.72f else 0.32f),
                    Color(0xFFFFC3A0).copy(alpha = 0.24f)
                )
            )
            AppDesignStyle.CRYSTAL_MINIMAL -> Brush.linearGradient(
                colors = listOf(
                    if (isDark) Color.White.copy(alpha = 0.26f) else Color(0xFF90CAF9).copy(alpha = 0.65f),
                    accent.copy(alpha = if (isHighlighted) 0.70f else 0.24f)
                )
            )
            AppDesignStyle.NEON_CYBER -> Brush.linearGradient(
                colors = listOf(
                    if (isHighlighted) Color(0xFF00F0FF) else accent.copy(alpha = 0.90f),
                    Color(0xFFFF2E93).copy(alpha = if (isHighlighted) 0.88f else 0.55f),
                    Color(0xFF00F0FF).copy(alpha = 0.45f)
                )
            )
            AppDesignStyle.ROYAL_ARABESQUE -> Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF3C669).copy(alpha = if (isHighlighted) 0.95f else 0.68f),
                    accent.copy(alpha = 0.40f),
                    Color(0xFFC58333).copy(alpha = if (isHighlighted) 0.85f else 0.45f)
                )
            )
            AppDesignStyle.NEUMORPHIC_STUDIO -> Brush.linearGradient(
                colors = listOf(
                    if (isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.92f),
                    if (isHighlighted) accent.copy(alpha = 0.55f) else Color.Transparent,
                    if (isDark) Color.Black.copy(alpha = 0.68f) else Color(0xFF9AA8BC).copy(alpha = 0.55f)
                )
            )
        }
    }
}

val LocalNaghamDesign = staticCompositionLocalOf {
    buildDesignTokens(
        style = AppDesignStyle.MATERIAL,
        primary = AmberGoldPrimary,
        isDark = true
    )
}

fun buildDesignShapes(style: AppDesignStyle): Shapes {
    return when (style) {
        AppDesignStyle.MATERIAL -> Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(30.dp)
        )
        AppDesignStyle.GLASS -> Shapes(
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(22.dp),
            large = RoundedCornerShape(26.dp),
            extraLarge = RoundedCornerShape(32.dp)
        )
        AppDesignStyle.OBSIDIAN_AMOLED -> Shapes(
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp)
        )
        AppDesignStyle.VELVET_SUNSET -> Shapes(
            small = RoundedCornerShape(18.dp),
            medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(34.dp)
        )
        AppDesignStyle.CRYSTAL_MINIMAL -> Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(26.dp)
        )
        AppDesignStyle.NEON_CYBER -> Shapes(
            small = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp, topEnd = 2.dp, bottomStart = 2.dp),
            medium = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 3.dp, bottomStart = 3.dp),
            large = CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp, topEnd = 4.dp, bottomStart = 4.dp),
            extraLarge = CutCornerShape(topStart = 22.dp, bottomEnd = 22.dp, topEnd = 6.dp, bottomStart = 6.dp)
        )
        AppDesignStyle.ROYAL_ARABESQUE -> Shapes(
            small = RoundedCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 5.dp, bottomStart = 5.dp),
            medium = RoundedCornerShape(topStart = 22.dp, bottomEnd = 22.dp, topEnd = 8.dp, bottomStart = 8.dp),
            large = RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp, topEnd = 10.dp, bottomStart = 10.dp),
            extraLarge = RoundedCornerShape(topStart = 32.dp, bottomEnd = 32.dp, topEnd = 12.dp, bottomStart = 12.dp)
        )
        AppDesignStyle.NEUMORPHIC_STUDIO -> Shapes(
            small = RoundedCornerShape(18.dp),
            medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(34.dp)
        )
    }
}

fun applyDesignStyleToColorScheme(
    base: ColorScheme,
    style: AppDesignStyle,
    darkTheme: Boolean
): ColorScheme {
    return when (style) {
        AppDesignStyle.MATERIAL -> base

        AppDesignStyle.GLASS -> if (darkTheme) {
            base.copy(
                background = Color(0xFF08070E),
                surface = Color(0xFF100D1C),
                surfaceVariant = Color(0xFF211938).copy(alpha = 0.44f),
                surfaceContainerLow = Color(0xFF161126).copy(alpha = 0.46f),
                surfaceContainer = Color(0xFF1C1630).copy(alpha = 0.56f),
                surfaceContainerHigh = Color(0xFF271E42).copy(alpha = 0.72f),
                surfaceContainerHighest = Color(0xFF332854).copy(alpha = 0.80f),
                outlineVariant = Color.White.copy(alpha = 0.20f)
            )
        } else {
            base.copy(
                background = Color(0xFFEAF2FB),
                surface = Color(0xFFF4F8FE),
                surfaceVariant = Color.White.copy(alpha = 0.62f),
                surfaceContainerLow = Color.White.copy(alpha = 0.58f),
                surfaceContainer = Color.White.copy(alpha = 0.66f),
                surfaceContainerHigh = Color.White.copy(alpha = 0.74f),
                surfaceContainerHighest = Color(0xFFDCE9F8).copy(alpha = 0.80f),
                outlineVariant = Color.White.copy(alpha = 0.85f)
            )
        }

        AppDesignStyle.OBSIDIAN_AMOLED -> if (darkTheme) {
            base.copy(
                background = Color(0xFF000000),
                surface = Color(0xFF050508),
                surfaceVariant = Color(0xFF0D0E14),
                surfaceContainerLowest = Color(0xFF000000),
                surfaceContainerLow = Color(0xFF08090E),
                surfaceContainer = Color(0xFF0D0F17),
                surfaceContainerHigh = Color(0xFF141722),
                surfaceContainerHighest = Color(0xFF1C2030),
                outline = base.primary.copy(alpha = 0.55f),
                outlineVariant = Color.White.copy(alpha = 0.14f)
            )
        } else {
            base.copy(
                background = Color(0xFFFFFFFF),
                surface = Color(0xFFF8F9FC),
                surfaceVariant = Color(0xFFEEF1F7),
                surfaceContainerLow = Color(0xFFF4F6FA),
                surfaceContainer = Color(0xFFECEFF6),
                surfaceContainerHigh = Color(0xFFE3E8F2),
                surfaceContainerHighest = Color(0xFFD8DFEC)
            )
        }

        AppDesignStyle.VELVET_SUNSET -> if (darkTheme) {
            base.copy(
                background = Color(0xFF130916),
                surface = Color(0xFF1C0E21),
                surfaceVariant = Color(0xFF2D1633).copy(alpha = 0.72f),
                surfaceContainerLow = Color(0xFF221127).copy(alpha = 0.76f),
                surfaceContainer = Color(0xFF2B1631).copy(alpha = 0.82f),
                surfaceContainerHigh = Color(0xFF381D3F).copy(alpha = 0.88f),
                surfaceContainerHighest = Color(0xFF47254F),
                secondary = Color(0xFFFF8FA3),
                outlineVariant = Color(0xFFFFB3C1).copy(alpha = 0.24f)
            )
        } else {
            base.copy(
                background = Color(0xFFFFF4F6),
                surface = Color(0xFFFFECF0),
                surfaceVariant = Color(0xFFFCE0E7),
                surfaceContainerLow = Color(0xFFFFF0F3),
                surfaceContainer = Color(0xFFFCE2E8),
                surfaceContainerHigh = Color(0xFFF7D5DE),
                surfaceContainerHighest = Color(0xFFEFBAC8)
            )
        }

        AppDesignStyle.CRYSTAL_MINIMAL -> if (darkTheme) {
            base.copy(
                background = Color(0xFF0B1017),
                surface = Color(0xFF101722),
                surfaceVariant = Color(0xFF172231).copy(alpha = 0.68f),
                surfaceContainerLow = Color(0xFF131C28).copy(alpha = 0.72f),
                surfaceContainer = Color(0xFF182332).copy(alpha = 0.76f),
                surfaceContainerHigh = Color(0xFF1F2D40).copy(alpha = 0.84f),
                surfaceContainerHighest = Color(0xFF273850),
                outlineVariant = Color(0xFF90CAF9).copy(alpha = 0.22f)
            )
        } else {
            base.copy(
                background = Color(0xFFF7FAFC),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFEDF3F8),
                surfaceContainerLow = Color(0xFFF3F7FB),
                surfaceContainer = Color(0xFFEAF1F7),
                surfaceContainerHigh = Color(0xFFE1EBF3),
                surfaceContainerHighest = Color(0xFFD5E2EE)
            )
        }

        AppDesignStyle.NEON_CYBER -> if (darkTheme) {
            base.copy(
                background = Color(0xFF050711),
                surface = Color(0xFF0A0E1F),
                surfaceVariant = Color(0xFF121936),
                surfaceContainerLow = Color(0xFF0E142B),
                surfaceContainer = Color(0xFF131B38),
                surfaceContainerHigh = Color(0xFF192347),
                surfaceContainerHighest = Color(0xFF222F5B),
                secondary = Color(0xFF00F0FF),
                onSecondary = Color(0xFF002024),
                secondaryContainer = Color(0xFF003B44),
                onSecondaryContainer = Color(0xFFB5FBFF),
                outline = Color(0xFF00F0FF).copy(alpha = 0.65f),
                outlineVariant = Color(0xFFFF2E93).copy(alpha = 0.42f)
            )
        } else {
            base.copy(
                background = Color(0xFFEEF2FF),
                surface = Color(0xFFF6F8FF),
                surfaceVariant = Color(0xFFDFE6FF),
                surfaceContainerLow = Color(0xFFE8EDFF),
                surfaceContainer = Color(0xFFDFE6FF),
                surfaceContainerHigh = Color(0xFFD4DDFF),
                surfaceContainerHighest = Color(0xFFC6D2FF),
                secondary = Color(0xFF007B8A),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFBCEEFF),
                onSecondaryContainer = Color(0xFF002025),
                outline = Color(0xFF007B8A),
                outlineVariant = Color(0xFFD81B60).copy(alpha = 0.35f)
            )
        }

        AppDesignStyle.ROYAL_ARABESQUE -> if (darkTheme) {
            base.copy(
                background = Color(0xFF120C08),
                surface = Color(0xFF1B120D),
                surfaceVariant = Color(0xFF2B1D14),
                surfaceContainerLow = Color(0xFF221710),
                surfaceContainer = Color(0xFF2A1C13),
                surfaceContainerHigh = Color(0xFF352318),
                surfaceContainerHighest = Color(0xFF422C1E),
                secondary = Color(0xFFF3C669),
                onSecondary = Color(0xFF2B1900),
                secondaryContainer = Color(0xFF53390D),
                onSecondaryContainer = Color(0xFFFFE3A8),
                outline = Color(0xFFD69E42),
                outlineVariant = Color(0xFF6B4E27)
            )
        } else {
            base.copy(
                background = Color(0xFFFDF6EA),
                surface = Color(0xFFFAF0DE),
                surfaceVariant = Color(0xFFF1E2C8),
                surfaceContainerLow = Color(0xFFF7ECDA),
                surfaceContainer = Color(0xFFF0E1C6),
                surfaceContainerHigh = Color(0xFFE8D5B5),
                surfaceContainerHighest = Color(0xFFDEC7A1),
                secondary = Color(0xFF945E08),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFFFDFAA),
                onSecondaryContainer = Color(0xFF2D1800),
                outline = Color(0xFFA87628),
                outlineVariant = Color(0xFFD8C09B)
            )
        }

        AppDesignStyle.NEUMORPHIC_STUDIO -> if (darkTheme) {
            base.copy(
                background = Color(0xFF1B212B),
                surface = Color(0xFF1F2632),
                surfaceVariant = Color(0xFF232B38),
                surfaceContainerLow = Color(0xFF202734),
                surfaceContainer = Color(0xFF232B39),
                surfaceContainerHigh = Color(0xFF273040),
                surfaceContainerHighest = Color(0xFF2E384A),
                outline = Color(0xFF6F7E95),
                outlineVariant = Color(0xFF374357)
            )
        } else {
            base.copy(
                background = Color(0xFFE4E9F1),
                surface = Color(0xFFE7ECF4),
                surfaceVariant = Color(0xFFE9EEF6),
                surfaceContainerLow = Color(0xFFE6EBF3),
                surfaceContainer = Color(0xFFE9EEF6),
                surfaceContainerHigh = Color(0xFFEDF2FA),
                surfaceContainerHighest = Color(0xFFD8E0EC),
                outline = Color(0xFF6B788D),
                outlineVariant = Color(0xFFC6D0DF)
            )
        }
    }
}

fun buildDesignTokens(
    style: AppDesignStyle,
    primary: Color,
    isDark: Boolean
): NaghamDesignTokens {
    val shapes = buildDesignShapes(style)
    val baseTokens = when (style) {
        AppDesignStyle.MATERIAL -> NaghamDesignTokens(
            style = style,
            isDark = isDark,
            cardShape = shapes.large,
            heroShape = shapes.large,
            pillShape = RoundedCornerShape(50),
            navBarShape = RoundedCornerShape(0.dp),
            isFloatingNavBar = false,
            cardAlpha = 0.72f,
            highlightedCardAlpha = 0.88f,
            navBarAlpha = 1f,
            borderWidth = 0.dp,
            cardShadowElevation = 0.dp,
            defaultBorderBrush = null
        )

        AppDesignStyle.GLASS -> NaghamDesignTokens(
            style = style,
            isDark = isDark,
            cardShape = shapes.large,
            heroShape = shapes.extraLarge,
            pillShape = RoundedCornerShape(50),
            navBarShape = RoundedCornerShape(34.dp),
            isFloatingNavBar = true,
            cardAlpha = if (isDark) 0.34f else 0.64f,
            highlightedCardAlpha = if (isDark) 0.54f else 0.82f,
            navBarAlpha = if (isDark) 0.74f else 0.82f,
            borderWidth = 1.1.dp,
            cardShadowElevation = 0.dp,
            defaultBorderBrush = null
        )

        AppDesignStyle.OBSIDIAN_AMOLED -> NaghamDesignTokens(
            style = style,
            isDark = isDark,
            cardShape = shapes.large,
            heroShape = shapes.extraLarge,
            pillShape = RoundedCornerShape(50),
            navBarShape = RoundedCornerShape(28.dp),
            isFloatingNavBar = true,
            cardAlpha = 0.96f,
            highlightedCardAlpha = 1f,
            navBarAlpha = 0.96f,
            borderWidth = 1.dp,
            cardShadowElevation = 0.dp,
            defaultBorderBrush = null
        )

        AppDesignStyle.VELVET_SUNSET -> NaghamDesignTokens(
            style = style,
            isDark = isDark,
            cardShape = shapes.large,
            heroShape = shapes.extraLarge,
            pillShape = RoundedCornerShape(50),
            navBarShape = RoundedCornerShape(32.dp),
            isFloatingNavBar = true,
            cardAlpha = if (isDark) 0.58f else 0.80f,
            highlightedCardAlpha = if (isDark) 0.78f else 0.92f,
            navBarAlpha = if (isDark) 0.84f else 0.90f,
            borderWidth = 1.15.dp,
            cardShadowElevation = 2.dp,
            defaultBorderBrush = null
        )

        AppDesignStyle.CRYSTAL_MINIMAL -> NaghamDesignTokens(
            style = style,
            isDark = isDark,
            cardShape = shapes.medium,
            heroShape = shapes.large,
            pillShape = RoundedCornerShape(50),
            navBarShape = RoundedCornerShape(0.dp),
            isFloatingNavBar = false,
            cardAlpha = if (isDark) 0.68f else 0.92f,
            highlightedCardAlpha = if (isDark) 0.86f else 0.98f,
            navBarAlpha = 0.94f,
            borderWidth = 1.dp,
            cardShadowElevation = 0.dp,
            defaultBorderBrush = null
        )

        AppDesignStyle.NEON_CYBER -> NaghamDesignTokens(
            style = style,
            isDark = isDark,
            cardShape = shapes.medium,
            heroShape = shapes.large,
            pillShape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp, topEnd = 2.dp, bottomStart = 2.dp),
            navBarShape = CutCornerShape(topStart = 20.dp, bottomEnd = 20.dp, topEnd = 4.dp, bottomStart = 4.dp),
            isFloatingNavBar = true,
            cardAlpha = if (isDark) 0.82f else 0.90f,
            highlightedCardAlpha = 0.94f,
            navBarAlpha = if (isDark) 0.90f else 0.95f,
            borderWidth = 1.4.dp,
            cardShadowElevation = 4.dp,
            defaultBorderBrush = null
        )

        AppDesignStyle.ROYAL_ARABESQUE -> NaghamDesignTokens(
            style = style,
            isDark = isDark,
            cardShape = shapes.medium,
            heroShape = shapes.large,
            pillShape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 6.dp, bottomStart = 6.dp),
            navBarShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            isFloatingNavBar = false,
            cardAlpha = if (isDark) 0.88f else 0.92f,
            highlightedCardAlpha = 0.96f,
            navBarAlpha = 0.96f,
            borderWidth = 1.25.dp,
            cardShadowElevation = 2.dp,
            defaultBorderBrush = null
        )

        AppDesignStyle.NEUMORPHIC_STUDIO -> NaghamDesignTokens(
            style = style,
            isDark = isDark,
            cardShape = shapes.large,
            heroShape = shapes.extraLarge,
            pillShape = RoundedCornerShape(50),
            navBarShape = RoundedCornerShape(30.dp),
            isFloatingNavBar = true,
            cardAlpha = 0.96f,
            highlightedCardAlpha = 1f,
            navBarAlpha = 0.98f,
            borderWidth = 1.6.dp,
            cardShadowElevation = if (isDark) 8.dp else 6.dp,
            defaultBorderBrush = null
        )
    }

    return baseTokens.copy(
        defaultBorderBrush = baseTokens.borderBrushFor(primary, isHighlighted = false)
    )
}

@Composable
fun Modifier.naghamDesignBorder(
    accentColor: Color = MaterialTheme.colorScheme.primary,
    shapeOverride: Shape? = null,
    isHighlighted: Boolean = false
): Modifier {
    val tokens = LocalNaghamDesign.current
    val shape = shapeOverride ?: tokens.cardShape
    val brush = tokens.borderBrushFor(accentColor, isHighlighted = isHighlighted)
    val width = when {
        tokens.style == AppDesignStyle.MATERIAL && isHighlighted -> 1.8.dp
        tokens.style == AppDesignStyle.MATERIAL -> 0.dp
        isHighlighted -> tokens.borderWidth + 0.6.dp
        else -> tokens.borderWidth
    }
    return this
        .then(
            if (tokens.cardShadowElevation > 0.dp) {
                Modifier.shadow(
                    elevation = tokens.cardShadowElevation,
                    shape = shape,
                    clip = false
                )
            } else {
                Modifier
            }
        )
        .then(
            if (brush != null && width > 0.dp) {
                Modifier.border(width = width, brush = brush, shape = shape)
            } else {
                Modifier
            }
        )
}

/**
 * Full-bleed atmospheric backdrop tailored to the active [AppDesignStyle].
 * Uses [Modifier.drawWithCache] so radial/linear brushes and geometric paths are allocated
 * only once per layout size or theme change, keeping page swipes silky-smooth at 60/120fps.
 */
@Composable
fun NaghamThemedBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val tokens = LocalNaghamDesign.current
    val bgColor = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (tokens.style != AppDesignStyle.MATERIAL) {
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        if (w <= 2f || h <= 2f) {
                            onDrawBehind { }
                        } else {
                            val style = tokens.style
                            val isDark = tokens.isDark
                            when (style) {
                                AppDesignStyle.GLASS -> {
                                    val topViolet = Color(0xFF9D5CE6)
                                    val plumMid = Color(0xFF5A2E8C)
                                    val topAlpha = if (isDark) 0.52f else 0.28f
                                    val topRadius = min(w, h) * 0.96f
                                    val topCenter = Offset(w * 0.45f, h * 0.05f)
                                    val topBrush = Brush.radialGradient(
                                        colors = listOf(
                                            topViolet.copy(alpha = topAlpha),
                                            primary.copy(alpha = topAlpha * 0.55f),
                                            plumMid.copy(alpha = topAlpha * 0.28f),
                                            Color.Transparent
                                        ),
                                        center = topCenter,
                                        radius = topRadius
                                    )
                                    val r2 = min(w, h) * 0.66f
                                    val c2 = Offset(w * 0.86f, h * 0.32f)
                                    val brush2 = Brush.radialGradient(
                                        colors = listOf(
                                            primary.copy(alpha = if (isDark) 0.22f else 0.18f),
                                            secondary.copy(alpha = 0.08f),
                                            Color.Transparent
                                        ),
                                        center = c2,
                                        radius = r2
                                    )
                                    val r3 = min(w, h) * 0.62f
                                    val c3 = Offset(w * 0.16f, h * 0.88f)
                                    val brush3 = Brush.radialGradient(
                                        colors = listOf(
                                            tertiary.copy(alpha = if (isDark) 0.14f else 0.12f),
                                            Color.Transparent
                                        ),
                                        center = c3,
                                        radius = r3
                                    )
                                    onDrawBehind {
                                        drawCircle(brush = topBrush, radius = topRadius, center = topCenter)
                                        drawCircle(brush = brush2, radius = r2, center = c2)
                                        drawCircle(brush = brush3, radius = r3, center = c3)
                                    }
                                }

                                AppDesignStyle.OBSIDIAN_AMOLED -> {
                                    val rimRadius = min(w, h) * 0.58f
                                    val rimCenter = Offset(w * 0.5f, 0f)
                                    val rimBrush = Brush.radialGradient(
                                        colors = listOf(
                                            primary.copy(alpha = if (isDark) 0.16f else 0.10f),
                                            Color.Transparent
                                        ),
                                        center = rimCenter,
                                        radius = rimRadius
                                    )
                                    onDrawBehind {
                                        drawCircle(brush = rimBrush, radius = rimRadius, center = rimCenter)
                                    }
                                }

                                AppDesignStyle.VELVET_SUNSET -> {
                                    val rTop = min(w, h) * 0.95f
                                    val cTop = Offset(w * 0.30f, h * 0.04f)
                                    val sunsetTop = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFF6B8B).copy(alpha = if (isDark) 0.38f else 0.22f),
                                            Color(0xFFFF9E7D).copy(alpha = if (isDark) 0.20f else 0.12f),
                                            Color.Transparent
                                        ),
                                        center = cTop,
                                        radius = rTop
                                    )
                                    val rBottom = min(w, h) * 0.72f
                                    val cBottom = Offset(w * 0.85f, h * 0.82f)
                                    val sunsetBottom = Brush.radialGradient(
                                        colors = listOf(
                                            primary.copy(alpha = if (isDark) 0.20f else 0.12f),
                                            Color.Transparent
                                        ),
                                        center = cBottom,
                                        radius = rBottom
                                    )
                                    onDrawBehind {
                                        drawCircle(brush = sunsetTop, radius = rTop, center = cTop)
                                        drawCircle(brush = sunsetBottom, radius = rBottom, center = cBottom)
                                    }
                                }

                                AppDesignStyle.CRYSTAL_MINIMAL -> {
                                    val crystalBrush = Brush.linearGradient(
                                        colors = listOf(
                                            primary.copy(alpha = if (isDark) 0.10f else 0.08f),
                                            Color.Transparent,
                                            secondary.copy(alpha = if (isDark) 0.07f else 0.06f)
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(w, h)
                                    )
                                    onDrawBehind {
                                        drawRect(brush = crystalBrush)
                                    }
                                }

                                AppDesignStyle.NEON_CYBER -> {
                                    val glowAlpha = if (isDark) 0.26f else 0.16f
                                    val topGlowRadius = min(w, h) * 0.7f
                                    val c1 = Offset(w * 0.85f, h * 0.10f)
                                    val b1 = Brush.radialGradient(
                                        colors = listOf(Color(0xFF00F0FF).copy(alpha = glowAlpha), Color.Transparent),
                                        center = c1,
                                        radius = topGlowRadius
                                    )
                                    val c2 = Offset(w * 0.12f, h * 0.84f)
                                    val b2 = Brush.radialGradient(
                                        colors = listOf(Color(0xFFFF2E93).copy(alpha = glowAlpha * 0.9f), Color.Transparent),
                                        center = c2,
                                        radius = topGlowRadius
                                    )
                                    val lineColor = primary.copy(alpha = if (isDark) 0.08f else 0.06f)
                                    val strokePx = 1.dp.toPx()
                                    val stepX = (w / 8f).coerceAtLeast(24f)
                                    val stepY = (h / 12f).coerceAtLeast(24f)
                                    onDrawBehind {
                                        drawCircle(brush = b1, radius = topGlowRadius, center = c1)
                                        drawCircle(brush = b2, radius = topGlowRadius, center = c2)
                                        var x = stepX
                                        while (x < w) {
                                            drawLine(lineColor, Offset(x, 0f), Offset(x, h), strokePx)
                                            x += stepX
                                        }
                                        var y = stepY
                                        while (y < h) {
                                            drawLine(lineColor, Offset(0f, y), Offset(w, y), strokePx)
                                            y += stepY
                                        }
                                    }
                                }

                                AppDesignStyle.ROYAL_ARABESQUE -> {
                                    val gold = Color(0xFFF3C669)
                                    val lanternRadius = min(w, h) * 0.80f
                                    val lanternCenter = Offset(w * 0.5f, h * 0.06f)
                                    val lanternBrush = Brush.radialGradient(
                                        colors = listOf(
                                            gold.copy(alpha = if (isDark) 0.18f else 0.16f),
                                            primary.copy(alpha = 0.06f),
                                            Color.Transparent
                                        ),
                                        center = lanternCenter,
                                        radius = lanternRadius
                                    )
                                    val starColor = gold.copy(alpha = if (isDark) 0.08f else 0.11f)
                                    val starRadius = min(w, h) * 0.24f
                                    val rosette1Center = Offset(w * 0.88f, h * 0.18f)
                                    val rosette2Center = Offset(w * 0.12f, h * 0.78f)
                                    val rosette1Path = buildRosettePath(rosette1Center, starRadius)
                                    val rosette2Path = buildRosettePath(rosette2Center, starRadius * 0.85f)
                                    val strokeOuter = Stroke(width = 1.4.dp.toPx())
                                    val strokeInner = Stroke(width = 1.dp.toPx())
                                    onDrawBehind {
                                        drawCircle(brush = lanternBrush, radius = lanternRadius, center = lanternCenter)
                                        if (rosette1Path != null) {
                                            drawPath(rosette1Path, starColor, style = strokeOuter)
                                            drawCircle(starColor, starRadius * 0.25f, rosette1Center, style = strokeInner)
                                        }
                                        if (rosette2Path != null) {
                                            drawPath(rosette2Path, starColor, style = strokeOuter)
                                            drawCircle(starColor, starRadius * 0.85f * 0.25f, rosette2Center, style = strokeInner)
                                        }
                                    }
                                }

                                AppDesignStyle.NEUMORPHIC_STUDIO -> {
                                    val neuBrush = Brush.linearGradient(
                                        colors = if (isDark) {
                                            listOf(
                                                Color.White.copy(alpha = 0.05f),
                                                primary.copy(alpha = 0.03f),
                                                Color.Black.copy(alpha = 0.24f)
                                            )
                                        } else {
                                            listOf(
                                                Color.White.copy(alpha = 0.65f),
                                                primary.copy(alpha = 0.04f),
                                                Color(0xFFB5C2D4).copy(alpha = 0.28f)
                                            )
                                        },
                                        start = Offset.Zero,
                                        end = Offset(w, h)
                                    )
                                    onDrawBehind {
                                        drawRect(brush = neuBrush)
                                    }
                                }

                                AppDesignStyle.MATERIAL -> onDrawBehind { }
                            }
                        }
                    }
            )
        }
        content()
    }
}

private fun buildRosettePath(center: Offset, radius: Float): Path? {
    if (radius <= 4f) return null
    val path = Path()
    val points = 16
    val innerRadius = radius * 0.46f
    for (i in 0 until points) {
        val angle = (i * 2.0 * PI / points) - (PI / 2.0)
        val r = if (i % 2 == 0) radius else innerRadius
        val px = center.x + (r * cos(angle)).toFloat()
        val py = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    return path
}
