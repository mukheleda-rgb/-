package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.audio.PlaybackState
import com.example.data.model.CloudTrack
import com.example.ui.theme.LocalNaghamColors
import com.example.ui.theme.PlayerVisualEffect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun MiniEqualizerBars(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val s1: androidx.compose.runtime.State<Float>?
    val s2: androidx.compose.runtime.State<Float>?
    val s3: androidx.compose.runtime.State<Float>?

    if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "mini_eq")
        s1 = transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
            label = "b1"
        )
        s2 = transition.animateFloat(
            initialValue = 0.9f,
            targetValue = 0.2f,
            animationSpec = infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse),
            label = "b2"
        )
        s3 = transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(tween(510, easing = LinearEasing), RepeatMode.Reverse),
            label = "b3"
        )
    } else {
        s1 = null
        s2 = null
        s3 = null
    }

    Canvas(
        modifier = modifier
            .width(14.dp)
            .height(18.dp)
    ) {
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return@Canvas

        val f1 = s1?.value ?: 0.30f
        val f2 = s2?.value ?: 0.50f
        val f3 = s3?.value ?: 0.35f

        val barW = 3.dp.toPx()
        val gap = 2.5.dp.toPx()
        val corner = CornerRadius(barW * 0.5f, barW * 0.5f)
        val fractions = floatArrayOf(f1, f2, f3)

        for (i in 0 until 3) {
            val barH = (h * fractions[i]).coerceIn(barW, h)
            val left = i * (barW + gap)
            drawRoundRect(
                color = color,
                topLeft = Offset(left, h - barH),
                size = Size(barW, barH),
                cornerRadius = corner
            )
        }
    }
}

