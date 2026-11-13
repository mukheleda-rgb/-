package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.CloudTrack
import com.example.ui.components.TrackArtwork
import com.example.ui.theme.PlayerThemeStyle
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Full-width horizontal swipe container that moves the active track 1:1 with the user's finger
 * and, upon crossing threshold, glides the track ALL THE WAY to the end of the screen in the
 * exact direction it was swiped before bringing the next/previous track smoothly into place.
 */
@Composable
fun SwipeableTrackStage(
    onSwipeLeftToNext: () -> Unit,
    onSwipeRightToPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    swipeThresholdDp: Dp = 50.dp,
    testTag: String? = "player_artwork_swipe_area",
    onPeakTranslationObserved: ((translationPx: Float, stageWidthPx: Float) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val currentOnNext by rememberUpdatedState(onSwipeLeftToNext)
    val currentOnPrev by rememberUpdatedState(onSwipeRightToPrevious)
    val currentPeakObserver by rememberUpdatedState(onPeakTranslationObserved)

    val defaultWidthPx = with(density) { 360.dp.toPx() }
    val thresholdPx = with(density) { swipeThresholdDp.toPx() }
    var measuredWidthPx by remember { mutableFloatStateOf(defaultWidthPx) }
    val offsetX = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .onSizeChanged { sz ->
                if (sz.width > 24) {
                    measuredWidthPx = sz.width.toFloat()
                }
            }
            .graphicsLayer {
                val w = measuredWidthPx.coerceAtLeast(120f)
                val tx = offsetX.value
                translationX = tx
                rotationZ = (tx / w) * 5.5f
                val progressOut = (abs(tx) / w).coerceIn(0f, 1f)
                val sc = 1f - (progressOut * 0.08f)
                scaleX = sc
                scaleY = sc
                alpha = (1f - (progressOut * 0.55f)).coerceIn(0.18f, 1f)
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val w = measuredWidthPx.coerceAtLeast(160f)
                        val effectiveThreshold = min(thresholdPx, w * 0.20f)
                        val currentTx = offsetX.value
                        scope.launch {
                            when {
                                currentTx < -effectiveThreshold -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    // Glide track all the way past the left screen edge in the swiped direction
                                    val fullLeftExit = -w * 1.12f
                                    offsetX.animateTo(
                                        targetValue = fullLeftExit,
                                        animationSpec = tween(durationMillis = 175, easing = FastOutSlowInEasing)
                                    )
                                    currentPeakObserver?.invoke(offsetX.value, w)
                                    currentOnNext()
                                    // Enter new track smoothly from the opposite right screen edge
                                    offsetX.snapTo(w * 1.05f)
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 195, easing = FastOutSlowInEasing)
                                    )
                                }

                                currentTx > effectiveThreshold -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    // Glide track all the way past the right screen edge in the swiped direction
                                    val fullRightExit = w * 1.12f
                                    offsetX.animateTo(
                                        targetValue = fullRightExit,
                                        animationSpec = tween(durationMillis = 175, easing = FastOutSlowInEasing)
                                    )
                                    currentPeakObserver?.invoke(offsetX.value, w)
                                    currentOnPrev()
                                    // Enter previous track smoothly from the opposite left screen edge
                                    offsetX.snapTo(-w * 1.05f)
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 195, easing = FastOutSlowInEasing)
                                    )
                                }

                                else -> {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount)
                        }
                    }
                )
            }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Renders the user-selected [PlayerThemeStyle] centerpiece inside [FullNowPlayingOverlay].
 */
@Composable
fun PlayerThemeHeroStage(
    style: PlayerThemeStyle,
    track: CloudTrack,
    prevTrack: CloudTrack?,
    nextTrack: CloudTrack?,
    isPlaying: Boolean,
    progress: Float,
    maxDurMs: Long,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("player_theme_stage_${style.id}"),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            PlayerThemeStyle.AURORA_RING -> AuroraRingPlayerStage(
                track = track,
                prevTrack = prevTrack,
                nextTrack = nextTrack,
                progress = progress,
                maxDurMs = maxDurMs,
                accent = accent
            )

            PlayerThemeStyle.STUDIO_COVER -> StudioCoverPlayerStage(
                track = track,
                isPlaying = isPlaying,
                accent = accent
            )

            PlayerThemeStyle.VINYL_TURNTABLE -> VinylTurntablePlayerStage(
                track = track,
                isPlaying = isPlaying,
                accent = accent
            )

            PlayerThemeStyle.WAVEFORM_IMMERSIVE -> WaveformImmersivePlayerStage(
                track = track,
                progress = progress,
                accent = accent
            )

            PlayerThemeStyle.MINIMAL_FLOATING -> MinimalFloatingPlayerStage(
                track = track,
                progress = progress,
                accent = accent
            )

            PlayerThemeStyle.RADIAL_SPECTRUM -> RadialSpectrumPlayerStage(
                track = track,
                isPlaying = isPlaying,
                progress = progress,
                accent = accent
            )
        }
    }
}

