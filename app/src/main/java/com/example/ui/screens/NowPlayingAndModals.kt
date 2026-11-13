package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.audio.PlaybackState
import com.example.audio.RepeatMode
import com.example.data.model.CloudTrack
import com.example.data.model.EqualizerPreset
import com.example.data.model.LyricLine
import com.example.data.remote.LyricsService
import com.example.ui.components.TrackArtwork
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.AppDesignStyle
import com.example.ui.theme.LocalNaghamColors
import com.example.ui.theme.LocalNaghamDesign
import com.example.ui.theme.NaghamThemedBackdrop
import com.example.ui.theme.PlayerThemeStyle
import com.example.ui.theme.PlayerVisualEffect
import com.example.ui.theme.naghamDesignBorder
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun MiniPlayerBar(
    playbackState: PlaybackState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSwipePreviousTrack: () -> Unit = {},
    onOpenEqualizer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    val designTokens = LocalNaghamDesign.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentOnNext by rememberUpdatedState(onSkipNext)
    val currentOnPrev by rememberUpdatedState(onSwipePreviousTrack)
    val accent = if (track.quranAudio) Color(0xFF5CE65C) else MaterialTheme.colorScheme.primary
    val progress = if (playbackState.durationMs > 0L) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val defaultMiniWidthPx = with(density) { 340.dp.toPx() }
    var miniWidthPx by remember { mutableFloatStateOf(defaultMiniWidthPx) }
    val miniOffsetX = remember { Animatable(0f) }
    val miniThresholdPx = with(density) { 48.dp.toPx() }
    val miniShape = if (designTokens.style == AppDesignStyle.GLASS) {
        RoundedCornerShape(26.dp)
    } else {
        designTokens.cardShape
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .onSizeChanged { sz ->
                if (sz.width > 24) miniWidthPx = sz.width.toFloat()
            }
            .graphicsLayer {
                val w = miniWidthPx.coerceAtLeast(120f)
                val tx = miniOffsetX.value
                translationX = tx
                alpha = (1f - (abs(tx) / w) * 0.45f).coerceIn(0.25f, 1f)
            }
            .naghamDesignBorder(
                accentColor = accent,
                shapeOverride = miniShape,
                isHighlighted = true
            )
            .clip(miniShape)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val w = miniWidthPx.coerceAtLeast(160f)
                        val threshold = min(miniThresholdPx, w * 0.20f)
                        val tx = miniOffsetX.value
                        scope.launch {
                            when {
                                tx < -threshold -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    miniOffsetX.animateTo(-w, tween(165, easing = FastOutSlowInEasing))
                                    currentOnNext()
                                    miniOffsetX.snapTo(w * 0.85f)
                                    miniOffsetX.animateTo(0f, tween(185, easing = FastOutSlowInEasing))
                                }
                                tx > threshold -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    miniOffsetX.animateTo(w, tween(165, easing = FastOutSlowInEasing))
                                    currentOnPrev()
                                    miniOffsetX.snapTo(-w * 0.85f)
                                    miniOffsetX.animateTo(0f, tween(185, easing = FastOutSlowInEasing))
                                }
                                else -> {
                                    miniOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            miniOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            miniOffsetX.snapTo(miniOffsetX.value + dragAmount)
                        }
                    }
                )
            }
            .clickable(onClick = onExpand)
            .testTag("mini_player_bar"),
        colors = CardDefaults.cardColors(
            containerColor = if (track.quranAudio) {
                Color(0xFF0C261E).copy(alpha = 0.90f)
            } else if (designTokens.style == AppDesignStyle.GLASS) {
                Color(0xFF141122).copy(alpha = 0.84f)
            } else {
                designTokens.cardContainerColor(
                    base = MaterialTheme.colorScheme.surfaceContainerHigh,
                    isHighlighted = true
                )
            }
        ),
        shape = miniShape
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle frosted capsule horizontal progress fill tint synchronized Left-to-Right
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        accent.copy(alpha = 0.24f),
                                        accent.copy(alpha = 0.10f)
                                    )
                                )
                            )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackArtwork(
                    track = track,
                    shape = RoundedCornerShape(14.dp),
                    showFolderBadge = false,
                    modifier = Modifier.size(46.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (track.quranAudio) Color(0xFFEAFBF3) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (track.quranAudio) {
                            Color(0xFFA7D7C5)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onOpenEqualizer != null) {
                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier.testTag("mini_player_eq_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = LocalAppStrings.current.navEqualizer,
                            tint = if (track.quranAudio) Color(0xFF5CE65C) else accent
                        )
                    }
                }
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.testTag("mini_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play or Pause",
                        tint = if (track.quranAudio) Color(0xFF5CE65C) else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.testTag("mini_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = if (track.quranAudio) Color(0xFFEAFBF3) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun FullNowPlayingOverlay(
    playbackState: PlaybackState,
    playerThemeStyle: PlayerThemeStyle = PlayerThemeStyle.AURORA_RING,
    visualEffect: PlayerVisualEffect = PlayerVisualEffect.VINYL_AURA,
    onSelectVisualEffect: (PlayerVisualEffect) -> Unit = {},
    onPeakSwipeTranslationObserved: ((translationPx: Float, stageWidthPx: Float) -> Unit)? = null,
    onProgressSyncObserved: ((thumbX: Float, playedBoundaryX: Float, trackWidthPx: Float) -> Unit)? = null,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSwipePreviousTrack: () -> Unit = onSkipPrevious,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onEditTrack: (CloudTrack) -> Unit,
    onToggleFavorite: (CloudTrack) -> Unit = {},
    onAddToPlaylistRequest: (CloudTrack) -> Unit = {},
    onSelectQueueTrack: (CloudTrack) -> Unit = {},
    onSelectPreset: (EqualizerPreset) -> Unit,
    onBandChanged: (Int, Long) -> Unit,
    onBassChanged: (Long) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSyncEqToCloud: () -> Unit,
    onToggleEqEnabled: (Boolean) -> Unit = {},
    onResetEq: () -> Unit = {},
    onOpenFullEqualizer: (() -> Unit)? = null
) {
    val track = playbackState.currentTrack ?: return
    // 0 = Artwork / Medallion View, 1 = Synced Lyrics, 2 = Inline Equalizer
    var activeSection by rememberSaveable { mutableIntStateOf(0) }

    BackHandler {
        if (activeSection != 0) {
            activeSection = 0
        } else {
            onCollapse()
        }
    }

    if (track.quranAudio) {
        QuranSanctuaryPlayerContent(
            track = track,
            playbackState = playbackState,
            activeSection = activeSection,
            onChangeSection = { activeSection = it },
            onPeakSwipeTranslationObserved = onPeakSwipeTranslationObserved,
            onProgressSyncObserved = onProgressSyncObserved,
            onCollapse = onCollapse,
            onTogglePlayPause = onTogglePlayPause,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onSwipePreviousTrack = onSwipePreviousTrack,
            onSeekTo = onSeekTo,
            onCycleRepeat = onCycleRepeat,
            onEditTrack = onEditTrack,
            onSelectQueueTrack = onSelectQueueTrack,
            onSelectPreset = onSelectPreset,
            onBandChanged = onBandChanged,
            onBassChanged = onBassChanged,
            onSpeedChanged = onSpeedChanged,
            onSyncEqToCloud = onSyncEqToCloud,
            onToggleEqEnabled = onToggleEqEnabled,
            onResetEq = onResetEq,
            onOpenFullEqualizer = onOpenFullEqualizer
        )
    } else {
        FrostedStudioMusicPlayerContent(
            track = track,
            playbackState = playbackState,
            playerThemeStyle = playerThemeStyle,
            activeSection = activeSection,
            onChangeSection = { activeSection = it },
            onPeakSwipeTranslationObserved = onPeakSwipeTranslationObserved,
            onProgressSyncObserved = onProgressSyncObserved,
            onCollapse = onCollapse,
            onTogglePlayPause = onTogglePlayPause,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onSwipePreviousTrack = onSwipePreviousTrack,
            onSeekTo = onSeekTo,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat,
            onEditTrack = onEditTrack,
            onToggleFavorite = onToggleFavorite,
            onAddToPlaylistRequest = onAddToPlaylistRequest,
            onSelectQueueTrack = onSelectQueueTrack,
            onSelectPreset = onSelectPreset,
            onBandChanged = onBandChanged,
            onBassChanged = onBassChanged,
            onSpeedChanged = onSpeedChanged,
            onSyncEqToCloud = onSyncEqToCloud,
            onToggleEqEnabled = onToggleEqEnabled,
            onResetEq = onResetEq,
            onOpenFullEqualizer = onOpenFullEqualizer
        )
    }
}

@Composable
private fun FrostedStudioMusicPlayerContent(
    track: CloudTrack,
    playbackState: PlaybackState,
    playerThemeStyle: PlayerThemeStyle,
    activeSection: Int,
    onChangeSection: (Int) -> Unit,
    onPeakSwipeTranslationObserved: ((translationPx: Float, stageWidthPx: Float) -> Unit)?,
    onProgressSyncObserved: ((thumbX: Float, playedBoundaryX: Float, trackWidthPx: Float) -> Unit)?,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSwipePreviousTrack: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onEditTrack: (CloudTrack) -> Unit,
    onToggleFavorite: (CloudTrack) -> Unit,
    onAddToPlaylistRequest: (CloudTrack) -> Unit,
    onSelectQueueTrack: (CloudTrack) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onBandChanged: (Int, Long) -> Unit,
    onBassChanged: (Long) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSyncEqToCloud: () -> Unit,
    onToggleEqEnabled: (Boolean) -> Unit,
    onResetEq: () -> Unit,
    onOpenFullEqualizer: (() -> Unit)?
) {
    val strings = LocalAppStrings.current
    val accent = MaterialTheme.colorScheme.primary

    val parsedLyrics = remember(track.lyrics) { LyricsService.parseLyrics(track.lyrics) }
    val activeLineIndex = remember(parsedLyrics, playbackState.currentPositionMs) {
        val pos = playbackState.currentPositionMs
        parsedLyrics.indexOfLast { line ->
            line.timestampMs != null && line.timestampMs <= pos
        }
    }

    val maxDur = playbackState.durationMs.coerceAtLeast(1000L)
    val progress = (playbackState.currentPositionMs.toFloat() / maxDur.toFloat()).coerceIn(0f, 1f)

    val queue = playbackState.queue
    val currentIdx = remember(queue, track.id) {
        queue.indexOfFirst { it.id == track.id }
    }
    val prevTrack = remember(queue, currentIdx) {
        if (queue.size > 1 && currentIdx >= 0) {
            queue[(currentIdx - 1 + queue.size) % queue.size]
        } else null
    }
    val nextTrack = remember(queue, currentIdx) {
        if (queue.size > 1 && currentIdx >= 0) {
            queue[(currentIdx + 1) % queue.size]
        } else null
    }
    val upcomingQueue = remember(queue, currentIdx, track.id) {
        if (queue.size <= 1) {
            emptyList()
        } else if (currentIdx >= 0) {
            (queue.drop(currentIdx + 1) + queue.take(currentIdx)).take(6)
        } else {
            queue.filter { it.id != track.id }.take(6)
        }
    }

    NaghamThemedBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .testTag("full_player_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Top Bar (Image 1 Left: Chevron Down • Frosted Pill Indicator • Edit/Options)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.testTag("collapse_player_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close Player",
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.nowPlayingTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onEditTrack(track) },
                    modifier = Modifier.testTag("player_edit_track_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = strings.editTrackTitle,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Track Title & Artist + Quick Action Icons Row (Image 1 & Image 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onToggleFavorite(track) },
                        modifier = Modifier.testTag("player_fav_button")
                    ) {
                        Icon(
                            imageVector = if (track.favorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = strings.favoritesChip,
                            tint = if (track.favorite) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onChangeSection(if (activeSection == 1) 0 else 1) },
                        modifier = Modifier.testTag("player_tab_lyrics")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = strings.playerSectionLyrics,
                            tint = if (activeSection == 1) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onAddToPlaylistRequest(track) },
                        modifier = Modifier.testTag("player_add_playlist_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = strings.actionAddToPlaylist,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onChangeSection(if (activeSection == 2) 0 else 2) },
                        modifier = Modifier.testTag("player_open_eq_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = strings.playerSectionEqualizer,
                            tint = if (activeSection == 2) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Animated Centerpiece Section (Artwork Ring <-> Synced Lyrics <-> Equalizer)
            AnimatedContent(
                targetState = activeSection,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(tween(240)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(220)))
                        .togetherWith(
                            slideOutHorizontally(tween(200)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(180))
                        )
                },
                label = "player_center_section_anim",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { section ->
                when (section) {
                    0 -> {
                        SwipeableTrackStage(
                            onSwipeLeftToNext = onSkipNext,
                            onSwipeRightToPrevious = onSwipePreviousTrack,
                            testTag = "player_artwork_swipe_area",
                            onPeakTranslationObserved = onPeakSwipeTranslationObserved,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            PlayerThemeHeroStage(
                                style = playerThemeStyle,
                                track = track,
                                prevTrack = prevTrack,
                                nextTrack = nextTrack,
                                isPlaying = playbackState.isPlaying,
                                progress = progress,
                                maxDurMs = maxDur,
                                accent = accent
                            )
                        }
                    }

                    1 -> {
                        SyncedLyricsCard(
                            parsedLyrics = parsedLyrics,
                            activeLineIndex = activeLineIndex,
                            accent = accent,
                            onSeekTo = onSeekTo
                        )
                    }

                    else -> {
                        InlineEqualizerPanel(
                            playbackState = playbackState,
                            accentColor = accent,
                            onToggleEnabled = onToggleEqEnabled,
                            onResetFlat = onResetEq,
                            onSelectPreset = onSelectPreset,
                            onBandChanged = onBandChanged,
                            onBassChanged = onBassChanged,
                            onSpeedChanged = onSpeedChanged,
                            onSyncToCloud = onSyncEqToCloud,
                            onOpenFullEqualizer = onOpenFullEqualizer
                        )
                    }
                }
            }

            if (playbackState.playbackNotice != null) {
                Text(
                    text = playbackState.playbackNotice,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Synchronized LTR Seek Scrubber & Timestamps: progress thumb and played boundary share the exact same X
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_progress_timeline_ltr")
                ) {
                    SynchronizedPlayerSeekBar(
                        currentPositionMs = playbackState.currentPositionMs.coerceIn(0L, maxDur),
                        durationMs = maxDur,
                        playedColor = accent,
                        unplayedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                        thumbColor = Color.White,
                        waveformMode = false,
                        onSeekTo = onSeekTo,
                        onProgressSyncObserved = onProgressSyncObserved,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .testTag("player_seek_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDurationMs(playbackState.currentPositionMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDurationMs(maxDur),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Minimalist Transport Controls aligned Left-to-Right with timeline direction
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!playbackState.isQuranMode || playbackState.isMixedPlaylistQueue) {
                        IconButton(onClick = onToggleShuffle) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (playbackState.shuffleEnabled) accent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onSkipPrevious) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(accent, accent.copy(alpha = 0.80f))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            .clickable(onClick = onTogglePlayPause)
                            .testTag("full_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play or Pause",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    IconButton(onClick = onSkipNext) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    IconButton(onClick = onCycleRepeat) {
                        Icon(
                            imageVector = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (playbackState.repeatMode != RepeatMode.OFF) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Compact Up-Next Queue List below Transport Controls (Image 2)
            if (activeSection == 0 && upcomingQueue.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.upNextQueueTitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = accent.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 126.dp)
                        .testTag("player_up_next_list"),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(upcomingQueue, key = { it.id }) { qTrack ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectQueueTrack(qTrack) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("player_queue_item_${qTrack.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = qTrack.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = qTrack.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuranSanctuaryPlayerContent(
    track: CloudTrack,
    playbackState: PlaybackState,
    activeSection: Int,
    onChangeSection: (Int) -> Unit,
    onPeakSwipeTranslationObserved: ((translationPx: Float, stageWidthPx: Float) -> Unit)?,
    onProgressSyncObserved: ((thumbX: Float, playedBoundaryX: Float, trackWidthPx: Float) -> Unit)?,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSwipePreviousTrack: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onCycleRepeat: () -> Unit,
    onEditTrack: (CloudTrack) -> Unit,
    onSelectQueueTrack: (CloudTrack) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onBandChanged: (Int, Long) -> Unit,
    onBassChanged: (Long) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSyncEqToCloud: () -> Unit,
    onToggleEqEnabled: (Boolean) -> Unit,
    onResetEq: () -> Unit,
    onOpenFullEqualizer: (() -> Unit)?
) {
    val strings = LocalAppStrings.current

    val emeraldLime = Color(0xFF5CE65C)
    val deepSanctuaryTop = Color(0xFF071A15)
    val deepSanctuaryMid = Color(0xFF0C261E)
    val deepSanctuaryBottom = Color(0xFF05120E)

    val parsedLyrics = remember(track.lyrics) { LyricsService.parseLyrics(track.lyrics) }
    val activeLineIndex = remember(parsedLyrics, playbackState.currentPositionMs) {
        val pos = playbackState.currentPositionMs
        parsedLyrics.indexOfLast { line ->
            line.timestampMs != null && line.timestampMs <= pos
        }
    }
    val activeVersePreview = remember(parsedLyrics, activeLineIndex, track.album) {
        parsedLyrics.getOrNull(activeLineIndex.coerceAtLeast(0))?.text
            ?: track.album.takeIf { it.isNotBlank() }.orEmpty()
    }

    val maxDur = playbackState.durationMs.coerceAtLeast(1000L)
    val progress = (playbackState.currentPositionMs.toFloat() / maxDur.toFloat()).coerceIn(0f, 1f)

    val recitations = remember(playbackState.queue, track) {
        playbackState.queue.ifEmpty { listOf(track) }.distinctBy { it.id }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(deepSanctuaryTop, deepSanctuaryMid, deepSanctuaryBottom)
                )
            )
            .drawBehind {
                drawMosqueSilhouette(Color(0xFF153D30).copy(alpha = 0.45f))
            }
            .testTag("full_player_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Top Sanctuary Header (Image 3 Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onCollapse,
                    shape = CircleShape,
                    color = Color(0xFF113328),
                    border = BorderStroke(1.dp, emeraldLime.copy(alpha = 0.30f)),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("collapse_player_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Close Player",
                            tint = Color.White
                        )
                    }
                }

                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFEAFBF3),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                    textAlign = TextAlign.Center
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onChangeSection(if (activeSection == 1) 0 else 1) },
                        modifier = Modifier.testTag("player_tab_lyrics")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = strings.playerSectionLyrics,
                            tint = if (activeSection == 1) emeraldLime else Color(0xFFA7D7C5)
                        )
                    }
                    IconButton(
                        onClick = { onChangeSection(if (activeSection == 2) 0 else 2) },
                        modifier = Modifier.testTag("player_open_eq_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = strings.playerSectionEqualizer,
                            tint = if (activeSection == 2) emeraldLime else Color(0xFFA7D7C5)
                        )
                    }
                    IconButton(
                        onClick = { onEditTrack(track) },
                        modifier = Modifier.testTag("player_edit_track_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = strings.editTrackTitle,
                            tint = Color(0xFFA7D7C5)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = activeSection,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(tween(240)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(220)))
                        .togetherWith(
                            slideOutHorizontally(tween(200)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(180))
                        )
                },
                label = "quran_player_section_anim",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { section ->
                when (section) {
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Full-width Swipeable Stage for Circular Quran Medallion (glides to screen edge in swipe direction)
                            SwipeableTrackStage(
                                onSwipeLeftToNext = onSkipNext,
                                onSwipeRightToPrevious = onSwipePreviousTrack,
                                testTag = "player_artwork_swipe_area",
                                onPeakTranslationObserved = onPeakSwipeTranslationObserved,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier.size(220.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val w = size.width
                                        val h = size.height
                                        if (w <= 2f || h <= 2f) return@Canvas
                                        val center = Offset(w / 2f, h / 2f)
                                        val radius = (minOf(w, h) / 2f) - 6.dp.toPx()

                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    emeraldLime.copy(alpha = 0.24f),
                                                    Color(0xFF0E3428).copy(alpha = 0.55f),
                                                    Color.Transparent
                                                ),
                                                center = center,
                                                radius = radius * 1.06f
                                            ),
                                            radius = radius * 1.06f,
                                            center = center
                                        )

                                        val arcDiameter = radius * 2f
                                        if (arcDiameter > 8f) {
                                            val clampedProg = progress.coerceIn(0f, 1f)
                                            val sweepDeg = 360f * clampedProg
                                            val topLeft = Offset(center.x - radius, center.y - radius)
                                            drawArc(
                                                color = emeraldLime.copy(alpha = 0.18f),
                                                startAngle = -90f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                topLeft = topLeft,
                                                size = Size(arcDiameter, arcDiameter),
                                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                            drawArc(
                                                color = emeraldLime,
                                                startAngle = -90f,
                                                sweepAngle = sweepDeg,
                                                useCenter = false,
                                                topLeft = topLeft,
                                                size = Size(arcDiameter, arcDiameter),
                                                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                            val tipRad = ((-90f + sweepDeg) * PI / 180.0)
                                            val tipCenter = Offset(
                                                x = center.x + (radius * cos(tipRad)).toFloat(),
                                                y = center.y + (radius * sin(tipRad)).toFloat()
                                            )
                                            drawCircle(
                                                color = emeraldLime.copy(alpha = 0.35f),
                                                radius = 6.5.dp.toPx(),
                                                center = tipCenter
                                            )
                                            drawCircle(
                                                color = Color.White,
                                                radius = 3.6.dp.toPx(),
                                                center = tipCenter
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        border = BorderStroke(2.dp, emeraldLime.copy(alpha = 0.45f)),
                                        modifier = Modifier.size(182.dp)
                                    ) {
                                        TrackArtwork(
                                            track = track,
                                            shape = CircleShape,
                                            showFolderBadge = false,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Floating green Play/Pause badge on bottom-right edge (Image 3 Right)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .offset(x = (-10).dp, y = (-10).dp)
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(emeraldLime)
                                            .border(2.dp, deepSanctuaryMid, CircleShape)
                                            .clickable(onClick = onTogglePlayPause)
                                            .testTag("quran_medallion_play_badge"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play or Pause",
                                            tint = Color(0xFF04210E),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Surah Title, Reciter & Verse Preview
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFA7D7C5),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (activeVersePreview.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = activeVersePreview,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFD5F5E7),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Circular Sanctuary Transport Buttons aligned Left-to-Right with timeline
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = onSkipPrevious,
                                        shape = CircleShape,
                                        color = Color(0xFF13392D),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.SkipPrevious,
                                                contentDescription = "Previous",
                                                tint = emeraldLime
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clip(CircleShape)
                                            .background(emeraldLime)
                                            .clickable(onClick = onTogglePlayPause)
                                            .testTag("full_play_pause_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play or Pause",
                                            tint = Color(0xFF04210E),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Surface(
                                        onClick = onSkipNext,
                                        shape = CircleShape,
                                        color = Color(0xFF13392D),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.SkipNext,
                                                contentDescription = "Next",
                                                tint = emeraldLime
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = onCycleRepeat,
                                        shape = CircleShape,
                                        color = if (playbackState.repeatMode != RepeatMode.OFF) {
                                            emeraldLime.copy(alpha = 0.24f)
                                        } else {
                                            Color(0xFF13392D)
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (playbackState.repeatMode == RepeatMode.ONE) {
                                                    Icons.Default.RepeatOne
                                                } else {
                                                    Icons.Default.Repeat
                                                },
                                                contentDescription = "Repeat",
                                                tint = if (playbackState.repeatMode != RepeatMode.OFF) {
                                                    emeraldLime
                                                } else {
                                                    Color(0xFFA7D7C5)
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Interactive Waveform Bar Scrubber + Timestamps in synchronized LTR so thumb & played bars share exact boundaryX
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("player_progress_timeline_ltr"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatDurationMs(playbackState.currentPositionMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA7D7C5)
                                    )
                                    SynchronizedPlayerSeekBar(
                                        currentPositionMs = playbackState.currentPositionMs.coerceIn(0L, maxDur),
                                        durationMs = maxDur,
                                        playedColor = Color(0xFFFF7A45),
                                        unplayedColor = Color(0xFF285244),
                                        thumbColor = Color(0xFFFF7A45),
                                        waveformMode = true,
                                        onSeekTo = onSeekTo,
                                        onProgressSyncObserved = onProgressSyncObserved,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .padding(horizontal = 8.dp)
                                            .testTag("player_seek_slider")
                                    )
                                    Text(
                                        text = formatDurationMs(maxDur),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA7D7C5)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Horizontal Recitations / Reciters Strip (Image 3 Right bottom)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = strings.recitersSectionTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFEAFBF3)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(end = 8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quran_reciters_row")
                                ) {
                                    items(recitations, key = { it.id }) { item ->
                                        val isCurrentItem = item.id == track.id
                                        Surface(
                                            onClick = { onSelectQueueTrack(item) },
                                            shape = RoundedCornerShape(18.dp),
                                            color = if (isCurrentItem) {
                                                Color(0xFF143D30)
                                            } else {
                                                Color(0xFF0E2B22)
                                            },
                                            border = BorderStroke(
                                                width = if (isCurrentItem) 1.5.dp else 1.dp,
                                                color = if (isCurrentItem) {
                                                    emeraldLime.copy(alpha = 0.85f)
                                                } else {
                                                    Color.White.copy(alpha = 0.12f)
                                                }
                                            ),
                                            modifier = Modifier
                                                .width(122.dp)
                                                .testTag("quran_reciter_card_${item.id}")
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(modifier = Modifier.size(48.dp)) {
                                                    TrackArtwork(
                                                        track = item,
                                                        shape = CircleShape,
                                                        showFolderBadge = false,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    if (isCurrentItem) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = null,
                                                            tint = emeraldLime,
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.BottomEnd)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = item.artist.ifBlank { item.title },
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = item.title,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFA7D7C5),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        SyncedLyricsCard(
                            parsedLyrics = parsedLyrics,
                            activeLineIndex = activeLineIndex,
                            accent = emeraldLime,
                            onSeekTo = onSeekTo
                        )
                    }

                    else -> {
                        InlineEqualizerPanel(
                            playbackState = playbackState,
                            accentColor = emeraldLime,
                            onToggleEnabled = onToggleEqEnabled,
                            onResetFlat = onResetEq,
                            onSelectPreset = onSelectPreset,
                            onBandChanged = onBandChanged,
                            onBassChanged = onBassChanged,
                            onSpeedChanged = onSpeedChanged,
                            onSyncToCloud = onSyncEqToCloud,
                            onOpenFullEqualizer = onOpenFullEqualizer
                        )
                    }
                }
            }
        }
    }
}

internal data class SeekBarSyncGeometry(
    val trackStartX: Float,
    val trackEndX: Float,
    val usableWidth: Float,
    val boundaryX: Float
)

internal fun computeSeekBarSyncGeometry(
    widthPx: Float,
    thumbRadiusPx: Float,
    fraction: Float
): SeekBarSyncGeometry {
    val trackStartX = thumbRadiusPx.coerceAtMost(widthPx * 0.25f)
    val trackEndX = (widthPx - trackStartX).coerceAtLeast(trackStartX + 1f)
    val usableWidth = trackEndX - trackStartX
    val clampedFrac = fraction.coerceIn(0f, 1f)
    val boundaryX = trackStartX + usableWidth * clampedFrac
    return SeekBarSyncGeometry(
        trackStartX = trackStartX,
        trackEndX = trackEndX,
        usableWidth = usableWidth,
        boundaryX = boundaryX
    )
}

/**
 * Unified Left-to-Right player scrubber where the progress indicator thumb (`مؤشر التقدم`)
 * and the played/read boundary (`الحد المقروء`) share a single computed `boundaryX` coordinate
 * inside one Canvas + gesture detector — guaranteeing 0.0px divergence in both Arabic (RTL) and English (LTR).
 */
@Composable
internal fun SynchronizedPlayerSeekBar(
    currentPositionMs: Long,
    durationMs: Long,
    playedColor: Color,
    unplayedColor: Color,
    thumbColor: Color,
    waveformMode: Boolean,
    onSeekTo: (Long) -> Unit,
    onProgressSyncObserved: ((thumbX: Float, playedBoundaryX: Float, trackWidthPx: Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val safeDuration = durationMs.coerceAtLeast(1000L)
    val externalFraction = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(externalFraction) }
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val thumbRadiusPx = with(density) { 7.dp.toPx() }

    val currentOnSeekTo by rememberUpdatedState(onSeekTo)
    val currentSyncObserver by rememberUpdatedState(onProgressSyncObserved)
    val activeFraction = if (isDragging) dragFraction else externalFraction

    LaunchedEffect(canvasWidthPx, activeFraction, thumbRadiusPx) {
        if (canvasWidthPx > 2f) {
            val geom = computeSeekBarSyncGeometry(canvasWidthPx, thumbRadiusPx, activeFraction)
            currentSyncObserver?.invoke(geom.boundaryX, geom.boundaryX, geom.usableWidth)
        }
    }

    fun xToFraction(x: Float): Float {
        val w = canvasWidthPx.coerceAtLeast(thumbRadiusPx * 2f + 2f)
        val geom = computeSeekBarSyncGeometry(w, thumbRadiusPx, 0f)
        return ((x - geom.trackStartX) / geom.usableWidth).coerceIn(0f, 1f)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Canvas(
            modifier = modifier
                .onSizeChanged { sz ->
                    if (sz.width > 2) {
                        canvasWidthPx = sz.width.toFloat()
                        val geom = computeSeekBarSyncGeometry(sz.width.toFloat(), thumbRadiusPx, activeFraction)
                        currentSyncObserver?.invoke(geom.boundaryX, geom.boundaryX, geom.usableWidth)
                    }
                }
                .pointerInput(safeDuration) {
                    detectTapGestures { offset ->
                        val frac = xToFraction(offset.x)
                        dragFraction = frac
                        currentOnSeekTo((frac * safeDuration).toLong())
                    }
                }
                .pointerInput(safeDuration) {
                    detectHorizontalDragGestures(
                        onDragStart = { startOffset ->
                            isDragging = true
                            val frac = xToFraction(startOffset.x)
                            dragFraction = frac
                            currentOnSeekTo((frac * safeDuration).toLong())
                        },
                        onDragEnd = {
                            val finalFrac = dragFraction
                            isDragging = false
                            currentOnSeekTo((finalFrac * safeDuration).toLong())
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val frac = xToFraction(change.position.x)
                            dragFraction = frac
                            currentOnSeekTo((frac * safeDuration).toLong())
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            if (w <= 2f || h <= 2f) return@Canvas

            val geom = computeSeekBarSyncGeometry(w, thumbRadiusPx, activeFraction)
            val trackStartX = geom.trackStartX
            val trackEndX = geom.trackEndX
            val usableWidth = geom.usableWidth
            val boundaryX = geom.boundaryX
            val clampedFrac = activeFraction.coerceIn(0f, 1f)
            val centerY = h / 2f

            if (waveformMode) {
                val barCount = 34
                val step = usableWidth / barCount.toFloat()
                val strokeW = (step * 0.46f).coerceIn(2f, 6f)
                val maxBarH = (h * 0.68f).coerceAtLeast(6f)

                for (i in 0 until barCount) {
                    val amp = (0.28f + 0.68f * abs(sin(i * 0.62f + 0.9f))).coerceIn(0.22f, 0.98f)
                    val barH = maxBarH * amp
                    val barX = trackStartX + step * i + step / 2f
                    val yTop = centerY - barH / 2f
                    drawLine(
                        color = if (barX <= boundaryX + 0.5f && clampedFrac > 0f) playedColor else unplayedColor,
                        start = Offset(barX, yTop),
                        end = Offset(barX, yTop + barH),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            } else {
                val trackStroke = 4.dp.toPx()
                // Unplayed portion to the right of boundaryX
                if (boundaryX < trackEndX) {
                    drawLine(
                        color = unplayedColor,
                        start = Offset(boundaryX, centerY),
                        end = Offset(trackEndX, centerY),
                        strokeWidth = trackStroke,
                        cap = StrokeCap.Round
                    )
                }
                // Played portion from trackStartX to boundaryX
                if (boundaryX > trackStartX) {
                    drawLine(
                        color = playedColor,
                        start = Offset(trackStartX, centerY),
                        end = Offset(boundaryX, centerY),
                        strokeWidth = trackStroke,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Progress indicator thumb drawn at the exact same boundaryX as the played cutoff
            val activeThumbR = if (isDragging) thumbRadiusPx * 1.18f else thumbRadiusPx
            drawCircle(
                color = playedColor.copy(alpha = if (isDragging) 0.36f else 0.24f),
                radius = activeThumbR * 1.55f,
                center = Offset(boundaryX, centerY)
            )
            drawCircle(
                color = thumbColor,
                radius = activeThumbR,
                center = Offset(boundaryX, centerY)
            )
            if (waveformMode) {
                drawCircle(
                    color = Color.White,
                    radius = (activeThumbR * 0.45f).coerceAtLeast(2f),
                    center = Offset(boundaryX, centerY)
                )
            }

            currentSyncObserver?.invoke(boundaryX, boundaryX, usableWidth)
        }
    }
}

@Composable
internal fun QuranWaveformBarsCanvas(
    progress: Float,
    playedColor: Color,
    unplayedColor: Color,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            if (w <= 2f || h <= 2f) return@Canvas

            val barCount = 34
            val step = w / barCount.toFloat()
            val strokeW = (step * 0.46f).coerceIn(2f, 6f)
            val boundaryX = w * progress.coerceIn(0f, 1f)

            for (i in 0 until barCount) {
                val amp = (0.28f + 0.68f * abs(sin(i * 0.62f + 0.9f))).coerceIn(0.22f, 0.98f)
                val barH = h * amp
                val x = step * i + step / 2f
                val yTop = (h - barH) / 2f
                drawLine(
                    color = if (x <= boundaryX + 0.5f && progress > 0f) playedColor else unplayedColor,
                    start = Offset(x, yTop),
                    end = Offset(x, yTop + barH),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

internal fun DrawScope.drawMosqueSilhouette(silhouetteColor: Color) {
    val w = size.width
    val h = size.height
    if (w <= 2f || h <= 2f) return

    val baseLineY = h * 0.72f
    val domeRadius = w * 0.22f
    val domeCenter = Offset(w * 0.26f, baseLineY)

    drawCircle(
        color = silhouetteColor,
        radius = domeRadius,
        center = domeCenter
    )
    // Minaret spire
    drawLine(
        color = silhouetteColor,
        start = Offset(w * 0.80f, baseLineY + domeRadius),
        end = Offset(w * 0.80f, baseLineY - domeRadius * 1.35f),
        strokeWidth = (w * 0.045f).coerceAtLeast(4f),
        cap = StrokeCap.Round
    )
    // Crescent moon accent at top-right
    drawCircle(
        color = silhouetteColor.copy(alpha = 0.55f),
        radius = (w * 0.04f).coerceAtLeast(4f),
        center = Offset(w * 0.72f, h * 0.18f),
        style = Stroke(width = 2.dp.toPx())
    )
}

@Composable
private fun SyncedLyricsCard(
    parsedLyrics: List<LyricLine>,
    activeLineIndex: Int,
    accent: Color,
    onSeekTo: (Long) -> Unit
) {
    val strings = LocalAppStrings.current
    val designTokens = LocalNaghamDesign.current

    Card(
        modifier = Modifier
            .fillMaxSize()
            .naghamDesignBorder(accentColor = accent),
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
                .padding(18.dp)
        ) {
            Text(
                text = strings.liveLyricsHeader,
                style = MaterialTheme.typography.labelLarge,
                color = accent
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (parsedLyrics.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noLyricsYet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(parsedLyrics) { idx, line ->
                        val isActive = idx == activeLineIndex
                        Text(
                            text = line.text,
                            style = if (isActive) {
                                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            } else {
                                MaterialTheme.typography.bodyLarge
                            },
                            color = if (isActive) {
                                accent
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    line.timestampMs?.let { onSeekTo(it) }
                                }
                        )
                    }
                }
            }
        }
    }
}