@Composable
fun LiquidSoundwaveBackdrop(
    isPlaying: Boolean,
    accentColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "liquid_wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "wave_phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return@Canvas
        val ampBase = if (isPlaying) h * 0.065f else h * 0.015f

        for (layer in 0..2) {
            val path = Path()
            val yCenter = h * (0.42f + layer * 0.14f)
            val freq = 1.6f + layer * 0.7f
            val layerPhase = if (isPlaying) phase * (1f + layer * 0.35f) else layer * 1.1f
            val amp = ampBase * (1f - layer * 0.22f)

            path.moveTo(0f, yCenter)
            val steps = 48
            for (i in 0..steps) {
                val x = (w / steps) * i
                val angle = (i.toFloat() / steps) * freq * (2f * PI).toFloat() + layerPhase
                val y = yCenter + sin(angle) * amp
                path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = if (layer % 2 == 0) {
                    accentColor.copy(alpha = if (isPlaying) 0.22f else 0.08f)
                } else {
                    secondaryColor.copy(alpha = if (isPlaying) 0.16f else 0.06f)
                },
                style = Stroke(width = (3.5f - layer * 0.7f).dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun PlayerArtworkWithEffects(
    track: CloudTrack,
    playbackState: PlaybackState,
    visualEffect: PlayerVisualEffect,
    modifier: Modifier = Modifier
) {
    val extColors = LocalNaghamColors.current
    val accent = if (track.quranAudio) extColors.quranAccent else MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val isPlaying = playbackState.isPlaying

    val transition = rememberInfiniteTransition(label = "player_fx")
    val continuousSpin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_spin"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "aura_pulse"
    )

    val lastAngle = remember(track.id) { object { var deg = 0f } }
    val activeSpin = isPlaying && !track.quranAudio && visualEffect == PlayerVisualEffect.VINYL_AURA
    val rotationDeg = if (activeSpin) {
        lastAngle.deg = continuousSpin
        continuousSpin
    } else {
        lastAngle.deg
    }

    val bassFactor = (playbackState.bassBoostStrength / 1000f).coerceIn(0f, 1f)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Background Aura / Rings / Waves
        Canvas(
            modifier = Modifier
                .size(268.dp)
        ) {
            if (size.minDimension <= 1f) return@Canvas
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.36f

            if (track.quranAudio) {
                // Respectful calm breathing glow for Holy Quran
                val calmBreath = if (isPlaying) (sin(pulse) * 0.5f + 0.5f) else 0.2f
                val safeRadius = (size.minDimension * 0.50f).coerceAtLeast(1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.26f + calmBreath * 0.12f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = safeRadius
                    ),
                    radius = safeRadius,
                    center = center
                )
            } else {
                when (visualEffect) {
                    PlayerVisualEffect.VINYL_AURA -> {
                        for (ring in 0..2) {
                            val wave = if (isPlaying) {
                                (sin(pulse + ring * 1.3f) * 0.5f + 0.5f)
                            } else 0.15f
                            val ringRadius = baseRadius + (18f + ring * 18f + wave * (14f + bassFactor * 12f)).dp.toPx()
                            drawCircle(
                                color = accent.copy(alpha = (0.28f - ring * 0.08f).coerceAtLeast(0.05f)),
                                radius = ringRadius,
                                center = center,
                                style = Stroke(width = (2.4f - ring * 0.5f).dp.toPx())
                            )
                        }
                    }

                    PlayerVisualEffect.SPECTRUM_BARS -> {
                        val barCount = 32
                        val innerR = baseRadius + 10.dp.toPx()
                        for (i in 0 until barCount) {
                            val angleRad = (i.toFloat() / barCount) * (2f * PI).toFloat()
                            val bandIdx = (i * 5 / barCount).coerceIn(0, 4)
                            val eqBoost = ((playbackState.eqBandsMb.getOrElse(bandIdx) { 0L } + 1500f) / 3000f)
                            val dynamicMag = if (isPlaying) {
                                abs(sin(pulse * 1.8f + i * 0.45f)) * (0.45f + eqBoost * 0.55f + bassFactor * 0.25f)
                            } else {
                                0.14f
                            }
                            val barLen = (8f + dynamicMag * 26f).dp.toPx()
                            val sx = center.x + kotlin.math.cos(angleRad) * innerR
                            val sy = center.y + sin(angleRad) * innerR
                            val ex = center.x + kotlin.math.cos(angleRad) * (innerR + barLen)
                            val ey = center.y + sin(angleRad) * (innerR + barLen)
                            drawLine(
                                color = if (i % 2 == 0) accent else secondary,
                                start = Offset(sx, sy),
                                end = Offset(ex, ey),
                                strokeWidth = 3.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    PlayerVisualEffect.LIQUID_WAVE -> {
                        val glowScale = if (isPlaying) 0.48f + sin(pulse) * 0.04f else 0.42f
                        val safeRadius = (size.minDimension * glowScale).coerceAtLeast(1f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accent.copy(alpha = 0.34f),
                                    secondary.copy(alpha = 0.14f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = safeRadius
                            ),
                            radius = safeRadius,
                            center = center
                        )
                    }
                }
            }
        }

        // Centerpiece Album or Folder Artwork
        val useCircularDisc = !track.quranAudio && visualEffect == PlayerVisualEffect.VINYL_AURA
        val artShape = if (useCircularDisc) CircleShape else RoundedCornerShape(28.dp)

        Box(
            modifier = Modifier
                .size(204.dp)
                .graphicsLayer {
                    rotationZ = if (useCircularDisc) rotationDeg else 0f
                },
            contentAlignment = Alignment.Center
        ) {
            TrackArtwork(
                track = track,
                shape = artShape,
                showFolderBadge = !useCircularDisc,
                modifier = Modifier.fillMaxSize()
            )

            // Vinyl center spindle ring when in Rotating Vinyl mode
            if (useCircularDisc) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                        .border(2.dp, accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                }
            }
        }
    }
}

@Composable
fun HorizontalSpectrumBarStrip(
    isPlaying: Boolean,
    eqBandsMb: List<Long>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "horiz_spectrum")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "spectrum_phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        val count = 28
        val spacing = 4.dp.toPx()
        val totalSpacing = spacing * (count - 1)
        val barWidth = ((size.width - totalSpacing) / count).coerceAtLeast(2f)
        val minBarH = 4.dp.toPx().coerceAtMost(size.height)

        for (i in 0 until count) {
            val bandIdx = (i * 5 / count).coerceIn(0, 4)
            val eqNorm = ((eqBandsMb.getOrElse(bandIdx) { 0L } + 1500f) / 3000f).coerceIn(0.15f, 1f)
            val wave = if (isPlaying) {
                (abs(sin(phase + i * 0.38f)) * 0.72f + 0.22f) * (0.55f + eqNorm * 0.45f)
            } else {
                0.18f
            }
            val barH = (size.height * wave).coerceIn(minBarH, size.height)
            val x = i * (barWidth + spacing)
            val y = (size.height - barH) / 2f
            drawRoundRect(
                color = accentColor.copy(alpha = 0.45f + wave * 0.50f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