@Composable
private fun AuroraRingPlayerStage(
    track: CloudTrack,
    prevTrack: CloudTrack?,
    nextTrack: CloudTrack?,
    progress: Float,
    maxDurMs: Long,
    accent: Color
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (prevTrack != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-30).dp)
                        .size(88.dp)
                        .alpha(0.45f)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                ) {
                    TrackArtwork(
                        track = prevTrack,
                        shape = CircleShape,
                        showFolderBadge = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (nextTrack != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 30.dp)
                        .size(88.dp)
                        .alpha(0.45f)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                ) {
                    TrackArtwork(
                        track = nextTrack,
                        shape = CircleShape,
                        showFolderBadge = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Box(
                modifier = Modifier.size(228.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    if (w <= 2f || h <= 2f) return@Canvas
                    val center = Offset(w / 2f, h / 2f)
                    val outerRadius = (min(w, h) / 2f) - 4.dp.toPx()

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.04f),
                                Color.White.copy(alpha = 0.14f),
                                accent.copy(alpha = 0.16f)
                            ),
                            center = center,
                            radius = outerRadius
                        ),
                        radius = outerRadius,
                        center = center
                    )

                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.55f),
                                accent.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        ),
                        radius = outerRadius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    val arcInset = 13.dp.toPx()
                    val arcRadius = outerRadius - arcInset
                    val arcDiameter = arcRadius * 2f
                    if (arcDiameter > 8f) {
                        val clampedProg = progress.coerceIn(0f, 1f)
                        val sweepDeg = 270f * clampedProg
                        val topLeft = Offset(center.x - arcRadius, center.y - arcRadius)
                        drawArc(
                            color = Color.White.copy(alpha = 0.14f),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcDiameter, arcDiameter),
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = accent,
                            startAngle = 135f,
                            sweepAngle = sweepDeg,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcDiameter, arcDiameter),
                            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Glowing indicator dot locked directly onto the leading played arc boundary
                        val tipRad = ((135f + sweepDeg) * PI / 180.0)
                        val tipOffset = Offset(
                            x = center.x + (arcRadius * cos(tipRad)).toFloat(),
                            y = center.y + (arcRadius * sin(tipRad)).toFloat()
                        )
                        drawCircle(
                            color = accent.copy(alpha = 0.35f),
                            radius = 7.dp.toPx(),
                            center = tipOffset
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.8.dp.toPx(),
                            center = tipOffset
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    tonalElevation = 6.dp,
                    shadowElevation = 10.dp,
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.38f)),
                    modifier = Modifier.size(152.dp)
                ) {
                    TrackArtwork(
                        track = track,
                        shape = CircleShape,
                        showFolderBadge = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(
                    text = formatDurationMs(maxDurMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun StudioCoverPlayerStage(
    track: CloudTrack,
    isPlaying: Boolean,
    accent: Color
) {
    val coverShape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(256.dp)) {
            val w = size.width
            val h = size.height
            if (w <= 2f || h <= 2f) return@Canvas
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = if (isPlaying) 0.34f else 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(w / 2f, h / 2f),
                    radius = min(w, h) / 2f
                ),
                radius = min(w, h) / 2f
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = coverShape,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.48f),
                            accent.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    )
                ),
                modifier = Modifier.size(214.dp)
            ) {
                TrackArtwork(
                    track = track,
                    shape = coverShape,
                    showFolderBadge = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (track.album.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.32f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = track.album,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VinylTurntablePlayerStage(
    track: CloudTrack,
    isPlaying: Boolean,
    accent: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val spinDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11_000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "vinyl_spin_angle"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(236.dp),
            contentAlignment = Alignment.Center
        ) {
            // Spinning grooved vinyl platter
            Box(
                modifier = Modifier
                    .size(216.dp)
                    .graphicsLayer {
                        rotationZ = if (isPlaying) spinDegrees else 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    if (w <= 2f || h <= 2f) return@Canvas
                    val center = Offset(w / 2f, h / 2f)
                    val r = min(w, h) / 2f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF23252E), Color(0xFF0E1017), Color(0xFF1A1D26)),
                            center = center,
                            radius = r
                        ),
                        radius = r,
                        center = center
                    )

                    // Vinyl specular sheen & concentric grooves
                    for (ring in 1..6) {
                        val grooveR = r * (0.48f + ring * 0.075f)
                        drawCircle(
                            color = Color.White.copy(alpha = if (ring % 2 == 0) 0.10f else 0.05f),
                            radius = grooveR,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                    drawCircle(
                        color = accent.copy(alpha = 0.45f),
                        radius = r - 1.5.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }

                // Center album art record label
                Surface(
                    shape = CircleShape,
                    border = BorderStroke(2.dp, accent.copy(alpha = 0.8f)),
                    modifier = Modifier.size(96.dp)
                ) {
                    TrackArtwork(
                        track = track,
                        shape = CircleShape,
                        showFolderBadge = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Metallic center spindle pin
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F3EE))
                        .border(2.dp, Color(0xFF1B1D24), CircleShape)
                )
            }

            // Precision angled studio tonearm at top-right
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (w <= 2f || h <= 2f) return@Canvas
                val pivot = Offset(w * 0.88f, h * 0.14f)
                val elbow = Offset(w * 0.83f, h * 0.50f)
                val stylus = if (isPlaying) {
                    Offset(w * 0.68f, h * 0.63f)
                } else {
                    Offset(w * 0.80f, h * 0.66f)
                }
                drawCircle(color = Color(0xFF343946), radius = 11.dp.toPx(), center = pivot)
                drawCircle(color = accent, radius = 5.dp.toPx(), center = pivot)
                drawLine(
                    color = Color(0xFFDDE3EE),
                    start = pivot,
                    end = elbow,
                    strokeWidth = 3.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFDDE3EE),
                    start = elbow,
                    end = stylus,
                    strokeWidth = 2.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = accent, radius = 4.dp.toPx(), center = stylus)
            }
        }
    }
}

