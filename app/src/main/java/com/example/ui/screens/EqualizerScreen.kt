package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.PlaybackState
import com.example.data.model.EqualizerPreset
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.LocalNaghamColors
import com.example.ui.theme.LocalNaghamDesign
import com.example.ui.theme.NaghamThemedBackdrop
import com.example.ui.theme.ThemePreferences
import com.example.ui.theme.naghamDesignBorder
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private val DEFAULT_EQ_FREQUENCIES = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
private val PLAYBACK_SPEED_STEPS = listOf(0.75f, 1.0f, 1.25f, 1.5f)

@Composable
fun EqualizerScreen(
    playbackState: PlaybackState,
    onClose: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onResetFlat: () -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onBandChanged: (Int, Long) -> Unit,
    onBassChanged: (Long) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSyncToCloud: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val extColors = LocalNaghamColors.current
    val designTokens = LocalNaghamDesign.current
    val accent = if (playbackState.isQuranMode) extColors.quranAccent else MaterialTheme.colorScheme.primary

    LaunchedEffect(context) {
        ThemePreferences.getEqVerticalMode(context)
    }
    val isVerticalMode by ThemePreferences.eqVerticalModeFlow.collectAsStateWithLifecycle()

    NaghamThemedBackdrop(
        modifier = modifier
            .fillMaxSize()
            .testTag("equalizer_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
            ) {
                // Top Studio Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("eq_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.cancelAction,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = strings.eqTitle,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                EqualizerStatusPill(
                                    enabled = playbackState.eqEnabled,
                                    accentColor = accent
                                )
                            }
                            Text(
                                text = strings.eqSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onResetFlat,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("eq_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = strings.eqResetFlat,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = playbackState.eqEnabled,
                            onCheckedChange = onToggleEnabled,
                            modifier = Modifier.testTag("eq_master_switch")
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("equalizer_scroll_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (playbackState.isQuranMode) {
                        item {
                            Surface(
                                shape = designTokens.cardShape,
                                color = extColors.quranAccent.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, extColors.quranAccent.copy(alpha = 0.45f), designTokens.cardShape)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = extColors.quranAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = strings.eqQuranVocalNotice,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 1. Live Acoustic Frequency Response Curve + Cloud Sync
                    item {
                        Card(
                            shape = designTokens.cardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = designTokens.cardContainerColor(
                                    base = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    isHighlighted = true
                                )
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .naghamDesignBorder(accentColor = accent, isHighlighted = true)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val activePreset = EqualizerPreset.fromId(playbackState.eqPresetId)
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.Equalizer,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (strings.isArabic) activePreset.labelAr else activePreset.labelEn,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = onSyncToCloud,
                                        modifier = Modifier.testTag("eq_sync_cloud_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.saveAction, style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                EqualizerResponseCurveCanvas(
                                    bandsMb = playbackState.eqBandsMb,
                                    enabled = playbackState.eqEnabled,
                                    accentColor = accent,
                                    secondaryColor = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(112.dp)
                                        .testTag("eq_response_curve_canvas")
                                )
                            }
                        }
                    }

                    // 2. Presets Strip
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = strings.eqPresetsHeader,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            EqualizerPresetsRow(
                                selectedPresetId = playbackState.eqPresetId,
                                onSelectPreset = onSelectPreset
                            )
                        }
                    }

                    // 3. Multi-Band Frequency Sliders (Horizontal Sliders <-> Vertical Studio Faders)
                    item {
                        FrequencyBandsSlidersCard(
                            playbackState = playbackState,
                            accentColor = accent,
                            isVerticalMode = isVerticalMode,
                            onChangeVerticalMode = { vertical ->
                                ThemePreferences.saveEqVerticalMode(context, vertical)
                            },
                            onBandChanged = onBandChanged
                        )
                    }

                    // 4. Acoustic Enhancers (Bass Boost Slider + Playback Speed Slider)
                    item {
                        AcousticEnhancersCard(
                            playbackState = playbackState,
                            accentColor = accent,
                            onBassChanged = onBassChanged,
                            onSpeedChanged = onSpeedChanged
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InlineEqualizerPanel(
    playbackState: PlaybackState,
    accentColor: Color,
    onToggleEnabled: (Boolean) -> Unit = {},
    onResetFlat: () -> Unit = {},
    onSelectPreset: (EqualizerPreset) -> Unit,
    onBandChanged: (Int, Long) -> Unit,
    onBassChanged: (Long) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSyncToCloud: () -> Unit,
    onOpenFullEqualizer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val designTokens = LocalNaghamDesign.current
    LaunchedEffect(context) {
        ThemePreferences.getEqVerticalMode(context)
    }
    val isVerticalMode by ThemePreferences.eqVerticalModeFlow.collectAsStateWithLifecycle()

    Card(
        modifier = modifier
            .fillMaxSize()
            .naghamDesignBorder(accentColor = accentColor)
            .testTag("inline_equalizer_panel"),
        colors = CardDefaults.cardColors(
            containerColor = designTokens.cardContainerColor(
                MaterialTheme.colorScheme.surfaceContainerLow
            )
        ),
        shape = designTokens.cardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = strings.eqTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        EqualizerStatusPill(
                            enabled = playbackState.eqEnabled,
                            accentColor = accentColor
                        )
                    }
                    Text(
                        text = strings.eqSubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenFullEqualizer != null) {
                        IconButton(
                            onClick = onOpenFullEqualizer,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("player_expand_full_eq_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = strings.eqOpenStudioBtn,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = onSyncToCloud,
                        modifier = Modifier.testTag("inline_sync_eq_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.saveAction, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.eqMasterEnable,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onResetFlat,
                        modifier = Modifier.testTag("inline_eq_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = strings.eqResetFlat,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Switch(
                        checked = playbackState.eqEnabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.testTag("inline_eq_master_switch")
                    )
                }
            }

            EqualizerResponseCurveCanvas(
                bandsMb = playbackState.eqBandsMb,
                enabled = playbackState.eqEnabled,
                accentColor = accentColor,
                secondaryColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
            )

            EqualizerPresetsRow(
                selectedPresetId = playbackState.eqPresetId,
                onSelectPreset = onSelectPreset
            )

            FrequencyBandsSlidersSection(
                playbackState = playbackState,
                accentColor = accentColor,
                isVerticalMode = isVerticalMode,
                onChangeVerticalMode = { vertical ->
                    ThemePreferences.saveEqVerticalMode(context, vertical)
                },
                onBandChanged = onBandChanged
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

            AcousticEnhancersSection(
                playbackState = playbackState,
                accentColor = accentColor,
                onBassChanged = onBassChanged,
                onSpeedChanged = onSpeedChanged
            )
        }
    }
}

@Composable
internal fun EqualizerStatusPill(
    enabled: Boolean,
    accentColor: Color
) {
    val strings = LocalAppStrings.current
    val bgColor by animateColorAsState(
        targetValue = if (enabled) accentColor.copy(alpha = 0.20f)
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "eq_status_bg"
    )
    Surface(
        shape = CircleShape,
        color = bgColor
    ) {
        Text(
            text = if (enabled) strings.eqActiveBadge else strings.eqBypassedBadge,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
internal fun EqualizerResponseCurveCanvas(
    bandsMb: List<Long>,
    enabled: Boolean,
    accentColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val activeFactor = if (enabled) 1f else 0f
    val b0 by animateFloatAsState(
        targetValue = (bandsMb.getOrElse(0) { 0L }.coerceIn(-1500L, 1500L) / 1500f) * activeFactor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "eq_b0"
    )
    val b1 by animateFloatAsState(
        targetValue = (bandsMb.getOrElse(1) { 0L }.coerceIn(-1500L, 1500L) / 1500f) * activeFactor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "eq_b1"
    )
    val b2 by animateFloatAsState(
        targetValue = (bandsMb.getOrElse(2) { 0L }.coerceIn(-1500L, 1500L) / 1500f) * activeFactor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "eq_b2"
    )
    val b3 by animateFloatAsState(
        targetValue = (bandsMb.getOrElse(3) { 0L }.coerceIn(-1500L, 1500L) / 1500f) * activeFactor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "eq_b3"
    )
    val b4 by animateFloatAsState(
        targetValue = (bandsMb.getOrElse(4) { 0L }.coerceIn(-1500L, 1500L) / 1500f) * activeFactor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "eq_b4"
    )
    val normalizedBands = listOf(b0, b1, b2, b3, b4)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val surfaceBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)
    val disabledDotColor = MaterialTheme.colorScheme.outline

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Canvas(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(surfaceBg)
                .border(1.dp, accentColor.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            val w = size.width
            val h = size.height
            if (w <= 2f || h <= 2f) return@Canvas
            val midY = h / 2f

            // +15 dB, 0 dB, -15 dB horizontal reference guides
            drawLine(color = gridColor, start = Offset(0f, h * 0.08f), end = Offset(w, h * 0.08f), strokeWidth = 1.dp.toPx())
            drawLine(
                color = accentColor.copy(alpha = 0.30f),
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1.2.dp.toPx()
            )
            drawLine(color = gridColor, start = Offset(0f, h * 0.92f), end = Offset(w, h * 0.92f), strokeWidth = 1.dp.toPx())

            val points = normalizedBands.mapIndexed { idx, norm ->
                val x = (w / (normalizedBands.size - 1).coerceAtLeast(1)) * idx
                drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1.dp.toPx())
                val y = midY - (norm * (h * 0.42f))
                Offset(x, y)
            }

            if (points.size >= 2) {
                val strokePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val cx = (prev.x + curr.x) / 2f
                        cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                    }
                }
                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(points.last().x, h)
                    lineTo(points.first().x, h)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (enabled) 0.34f else 0.10f),
                            secondaryColor.copy(alpha = if (enabled) 0.12f else 0.04f),
                            Color.Transparent
                        )
                    )
                )
                drawPath(
                    path = strokePath,
                    brush = Brush.horizontalGradient(
                        colors = if (enabled) listOf(accentColor, secondaryColor, accentColor)
                        else listOf(accentColor.copy(alpha = 0.4f), accentColor.copy(alpha = 0.4f))
                    ),
                    style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                )
                points.forEach { pt ->
                    drawCircle(
                        color = accentColor.copy(alpha = if (enabled) 0.28f else 0.10f),
                        radius = 7.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = if (enabled) accentColor else disabledDotColor,
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}

@Composable
private fun EqualizerPresetsRow(
    selectedPresetId: String,
    onSelectPreset: (EqualizerPreset) -> Unit
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EqualizerPreset.entries.forEach { preset ->
            val selected = selectedPresetId == preset.id
            FilterChip(
                selected = selected,
                onClick = { onSelectPreset(preset) },
                leadingIcon = if (selected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                label = { Text(if (strings.isArabic) preset.labelAr else preset.labelEn) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("eq_preset_${preset.id}")
            )
        }
    }
}

@Composable
private fun FrequencyBandsSlidersCard(
    playbackState: PlaybackState,
    accentColor: Color,
    isVerticalMode: Boolean,
    onChangeVerticalMode: (Boolean) -> Unit,
    onBandChanged: (Int, Long) -> Unit
) {
    val designTokens = LocalNaghamDesign.current
    Card(
        shape = designTokens.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = designTokens.cardContainerColor(MaterialTheme.colorScheme.surfaceVariant)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .naghamDesignBorder()
            .testTag("eq_frequency_bands_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            FrequencyBandsSlidersSection(
                playbackState = playbackState,
                accentColor = accentColor,
                isVerticalMode = isVerticalMode,
                onChangeVerticalMode = onChangeVerticalMode,
                onBandChanged = onBandChanged
            )
        }
    }
}

@Composable
private fun FrequencyBandsSlidersSection(
    playbackState: PlaybackState,
    accentColor: Color,
    isVerticalMode: Boolean,
    onChangeVerticalMode: (Boolean) -> Unit,
    onBandChanged: (Int, Long) -> Unit
) {
    val strings = LocalAppStrings.current
    val bandLabels = remember(playbackState.eqBandFrequenciesLabel) {
        if (playbackState.eqBandFrequenciesLabel.size == 5) playbackState.eqBandFrequenciesLabel
        else DEFAULT_EQ_FREQUENCIES
    }

    Column(
        modifier = Modifier.alpha(if (playbackState.eqEnabled) 1f else 0.58f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.eqBandsHeader,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = !isVerticalMode,
                    onClick = { onChangeVerticalMode(false) },
                    label = { Text(strings.eqViewHorizontal, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("eq_mode_horizontal_button")
                )
                FilterChip(
                    selected = isVerticalMode,
                    onClick = { onChangeVerticalMode(true) },
                    label = { Text(strings.eqViewVertical, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("eq_mode_vertical_button")
                )
            }
        }

        if (isVerticalMode) {
            // Vertical 5-Band Studio Fader Rack (Low 60Hz -> High 14kHz Left-to-Right)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("eq_vertical_faders_rack"),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bandLabels.forEachIndexed { idx, freqLabel ->
                        val mb = playbackState.eqBandsMb.getOrElse(idx) { 0L }
                        val dbText = String.format(Locale.US, "%+.1f", mb / 100f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (mb != 0L) accentColor.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = playbackState.eqEnabled) {
                                        onBandChanged(idx, 0L)
                                    }
                                    .testTag("eq_band_reset_$idx")
                            ) {
                                Text(
                                    text = "${dbText}dB",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (mb != 0L) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Slider(
                                value = mb.toFloat(),
                                onValueChange = { onBandChanged(idx, it.roundToLong()) },
                                valueRange = -1500f..1500f,
                                enabled = playbackState.eqEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                                ),
                                modifier = Modifier
                                    .verticalFader()
                                    .width(176.dp)
                                    .height(48.dp)
                                    .testTag("eq_band_slider_$idx")
                            )

                            Text(
                                text = freqLabel,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = strings.eqBandRoles.getOrElse(idx) { "" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            // Detailed Horizontal Frequency Sliders with Role Labels & Tap-to-Zero dB Badges
            bandLabels.forEachIndexed { idx, freqLabel ->
                val mb = playbackState.eqBandsMb.getOrElse(idx) { 0L }
                val dbText = String.format(Locale.US, "%+.1f dB", mb / 100f)
                val roleLabel = strings.eqBandRoles.getOrElse(idx) { "" }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = freqLabel,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (roleLabel.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "• $roleLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (mb != 0L) accentColor.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(enabled = playbackState.eqEnabled) {
                                        onBandChanged(idx, 0L)
                                    }
                                    .testTag("eq_band_reset_$idx")
                            ) {
                                Text(
                                    text = dbText,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (mb != 0L) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Slider(
                                    value = mb.toFloat(),
                                    onValueChange = { onBandChanged(idx, it.roundToLong()) },
                                    valueRange = -1500f..1500f,
                                    enabled = playbackState.eqEnabled,
                                    colors = SliderDefaults.colors(
                                        thumbColor = accentColor,
                                        activeTrackColor = accentColor,
                                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("eq_band_slider_$idx")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "-15 dB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "0 dB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "+15 dB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AcousticEnhancersCard(
    playbackState: PlaybackState,
    accentColor: Color,
    onBassChanged: (Long) -> Unit,
    onSpeedChanged: (Float) -> Unit
) {
    val designTokens = LocalNaghamDesign.current
    Card(
        shape = designTokens.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = designTokens.cardContainerColor(MaterialTheme.colorScheme.surfaceVariant)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .naghamDesignBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AcousticEnhancersSection(
                playbackState = playbackState,
                accentColor = accentColor,
                onBassChanged = onBassChanged,
                onSpeedChanged = onSpeedChanged
            )
        }
    }
}

@Composable
private fun AcousticEnhancersSection(
    playbackState: PlaybackState,
    accentColor: Color,
    onBassChanged: (Long) -> Unit,
    onSpeedChanged: (Float) -> Unit
) {
    val strings = LocalAppStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = strings.eqEnhancersHeader,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Deep Bass Boost Slider
        Column(modifier = Modifier.alpha(if (playbackState.eqEnabled) 1f else 0.58f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speaker,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.bassBoostLabel, style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    text = "${playbackState.bassBoostStrength / 10}%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Slider(
                    value = playbackState.bassBoostStrength.toFloat(),
                    onValueChange = { onBassChanged(it.roundToLong()) },
                    valueRange = 0f..1000f,
                    enabled = playbackState.eqEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("eq_bass_slider")
                )
            }
        }

        // Playback Speed Slider + Quick Step Chips
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.playbackSpeedLabel, style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    text = String.format(Locale.US, "%.2fx", playbackState.playbackSpeed),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Slider(
                    value = playbackState.playbackSpeed,
                    onValueChange = { raw ->
                        val rounded = (raw * 20f).roundToLong() / 20f
                        onSpeedChanged(rounded.coerceIn(0.75f, 1.5f))
                    },
                    valueRange = 0.75f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("eq_speed_slider")
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PLAYBACK_SPEED_STEPS.forEach { spd ->
                    FilterChip(
                        selected = abs(playbackState.playbackSpeed - spd) < 0.03f,
                        onClick = { onSpeedChanged(spd) },
                        label = { Text("${spd}x") },
                        modifier = Modifier.testTag("eq_speed_chip_$spd")
                    )
                }
            }
        }
    }
}

private fun Modifier.verticalFader(): Modifier = this
    .graphicsLayer {
        rotationZ = 270f
        transformOrigin = TransformOrigin(0f, 0f)
    }
    .layout { measurable, constraints ->
        val placeable = measurable.measure(
            Constraints(
                minWidth = constraints.minHeight,
                maxWidth = constraints.maxHeight,
                minHeight = constraints.minWidth,
                maxHeight = constraints.maxWidth
            )
        )
        layout(placeable.height, placeable.width) {
            placeable.place(-placeable.width, 0)
        }
    }
