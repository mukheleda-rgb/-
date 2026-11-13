package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CloudPlaylist
import com.example.data.model.CloudTrack
import com.example.data.scanner.DeviceAudioScanner
import com.example.data.scanner.FolderFilterStore
import com.example.data.scanner.TrackSortStore
import com.example.ui.components.BatchAddToPlaylistDialog
import com.example.ui.components.BatchDeleteConfirmDialog
import com.example.ui.components.ManageExcludedFoldersDialog
import com.example.ui.components.MultiSelectActionBar
import com.example.ui.components.ReorderableTrackListBox
import com.example.ui.components.TrackDragHandle
import com.example.ui.components.TrackSelectionCheckbox
import com.example.ui.components.TrackSortBar
import com.example.ui.components.rememberReorderableTrackListController
import com.example.ui.components.reorderableTrackItem
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.LocalNaghamDesign

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuranSanctuaryScreen(
    allTracks: List<CloudTrack>,
    playlists: List<CloudPlaylist> = emptyList(),
    currentTrackId: String?,
    isPlaying: Boolean,
    isScanningStorage: Boolean,
    initialSelectedTrackId: String? = null,
    onInitialSelectionConsumed: () -> Unit = {},
    onPlayQuranQueue: (List<CloudTrack>, String) -> Unit,
    onEditTrack: (CloudTrack) -> Unit,
    onLongPressTrack: (CloudTrack) -> Unit = {},
    onBatchDeleteTracks: (List<CloudTrack>) -> Unit = {},
    onBatchAddTracksToPlaylist: (List<CloudTrack>, CloudPlaylist) -> Unit = { _, _ -> },
    onBatchCreatePlaylistWithTracks: (String, List<CloudTrack>) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
    isActiveTab: Boolean = true
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val strings = LocalAppStrings.current
    val designTokens = LocalNaghamDesign.current

    val emeraldLime = Color(0xFF5CE65C)
    val sanctuaryBgTop = Color(0xFF071A15)
    val sanctuaryBgMid = Color(0xFF0C261E)
    val sanctuaryBgBottom = Color(0xFF05120E)

    LaunchedEffect(context) {
        TrackSortStore.ensureLoaded(context)
        FolderFilterStore.ensureLoaded(context)
    }

    val selectedSort by TrackSortStore.quranSortOption.collectAsStateWithLifecycle()
    val customOrderIds by TrackSortStore.quranCustomOrder.collectAsStateWithLifecycle()
    val autoExcludeUnwanted by FolderFilterStore.autoExcludeUnwanted.collectAsStateWithLifecycle()
    val customExcludedFolders by FolderFilterStore.customExcludedFolders.collectAsStateWithLifecycle()
    val allowedOverrideFolders by FolderFilterStore.explicitlyAllowedFolders.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf("ALL_FOLDERS") }
    var showExcludedFoldersDialog by remember { mutableStateOf(false) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showBatchPlaylistDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    val quranTracks = remember(allTracks, autoExcludeUnwanted, customExcludedFolders, allowedOverrideFolders) {
        allTracks
            .filter { track ->
                track.quranAudio &&
                    track.id.isNotBlank() &&
                    !FolderFilterStore.isTrackExcluded(
                        context = context,
                        track = track,
                        autoExclude = autoExcludeUnwanted,
                        customExcluded = customExcludedFolders,
                        allowedOverrides = allowedOverrideFolders
                    )
            }
            .distinctBy { it.id }
    }

    LaunchedEffect(initialSelectedTrackId, quranTracks) {
        if (!initialSelectedTrackId.isNullOrBlank() && quranTracks.any { it.id == initialSelectedTrackId }) {
            isSelectionMode = true
            selectedIds = selectedIds + initialSelectedTrackId
            onInitialSelectionConsumed()
        }
    }

    LaunchedEffect(quranTracks) {
        if (selectedIds.isNotEmpty()) {
            val validSet = quranTracks.map { it.id }.toSet()
            selectedIds = selectedIds.intersect(validSet)
        }
    }

    val exitSelectionMode: () -> Unit = {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    BackHandler(enabled = isActiveTab && !isSelectionMode, onBack = onBack)
    BackHandler(enabled = isActiveTab && isSelectionMode, onBack = exitSelectionMode)

    val folderBySurahId = remember(quranTracks) {
        quranTracks.associate { surah ->
            surah.id to DeviceAudioScanner.resolveMetaForTrack(context, surah).folderName
        }
    }
    val availableFolders = remember(folderBySurahId) {
        folderBySurahId.values.filter { it.isNotBlank() }.distinct().sorted()
    }

    LaunchedEffect(availableFolders, selectedFolder) {
        if (selectedFolder != "ALL_FOLDERS" && selectedFolder !in availableFolders) {
            selectedFolder = "ALL_FOLDERS"
        }
    }

    val filteredQuranTracks = remember(quranTracks, searchQuery, selectedFolder, folderBySurahId) {
        quranTracks.filter { surah ->
            val folderName = folderBySurahId[surah.id].orEmpty()
            val matchesFolder = selectedFolder == "ALL_FOLDERS" || folderName == selectedFolder
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                surah.title.lowercase().contains(q) ||
                surah.artist.lowercase().contains(q) ||
                folderName.lowercase().contains(q)
            matchesFolder && matchesQuery
        }
    }

    val sortedQuranTracks = remember(filteredQuranTracks, selectedSort, customOrderIds) {
        TrackSortStore.sortTracks(
            tracks = filteredQuranTracks,
            option = selectedSort,
            customOrderIds = customOrderIds,
            dateLookupMs = { DeviceAudioScanner.resolveTrackDateMs(context, it) }
        )
    }

    val featuredSurah = remember(sortedQuranTracks, quranTracks, currentTrackId) {
        sortedQuranTracks.firstOrNull { it.id == currentTrackId }
            ?: quranTracks.firstOrNull { it.id == currentTrackId }
            ?: sortedQuranTracks.firstOrNull()
            ?: quranTracks.firstOrNull()
    }

    val selectedSurahsOrdered = remember(sortedQuranTracks, selectedIds) {
        sortedQuranTracks.filter { it.id in selectedIds }
    }

    val toggleSurahSelection: (String) -> Unit = { id ->
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    val lazyListState = rememberLazyListState()
    val reorderController = rememberReorderableTrackListController(lazyListState)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(sanctuaryBgTop, sanctuaryBgMid, sanctuaryBgBottom)
                )
            )
            .drawBehind {
                drawMosqueSilhouette(Color(0xFF143C2F).copy(alpha = 0.42f))
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                MultiSelectActionBar(
                    selectedCount = selectedIds.size,
                    totalVisibleCount = sortedQuranTracks.size,
                    scopeTagPrefix = "quran",
                    accentColor = emeraldLime,
                    onSelectAllOrClear = {
                        val visibleIds = sortedQuranTracks.map { it.id }.toSet()
                        selectedIds = if (visibleIds.isNotEmpty() && visibleIds.all { it in selectedIds }) {
                            emptySet()
                        } else {
                            visibleIds
                        }
                    },
                    onAddToPlaylistClick = {
                        if (selectedSurahsOrdered.isNotEmpty()) {
                            showBatchPlaylistDialog = true
                        }
                    },
                    onDeleteSelectedClick = {
                        if (selectedSurahsOrdered.isNotEmpty()) {
                            showBatchDeleteDialog = true
                        }
                    },
                    onExitSelectionMode = exitSelectionMode
                )
            }

            ReorderableTrackListBox(
                controller = reorderController,
                tracks = sortedQuranTracks,
                onMoveTrack = { movedId, targetId ->
                    val newOrder = TrackSortStore.moveTrackInOrder(
                        baseOrderIds = customOrderIds,
                        fallbackTracksInCurrentSort = sortedQuranTracks,
                        movedTrackId = movedId,
                        targetTrackId = targetId
                    )
                    TrackSortStore.saveQuranCustomOrder(context, newOrder)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("quran_tracks_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 148.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sanctuary Top Title & Active Isolation Status (Image 3 Left)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.navQuran,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = strings.quranIsolationPledge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF9ED4C0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                color = Color(0xFF12382B),
                                shape = designTokens.pillShape,
                                border = BorderStroke(1.dp, emeraldLime.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isScanningStorage) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = emeraldLime
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.VerifiedUser,
                                            contentDescription = null,
                                            tint = emeraldLime,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.tracksCount(quranTracks.size),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFEAFBF3)
                                    )
                                }
                            }
                        }
                    }

                    // Rounded Pill Search Bar (Image 3 Left)
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = strings.searchPlaceholder,
                                    color = Color(0xFF8BBCA9)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF8BBCA9)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF102F25),
                                unfocusedContainerColor = Color(0xFF0E2920),
                                focusedBorderColor = emeraldLime.copy(alpha = 0.7f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quran_search_input")
                        )
                    }

                    // Featured Recitation Card with Circular Lime-Emerald Play Button & Waveform (Image 3 Left)
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF12362A).copy(alpha = 0.92f)
                            ),
                            border = BorderStroke(1.dp, emeraldLime.copy(alpha = 0.36f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .clickable {
                                    featuredSurah?.let { target ->
                                        onPlayQuranQueue(sortedQuranTracks.ifEmpty { quranTracks }, target.id)
                                    }
                                }
                                .testTag("quran_featured_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_quran_sanctuary),
                                    contentDescription = strings.quranSanctuaryTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .border(1.dp, emeraldLime.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = featuredSurah?.title ?: strings.quranSanctuaryTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = featuredSurah?.artist ?: strings.quranSanctuarySubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFA7D7C5),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val isFeaturedPlaying = featuredSurah != null && featuredSurah.id == currentTrackId && isPlaying
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        QuranWaveformBarsCanvas(
                                            progress = if (isFeaturedPlaying) 0.55f else 0.35f,
                                            playedColor = Color(0xFFFF7A45),
                                            unplayedColor = Color(0xFF285244),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                val isFeaturedActivePlaying =
                                    featuredSurah != null && featuredSurah.id == currentTrackId && isPlaying
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(emeraldLime)
                                        .clickable {
                                            featuredSurah?.let { target ->
                                                onPlayQuranQueue(sortedQuranTracks.ifEmpty { quranTracks }, target.id)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isFeaturedActivePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = strings.playAll,
                                        tint = Color(0xFF04210E),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Unified Single-Row Primary Toolbar (Select • Folders • Sort)
                    item {
                        TrackSortBar(
                            selectedSort = selectedSort,
                            onSelectSort = { option ->
                                TrackSortStore.setQuranSortOption(
                                    context = context,
                                    option = option,
                                    seedOrderIdsIfEmpty = sortedQuranTracks.map { it.id }
                                )
                            },
                            scopeTagPrefix = "quran",
                            accentColor = emeraldLime,
                            isSelectionMode = isSelectionMode,
                            onToggleSelectionMode = {
                                if (isSelectionMode) {
                                    exitSelectionMode()
                                } else {
                                    isSelectionMode = true
                                }
                            },
                            onSelectAllRequest = {
                                selectedIds = sortedQuranTracks.map { it.id }.toSet()
                            },
                            showFoldersMenu = true,
                            availableFolders = availableFolders,
                            selectedFolder = selectedFolder,
                            onSelectFolder = { chosen -> selectedFolder = chosen },
                            onHideFolder = { folderToHide ->
                                FolderFilterStore.excludeFolder(context, folderToHide)
                            },
                            onManageExcludedFoldersClick = {
                                showExcludedFoldersDialog = true
                            }
                        )
                    }

                    // Glowing-border Emerald Surah Cards (Image 3 Left)
                    items(sortedQuranTracks, key = { it.id }) { surah ->
                        val isCurrent = surah.id == currentTrackId
                        val isSurahPlaying = isCurrent && isPlaying
                        val isSelected = surah.id in selectedIds
                        val highlighted = isCurrent || isSelected
                        val localMeta = remember(surah.sourceUri, surah.album) {
                            DeviceAudioScanner.resolveMetaForTrack(context, surah)
                        }

                        val cardColor = when {
                            isSelected -> emeraldLime.copy(alpha = 0.22f)
                            isCurrent -> Color(0xFF164335)
                            else -> Color(0xFF0F2D24).copy(alpha = 0.90f)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .reorderableTrackItem(surah.id, if (isSelectionMode) null else reorderController)
                                .clip(RoundedCornerShape(20.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            toggleSurahSelection(surah.id)
                                        } else {
                                            onPlayQuranQueue(sortedQuranTracks, surah.id)
                                        }
                                    },
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (isSelectionMode) {
                                            toggleSurahSelection(surah.id)
                                        } else {
                                            onLongPressTrack(surah)
                                        }
                                    }
                                )
                                .testTag("quran_item_card_${surah.id}"),
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            border = BorderStroke(
                                width = if (highlighted) 1.6.dp else 1.dp,
                                color = if (highlighted) emeraldLime else emeraldLime.copy(alpha = 0.26f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelectionMode) {
                                    TrackSelectionCheckbox(
                                        trackId = surah.id,
                                        selected = isSelected,
                                        accentColor = emeraldLime,
                                        onToggle = { toggleSurahSelection(surah.id) }
                                    )
                                } else {
                                    TrackDragHandle(
                                        trackId = surah.id,
                                        controller = reorderController,
                                        tint = emeraldLime
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))

                                // Circular Lime-Emerald Play Badge (Image 3 Left)
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(emeraldLime)
                                        .clickable {
                                            if (isSelectionMode) {
                                                toggleSurahSelection(surah.id)
                                            } else {
                                                onPlayQuranQueue(sortedQuranTracks, surah.id)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSurahPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = strings.playAll,
                                        tint = Color(0xFF04210E),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = surah.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (highlighted) emeraldLime else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val durationLabel = if (surah.durationMs > 0L) formatDurationMs(surah.durationMs) else null
                                    val subtitle = listOfNotNull(
                                        surah.artist.takeIf { it.isNotBlank() },
                                        localMeta.folderName.takeIf { it.isNotBlank() && it != surah.artist },
                                        durationLabel
                                    ).joinToString(" • ")
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFA7D7C5),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (!isSelectionMode) {
                                    IconButton(onClick = { onEditTrack(surah) }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Surah Details",
                                            tint = Color(0xFFA7D7C5)
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

    if (showExcludedFoldersDialog) {
        ManageExcludedFoldersDialog(
            onDismiss = { showExcludedFoldersDialog = false },
            accentColor = emeraldLime
        )
    }

    if (showBatchPlaylistDialog && selectedSurahsOrdered.isNotEmpty()) {
        BatchAddToPlaylistDialog(
            selectedTracks = selectedSurahsOrdered,
            playlists = playlists,
            onDismiss = { showBatchPlaylistDialog = false },
            onAddToExistingPlaylist = { tracks, pl ->
                onBatchAddTracksToPlaylist(tracks, pl)
                exitSelectionMode()
            },
            onCreateNewPlaylistWithTracks = { name, tracks ->
                onBatchCreatePlaylistWithTracks(name, tracks)
                exitSelectionMode()
            }
        )
    }

    if (showBatchDeleteDialog && selectedSurahsOrdered.isNotEmpty()) {
        BatchDeleteConfirmDialog(
            selectedCount = selectedSurahsOrdered.size,
            onDismiss = { showBatchDeleteDialog = false },
            onConfirmDelete = {
                onBatchDeleteTracks(selectedSurahsOrdered)
                exitSelectionMode()
            }
        )
    }
}