@Composable
private fun WaveformImmersivePlayerStage(
    track: CloudTrack,
    progress: Float,
    accent: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            tonalElevation = 8.dp,
            shadowElevation = 14.dp,
            border = BorderStroke(1.5.dp, accent.copy(alpha = 0.55f)),
            modifier = Modifier.size(164.dp)
        ) {
            TrackArtwork(
                track = track,
                shape = RoundedCornerShape(26.dp),
                showFolderBadge = false,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            QuranWaveformBarsCanvas(
                progress = progress,
                playedColor = accent,
                unplayedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f),
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(34.dp)
            )
        }
    }
}

@Composable
private fun MinimalFloatingPlayerStage(
    track: CloudTrack,
    progress: Float,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.62f),
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.42f),
                    accent.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.10f)
                )
            )
        ),
        modifier = Modifier
            .width(236.dp)
            .shadow(10.dp, RoundedCornerShape(32.dp), clip = false)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                modifier = Modifier.size(148.dp)
            ) {
                TrackArtwork(
                    track = track,
                    shape = RoundedCornerShape(22.dp),
                    showFolderBadge = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0.04f, 1f))
                            .clip(CircleShape)
                            .background(accent)
                    )
                }
            }
            if (track.album.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = track.album,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RadialSpectrumPlayerStage(
    track: CloudTrack,
    isPlaying: Boolean,
    progress: Float,
    accent: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radial_spectrum_pulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "radial_phase"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(236.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (w <= 2f || h <= 2f) return@Canvas
                val center = Offset(w / 2f, h / 2f)
                val innerR = (min(w, h) / 2f) * 0.67f
                val maxSpike = (min(w, h) / 2f) * 0.25f
                val spokeCount = 40
                val clampedProg = progress.coerceIn(0f, 1f)
                val activeSpokeIndex = (clampedProg * (spokeCount - 1)).toInt()

                for (i in 0 until spokeCount) {
                    val angleDeg = -90f + (360f / spokeCount) * i
                    val angleRad = angleDeg * PI / 180.0
                    val wave = if (isPlaying) {
                        0.38f + 0.62f * abs(sin(i * 0.55f + pulsePhase))
                    } else {
                        0.30f + 0.42f * abs(sin(i * 0.55f + 0.7f))
                    }
                    val len = maxSpike * wave
                    val start = Offset(
                        x = center.x + (innerR * cos(angleRad)).toFloat(),
                        y = center.y + (innerR * sin(angleRad)).toFloat()
                    )
                    val end = Offset(
                        x = center.x + ((innerR + len) * cos(angleRad)).toFloat(),
                        y = center.y + ((innerR + len) * sin(angleRad)).toFloat()
                    )
                    val isPlayedSpoke = i <= activeSpokeIndex && clampedProg > 0f
                    drawLine(
                        color = if (isPlayedSpoke) accent else Color.White.copy(alpha = 0.20f),
                        start = start,
                        end = end,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                //Synchronized progress tip dot on inner ring at exact played angle
                val tipRad = (-90f + 360f * clampedProg) * PI / 180.0
                val tipCenter = Offset(
                    x = center.x + ((innerR - 4.dp.toPx()) * cos(tipRad)).toFloat(),
                    y = center.y + ((innerR - 4.dp.toPx()) * sin(tipRad)).toFloat()
                )
                drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = tipCenter)
            }

            Surface(
                shape = CircleShape,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(2.dp, accent.copy(alpha = 0.65f)),
                modifier = Modifier.size(144.dp)
            ) {
                TrackArtwork(
                    track = track,
                    shape = CircleShape,
                    showFolderBadge = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Miniature visual preview card rendered inside Settings so the user can see each
 * Player Theme's distinct layout as a mini screen card and select it automatically.
 */
@Composable
fun PlayerThemeMiniPreviewCard(
    style: PlayerThemeStyle,
    isSelected: Boolean,
    isArabic: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(18.dp)
    val accent = style.previewPrimary
    val secondary = style.previewSecondary

    Surface(
        shape = cardShape,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
        },
        modifier = modifier
            .clip(cardShape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isSelected) {
                        listOf(accent, secondary)
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
                        )
                    }
                ),
                shape = cardShape
            )
            .clickable(onClick = onClick)
            .testTag("player_theme_card_${style.id}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Miniature Player Screen Mockup (LTR timeline & controls)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF191528),
                                    Color(0xFF0D0B16)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        if (w <= 2f || h <= 2f) return@Canvas

                        // Subtle top glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accent.copy(alpha = 0.30f), Color.Transparent),
                                center = Offset(w * 0.5f, h * 0.2f),
                                radius = min(w, h) * 0.75f
                            ),
                            radius = min(w, h) * 0.75f,
                            center = Offset(w * 0.5f, h * 0.2f)
                        )

                        // Mini top pill handle
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.32f),
                            topLeft = Offset(w * 0.40f, 1.dp.toPx()),
                            size = Size(w * 0.20f, 2.5.dp.toPx()),
                            cornerRadius = CornerRadius(4f, 4f)
                        )

                        val centerStage = Offset(w * 0.5f, h * 0.43f)
                        when (style) {
                            PlayerThemeStyle.AURORA_RING -> {
                                // Side peeking discs + concentric ring
                                drawCircle(
                                    color = secondary.copy(alpha = 0.35f),
                                    radius = h * 0.13f,
                                    center = Offset(w * 0.15f, centerStage.y)
                                )
                                drawCircle(
                                    color = secondary.copy(alpha = 0.35f),
                                    radius = h * 0.13f,
                                    center = Offset(w * 0.85f, centerStage.y)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.22f),
                                    radius = h * 0.24f,
                                    center = centerStage,
                                    style = Stroke(width = 1.6.dp.toPx())
                                )
                                drawArc(
                                    color = accent,
                                    startAngle = 135f,
                                    sweepAngle = 180f,
                                    useCenter = false,
                                    topLeft = Offset(centerStage.x - h * 0.24f, centerStage.y - h * 0.24f),
                                    size = Size(h * 0.48f, h * 0.48f),
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawCircle(
                                    brush = Brush.linearGradient(listOf(accent, secondary)),
                                    radius = h * 0.16f,
                                    center = centerStage
                                )
                            }

                            PlayerThemeStyle.STUDIO_COVER -> {
                                val boxSize = h * 0.44f
                                drawRoundRect(
                                    brush = Brush.linearGradient(listOf(accent, secondary)),
                                    topLeft = Offset(centerStage.x - boxSize / 2f, centerStage.y - boxSize / 2f),
                                    size = Size(boxSize, boxSize),
                                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                                )
                            }

                            PlayerThemeStyle.VINYL_TURNTABLE -> {
                                val r = h * 0.24f
                                drawCircle(color = Color(0xFF1C1F2A), radius = r, center = centerStage)
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.22f),
                                    radius = r * 0.72f,
                                    center = centerStage,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                                drawCircle(color = accent, radius = r * 0.38f, center = centerStage)
                                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = centerStage)
                                drawLine(
                                    color = Color(0xFFE0E6F0),
                                    start = Offset(w * 0.76f, h * 0.18f),
                                    end = Offset(centerStage.x + r * 0.55f, centerStage.y + r * 0.25f),
                                    strokeWidth = 1.8.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }

                            PlayerThemeStyle.WAVEFORM_IMMERSIVE -> {
                                val artSize = h * 0.28f
                                drawRoundRect(
                                    brush = Brush.linearGradient(listOf(accent, secondary)),
                                    topLeft = Offset(centerStage.x - artSize / 2f, h * 0.14f),
                                    size = Size(artSize, artSize),
                                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                                )
                                val bars = 15
                                val barAreaW = w * 0.74f
                                val startX = (w - barAreaW) / 2f
                                val step = barAreaW / bars
                                for (i in 0 until bars) {
                                    val bh = (0.06f + 0.11f * abs(sin(i * 0.8f + 0.5f))) * h
                                    val bx = startX + step * i + step / 2f
                                    val by = h * 0.58f
                                    drawLine(
                                        color = if (i <= 8) accent else Color.White.copy(alpha = 0.22f),
                                        start = Offset(bx, by - bh / 2f),
                                        end = Offset(bx, by + bh / 2f),
                                        strokeWidth = 2.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }

                            PlayerThemeStyle.MINIMAL_FLOATING -> {
                                val cardW = w * 0.58f
                                val cardH = h * 0.46f
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.10f),
                                    topLeft = Offset(centerStage.x - cardW / 2f, centerStage.y - cardH / 2f),
                                    size = Size(cardW, cardH),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )
                                val inner = h * 0.26f
                                drawRoundRect(
                                    brush = Brush.linearGradient(listOf(accent, secondary)),
                                    topLeft = Offset(centerStage.x - inner / 2f, centerStage.y - cardH * 0.34f),
                                    size = Size(inner, inner),
                                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                                )
                            }

                            PlayerThemeStyle.RADIAL_SPECTRUM -> {
                                val coreR = h * 0.15f
                                val spokeBase = h * 0.18f
                                val spokes = 18
                                for (i in 0 until spokes) {
                                    val rad = (-90f + (360f / spokes) * i) * PI / 180.0
                                    val extra = h * (0.04f + 0.06f * abs(sin(i * 0.9f)))
                                    val s = Offset(
                                        centerStage.x + (spokeBase * cos(rad)).toFloat(),
                                        centerStage.y + (spokeBase * sin(rad)).toFloat()
                                    )
                                    val e = Offset(
                                        centerStage.x + ((spokeBase + extra) * cos(rad)).toFloat(),
                                        centerStage.y + ((spokeBase + extra) * sin(rad)).toFloat()
                                    )
                                    drawLine(
                                        color = if (i <= 10) accent else Color.White.copy(alpha = 0.22f),
                                        start = s,
                                        end = e,
                                        strokeWidth = 1.6.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                                drawCircle(
                                    brush = Brush.linearGradient(listOf(accent, secondary)),
                                    radius = coreR,
                                    center = centerStage
                                )
                            }
                        }

                        // Mini synchronized LTR progress line + thumb + 3 mini transport buttons
                        val barY = h * 0.77f
                        val barStart = w * 0.14f
                        val barEnd = w * 0.86f
                        val thumbX = barStart + (barEnd - barStart) * 0.58f
                        drawLine(
                            color = Color.White.copy(alpha = 0.20f),
                            start = Offset(barStart, barY),
                            end = Offset(barEnd, barY),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = accent,
                            start = Offset(barStart, barY),
                            end = Offset(thumbX, barY),
                            strokeWidth = 2.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.8.dp.toPx(),
                            center = Offset(thumbX, barY)
                        )

                        val ctrlY = h * 0.91f
                        drawCircle(color = Color.White.copy(alpha = 0.55f), radius = 2.2.dp.toPx(), center = Offset(w * 0.36f, ctrlY))
                        drawCircle(color = accent, radius = 4.2.dp.toPx(), center = Offset(w * 0.50f, ctrlY))
                        drawCircle(color = Color.White.copy(alpha = 0.55f), radius = 2.2.dp.toPx(), center = Offset(w * 0.64f, ctrlY))
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(13.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isArabic) style.labelAr else style.labelEn,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isArabic) style.descAr else style.descEn,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
