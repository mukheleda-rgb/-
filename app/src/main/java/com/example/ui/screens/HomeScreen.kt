package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CloudTrack
import com.example.data.scanner.DeviceAudioScanner
import com.example.data.scanner.FolderFilterStore
import com.example.ui.components.MiniEqualizerBars
import com.example.ui.components.TrackArtwork
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.AppDesignStyle
import com.example.ui.theme.LocalNaghamColors
import com.example.ui.theme.LocalNaghamDesign
import com.example.ui.theme.naghamDesignBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class HomeTileFilter {
    OVERVIEW,
    RECENT_PLAYED,
    RECENT_ADDED,
    MOST_PLAYED
}

@Composable
fun HomeScreen(
    allTracks: List<CloudTrack>,
    recentTimestamps: Map<String, Long>,
    currentTrackId: String?,
    isPlaying: Boolean,
    isScanningStorage: Boolean,
    showGrantAudioPermissionButton: Boolean = false,
    onRequestAudioPermission: () -> Unit = {},
    onPlayMusicTrack: (List<CloudTrack>, String) -> Unit,
    onLongPressTrack: (CloudTrack) -> Unit,
    onJumpToQuranSanctuary: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    isActiveTab: Boolean = true
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val extColors = LocalNaghamColors.current
    val designTokens = LocalNaghamDesign.current
    var activeFilter by rememberSaveable { mutableStateOf(HomeTileFilter.OVERVIEW) }

    BackHandler(enabled = isActiveTab && activeFilter != HomeTileFilter.OVERVIEW) {
        activeFilter = HomeTileFilter.OVERVIEW
    }

    LaunchedEffect(context) {
        FolderFilterStore.ensureLoaded(context)
    }

    val autoExcludeUnwanted by FolderFilterStore.autoExcludeUnwanted.collectAsStateWithLifecycle()
    val customExcludedFolders by FolderFilterStore.customExcludedFolders.collectAsStateWithLifecycle()
    val allowedOverrideFolders by FolderFilterStore.explicitlyAllowedFolders.collectAsStateWithLifecycle()

    LaunchedEffect(allTracks) {
        if (allTracks.isNotEmpty()) {
            withContext(Dispatchers.Default) {
                val rawFolders = allTracks.mapNotNull { trk ->
                    DeviceAudioScanner.resolveMetaForTrack(context, trk).folderName.takeIf { it.isNotBlank() }
                }
                if (rawFolders.isNotEmpty()) {
                    FolderFilterStore.recordDiscoveredFolders(context, rawFolders)
                }
            }
        }
    }

    val visibleTracks = remember(
        allTracks,
        autoExcludeUnwanted,
        customExcludedFolders,
        allowedOverrideFolders
    ) {
        allTracks.filter { track ->
            track.id.isNotBlank() && !FolderFilterStore.isTrackExcluded(
                context = context,
                track = track,
                autoExclude = autoExcludeUnwanted,
                customExcluded = customExcludedFolders,
                allowedOverrides = allowedOverrideFolders
            )
        }.distinctBy { it.id }
    }

    // Strict isolation: Home Music Tiles ONLY display non-Quran tracks from allowed folders
    val musicTracks = remember(visibleTracks) {
        visibleTracks.filter { !it.quranAudio }
    }
    val isolatedQuranCount = remember(visibleTracks) { visibleTracks.count { it.quranAudio } }

    val recentlyAddedAll = remember(musicTracks) {
        musicTracks.sortedWith(
            compareByDescending<CloudTrack> { it.createdAt?.seconds ?: 0L }
                .thenByDescending { it.createdAt?.nanoseconds ?: 0 }
                .thenByDescending { it.id }
        )
    }

    val recentlyPlayedAll = remember(musicTracks, recentTimestamps, recentlyAddedAll) {
        val played = musicTracks.filter { recentTimestamps.containsKey(it.id) || it.playCount > 0L }
            .sortedWith(
                compareByDescending<CloudTrack> { recentTimestamps[it.id] ?: 0L }
                    .thenByDescending { it.updatedAt?.seconds ?: 0L }
                    .thenByDescending { it.playCount }
            )
        played.ifEmpty { recentlyAddedAll.take(6) }
    }

    val mostPlayedAll = remember(musicTracks, recentTimestamps) {
        musicTracks.sortedWith(
            compareByDescending<CloudTrack> { it.playCount }
                .thenByDescending { recentTimestamps[it.id] ?: 0L }
                .thenBy { it.title.lowercase() }
        )
    }

    val totalPlayCount = remember(musicTracks) { musicTracks.sumOf { it.playCount } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Clean Command-Free Top Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
                    .naghamDesignBorder(
                        shapeOverride = designTokens.heroShape,
                        isHighlighted = true
                    )
                    .clip(designTokens.heroShape)
                    .then(
                        if (designTokens.style == AppDesignStyle.MATERIAL) {
                            Modifier.border(1.dp, extColors.cardBorder, designTokens.heroShape)
                        } else {
                            Modifier
                        }
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_music_hero),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    extColors.heroOverlayStart,
                                    extColors.heroOverlayEnd
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.homeHeroTitle,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = extColors.onHeroOverlay
                            )
                            Text(
                                text = strings.homeHeroSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = extColors.onHeroOverlay.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                            shape = designTokens.pillShape,
                            modifier = Modifier
                                .clip(designTokens.pillShape)
                                .clickable(onClick = onOpenEqualizer)
                                .testTag("home_hero_equalizer_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Equalizer,
                                    contentDescription = strings.navEqualizer,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = strings.navEqualizer,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        shape = designTokens.pillShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isScanningStorage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isScanningStorage) strings.scanningStorage else strings.autoScanActiveBadge,
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

        if (showGrantAudioPermissionButton) {
            item {
                Card(
                    shape = designTokens.cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .naghamDesignBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.enableAutoScanPermissionBtn,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestAudioPermission,
                            modifier = Modifier.testTag("home_grant_audio_permission_button")
                        ) {
                            Text(strings.saveAction)
                        }
                    }
                }
            }
        }

        // 2x2 Interactive Bento Summary Squares (مربعات الواجهة الرئيسية)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeSummaryBentoSquare(
                        title = strings.tileRecentlyPlayed,
                        subtitle = strings.tracksCount(recentlyPlayedAll.size),
                        icon = Icons.Default.History,
                        accentColor = MaterialTheme.colorScheme.primary,
                        isSelected = activeFilter == HomeTileFilter.RECENT_PLAYED,
                        testTag = "home_bento_recent_played",
                        onClick = {
                            activeFilter = if (activeFilter == HomeTileFilter.RECENT_PLAYED) {
                                HomeTileFilter.OVERVIEW
                            } else {
                                HomeTileFilter.RECENT_PLAYED
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HomeSummaryBentoSquare(
                        title = strings.tileRecentlyAdded,
                        subtitle = strings.tracksCount(recentlyAddedAll.size),
                        icon = Icons.Default.FiberNew,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        isSelected = activeFilter == HomeTileFilter.RECENT_ADDED,
                        testTag = "home_bento_recent_added",
                        onClick = {
                            activeFilter = if (activeFilter == HomeTileFilter.RECENT_ADDED) {
                                HomeTileFilter.OVERVIEW
                            } else {
                                HomeTileFilter.RECENT_ADDED
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeSummaryBentoSquare(
                        title = strings.tileMostPlayed,
                        subtitle = strings.playsCountBadge(totalPlayCount),
                        icon = Icons.Default.LocalFireDepartment,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        isSelected = activeFilter == HomeTileFilter.MOST_PLAYED,
                        testTag = "home_bento_most_played",
                        onClick = {
                            activeFilter = if (activeFilter == HomeTileFilter.MOST_PLAYED) {
                                HomeTileFilter.OVERVIEW
                            } else {
                                HomeTileFilter.MOST_PLAYED
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HomeSummaryBentoSquare(
                        title = strings.navQuran,
                        subtitle = strings.tracksCount(isolatedQuranCount),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        accentColor = extColors.quranAccent,
                        isSelected = false,
                        testTag = "home_bento_quran",
                        onClick = onJumpToQuranSanctuary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Dedicated Equalizer Studio Quick Card
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
                    .naghamDesignBorder(isHighlighted = true)
                    .clip(designTokens.cardShape)
                    .clickable(onClick = onOpenEqualizer)
                    .testTag("home_open_equalizer_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.eqTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = strings.eqOpenStudioDesc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = designTokens.pillShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = strings.eqOpenStudioBtn,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Section Filter Chips + Long-press Tip
        item {
            val chipRowScroll = rememberScrollState()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(chipRowScroll, enabled = chipRowScroll.maxValue > 0),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeFilter == HomeTileFilter.OVERVIEW,
                        onClick = { activeFilter = HomeTileFilter.OVERVIEW },
                        label = { Text(strings.tileOverview) },
                        modifier = Modifier.testTag("home_chip_overview")
                    )
                    FilterChip(
                        selected = activeFilter == HomeTileFilter.RECENT_PLAYED,
                        onClick = { activeFilter = HomeTileFilter.RECENT_PLAYED },
                        label = { Text(strings.tileRecentlyPlayed) },
                        modifier = Modifier.testTag("home_chip_recent_played")
                    )
                    FilterChip(
                        selected = activeFilter == HomeTileFilter.RECENT_ADDED,
                        onClick = { activeFilter = HomeTileFilter.RECENT_ADDED },
                        label = { Text(strings.tileRecentlyAdded) },
                        modifier = Modifier.testTag("home_chip_recent_added")
                    )
                    FilterChip(
                        selected = activeFilter == HomeTileFilter.MOST_PLAYED,
                        onClick = { activeFilter = HomeTileFilter.MOST_PLAYED },
                        label = { Text(strings.tileMostPlayed) },
                        modifier = Modifier.testTag("home_chip_most_played")
                    )
                }
                Text(
                    text = strings.longPressHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (musicTracks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .naghamDesignBorder(),
                    colors = CardDefaults.cardColors(
                        containerColor = designTokens.cardContainerColor(
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    ),
                    shape = designTokens.cardShape
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = strings.emptyMusicTitle,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.emptyMusicTip,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Section 1: Recently Played Grid Tiles (آخر الأغاني المستمعة)
            if (activeFilter == HomeTileFilter.OVERVIEW || activeFilter == HomeTileFilter.RECENT_PLAYED) {
                val limit = if (activeFilter == HomeTileFilter.OVERVIEW) 4 else 24
                val itemsToShow = recentlyPlayedAll.take(limit)
                if (itemsToShow.isNotEmpty()) {
                    item {
                        HomeSectionHeader(
                            title = strings.tileRecentlyPlayed,
                            icon = Icons.Default.History,
                            onPlaySection = {
                                onPlayMusicTrack(recentlyPlayedAll, recentlyPlayedAll.first().id)
                            },
                            playAllLabel = strings.playAll
                        )
                    }
                    val rows = itemsToShow.chunked(2)
                    rows.forEach { pair ->
                        item(key = "rp_${pair.joinToString("_") { it.id }}") {
                            TrackTileGridRow(
                                pair = pair,
                                sectionKey = "recent_played",
                                currentTrackId = currentTrackId,
                                isPlaying = isPlaying,
                                badgeProvider = { t ->
                                    if (t.playCount > 0L) strings.playsCountBadge(t.playCount) else null
                                },
                                onTapTrack = { tapped -> onPlayMusicTrack(recentlyPlayedAll, tapped.id) },
                                onLongPressTrack = onLongPressTrack
                            )
                        }
                    }
                }
            }

            // Section 2: Recently Added Grid Tiles (المضافة حديثاً)
            if (activeFilter == HomeTileFilter.OVERVIEW || activeFilter == HomeTileFilter.RECENT_ADDED) {
                val limit = if (activeFilter == HomeTileFilter.OVERVIEW) 4 else 24
                val itemsToShow = recentlyAddedAll.take(limit)
                if (itemsToShow.isNotEmpty()) {
                    item {
                        HomeSectionHeader(
                            title = strings.tileRecentlyAdded,
                            icon = Icons.Default.FiberNew,
                            onPlaySection = {
                                onPlayMusicTrack(recentlyAddedAll, recentlyAddedAll.first().id)
                            },
                            playAllLabel = strings.playAll
                        )
                    }
                    val rows = itemsToShow.chunked(2)
                    rows.forEach { pair ->
                        item(key = "ra_${pair.joinToString("_") { it.id }}") {
                            TrackTileGridRow(
                                pair = pair,
                                sectionKey = "recent_added",
                                currentTrackId = currentTrackId,
                                isPlaying = isPlaying,
                                badgeProvider = { strings.newAddedBadge },
                                onTapTrack = { tapped -> onPlayMusicTrack(recentlyAddedAll, tapped.id) },
                                onLongPressTrack = onLongPressTrack
                            )
                        }
                    }
                }
            }

            // Section 3: Most Played Grid Tiles (الأكثر استماعاً)
            if (activeFilter == HomeTileFilter.OVERVIEW || activeFilter == HomeTileFilter.MOST_PLAYED) {
                val limit = if (activeFilter == HomeTileFilter.OVERVIEW) 4 else 24
                val itemsToShow = mostPlayedAll.take(limit)
                if (itemsToShow.isNotEmpty()) {
                    item {
                        HomeSectionHeader(
                            title = strings.tileMostPlayed,
                            icon = Icons.Default.LocalFireDepartment,
                            onPlaySection = {
                                onPlayMusicTrack(mostPlayedAll, mostPlayedAll.first().id)
                            },
                            playAllLabel = strings.playAll
                        )
                    }
                    val rows = itemsToShow.chunked(2)
                    rows.forEach { pair ->
                        item(key = "mp_${pair.joinToString("_") { it.id }}") {
                            TrackTileGridRow(
                                pair = pair,
                                sectionKey = "most_played",
                                currentTrackId = currentTrackId,
                                isPlaying = isPlaying,
                                badgeProvider = { t -> strings.playsCountBadge(t.playCount) },
                                onTapTrack = { tapped -> onPlayMusicTrack(mostPlayedAll, tapped.id) },
                                onLongPressTrack = onLongPressTrack
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSummaryBentoSquare(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val designTokens = LocalNaghamDesign.current
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.72f),
        label = "bentoScale"
    )
    val animatedContainerColor by animateColorAsState(
        targetValue = if (isSelected) {
            accentColor.copy(alpha = 0.24f)
        } else {
            designTokens.cardContainerColor(MaterialTheme.colorScheme.surfaceContainerHigh)
        },
        animationSpec = tween(220),
        label = "bentoColor"
    )
    Surface(
        onClick = onClick,
        shape = designTokens.cardShape,
        color = animatedContainerColor,
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .naghamDesignBorder(
                accentColor = accentColor,
                shapeOverride = designTokens.cardShape,
                isHighlighted = isSelected
            )
            .then(
                if (designTokens.style == AppDesignStyle.MATERIAL) {
                    Modifier.border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) accentColor else accentColor.copy(alpha = 0.24f),
                        shape = designTokens.cardShape
                    )
                } else {
                    Modifier
                }
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
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
private fun HomeSectionHeader(
    title: String,
    icon: ImageVector,
    onPlaySection: () -> Unit,
    playAllLabel: String
) {
    val designTokens = LocalNaghamDesign.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        Surface(
            onClick = onPlaySection,
            shape = designTokens.pillShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = playAllLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun TrackTileGridRow(
    pair: List<CloudTrack>,
    sectionKey: String,
    currentTrackId: String?,
    isPlaying: Boolean,
    badgeProvider: (CloudTrack) -> String?,
    onTapTrack: (CloudTrack) -> Unit,
    onLongPressTrack: (CloudTrack) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val first = pair[0]
        HomeTrackTileCard(
            track = first,
            sectionKey = sectionKey,
            isCurrent = first.id == currentTrackId,
            isPlaying = first.id == currentTrackId && isPlaying,
            badgeText = badgeProvider(first),
            onClick = { onTapTrack(first) },
            onLongClick = { onLongPressTrack(first) },
            modifier = Modifier.weight(1f)
        )
        if (pair.size > 1) {
            val second = pair[1]
            HomeTrackTileCard(
                track = second,
                sectionKey = sectionKey,
                isCurrent = second.id == currentTrackId,
                isPlaying = second.id == currentTrackId && isPlaying,
                badgeText = badgeProvider(second),
                onClick = { onTapTrack(second) },
                onLongClick = { onLongPressTrack(second) },
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeTrackTileCard(
    track: CloudTrack,
    sectionKey: String,
    isCurrent: Boolean,
    isPlaying: Boolean,
    badgeText: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val designTokens = LocalNaghamDesign.current
    val tileShape = designTokens.cardShape

    Card(
        modifier = modifier
            .height(168.dp)
            .naghamDesignBorder(
                accentColor = MaterialTheme.colorScheme.primary,
                shapeOverride = tileShape,
                isHighlighted = isCurrent
            )
            .clip(tileShape)
            .then(
                if (designTokens.style == AppDesignStyle.MATERIAL) {
                    Modifier.border(
                        width = if (isCurrent) 2.dp else 1.dp,
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
                        },
                        shape = tileShape
                    )
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .testTag("home_tile_${sectionKey}_${track.id}"),
        shape = tileShape,
        colors = CardDefaults.cardColors(
            containerColor = designTokens.cardContainerColor(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                isHighlighted = isCurrent
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            TrackArtwork(
                track = track,
                shape = tileShape,
                showFolderBadge = false,
                modifier = Modifier.fillMaxSize()
            )

            // Scrim gradient for readable typography over album/folder art
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.45f),
                                Color.Black.copy(alpha = 0.86f)
                            )
                        )
                    )
            )

            // Top row: Play/Equalizer circle + Badge pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        MiniEqualizerBars(
                            isPlaying = true,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (!badgeText.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.90f),
                        shape = designTokens.pillShape
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Bottom Track Title & Artist
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
