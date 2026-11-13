package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.data.model.CloudPlaylist
import com.example.data.model.CloudTrack
import com.example.data.model.TrackCategory
import com.example.data.scanner.DeviceAudioScanner
import com.example.data.scanner.FolderFilterStore
import com.example.data.scanner.TrackSortStore
import com.example.ui.components.BatchAddToPlaylistDialog
import com.example.ui.components.BatchDeleteConfirmDialog
import com.example.ui.components.CategoryFilterItem
import com.example.ui.components.ManageExcludedFoldersDialog
import com.example.ui.components.MultiSelectActionBar
import com.example.ui.components.ReorderableTrackListBox
import com.example.ui.components.ReorderableTrackListController
import com.example.ui.components.TrackArtwork
import com.example.ui.components.TrackDragHandle
import com.example.ui.components.TrackSelectionCheckbox
import com.example.ui.components.TrackSortBar
import com.example.ui.components.rememberReorderableTrackListController
import com.example.ui.components.reorderableTrackItem
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.AmberGoldPrimary
import com.example.ui.theme.AppDesignStyle
import com.example.ui.theme.LocalNaghamColors
import com.example.ui.theme.LocalNaghamDesign
import com.example.ui.theme.naghamDesignBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LibraryScreen(
    allTracks: List<CloudTrack>,
    playlists: List<CloudPlaylist> = emptyList(),
    currentTrackId: String?,
    isPlaying: Boolean,
    isScanningStorage: Boolean,
    initialSelectedTrackId: String? = null,
    onInitialSelectionConsumed: () -> Unit = {},
    onPlayMusicTrack: (List<CloudTrack>, String) -> Unit,
    onToggleFavorite: (CloudTrack) -> Unit,
    onEditTrack: (CloudTrack) -> Unit,
    onLongPressTrack: (CloudTrack) -> Unit = {},
    onBatchDeleteTracks: (List<CloudTrack>) -> Unit = {},
    onBatchAddTracksToPlaylist: (List<CloudTrack>, CloudPlaylist) -> Unit = { _, _ -> },
    onBatchCreatePlaylistWithTracks: (String, List<CloudTrack>) -> Unit = { _, _ -> },
    onJumpToQuranSanctuary: () -> Unit,
    onOpenPlayer: (() -> Unit)? = null,
    isActiveTab: Boolean = true
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val extColors = LocalNaghamColors.current
    val designTokens = LocalNaghamDesign.current

    LaunchedEffect(context) {
        TrackSortStore.ensureLoaded(context)
        FolderFilterStore.ensureLoaded(context)
    }

    val selectedSort by TrackSortStore.musicSortOption.collectAsStateWithLifecycle()
    val customOrderIds by TrackSortStore.musicCustomOrder.collectAsStateWithLifecycle()
    val autoExcludeUnwanted by FolderFilterStore.autoExcludeUnwanted.collectAsStateWithLifecycle()
    val customExcludedFolders by FolderFilterStore.customExcludedFolders.collectAsStateWithLifecycle()
    val allowedOverrideFolders by FolderFilterStore.explicitlyAllowedFolders.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedFolder by remember { mutableStateOf("ALL_FOLDERS") }
    var showExcludedFoldersDialog by remember { mutableStateOf(false) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showBatchPlaylistDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    // Record all discovered folders so users can restore or hide any folder in ManageExcludedFoldersDialog
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

    // Strict isolation on Music Library tab + Unwanted folder exclusion (WhatsApp, Voice Notes, Recordings)
    val musicTracks = remember(allTracks, autoExcludeUnwanted, customExcludedFolders, allowedOverrideFolders) {
        allTracks
            .filter { track ->
                !track.quranAudio &&
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

    val isolatedQuranCount = remember(allTracks, autoExcludeUnwanted, customExcludedFolders, allowedOverrideFolders) {
        allTracks.count { track ->
            track.quranAudio && !FolderFilterStore.isTrackExcluded(
                context = context,
                track = track,
                autoExclude = autoExcludeUnwanted,
                customExcluded = customExcludedFolders,
                allowedOverrides = allowedOverrideFolders
            )
        }
    }

    LaunchedEffect(initialSelectedTrackId, musicTracks) {
        if (!initialSelectedTrackId.isNullOrBlank() && musicTracks.any { it.id == initialSelectedTrackId }) {
            isSelectionMode = true
            selectedIds = selectedIds + initialSelectedTrackId
            onInitialSelectionConsumed()
        }
    }

    LaunchedEffect(musicTracks) {
        if (selectedIds.isNotEmpty()) {
            val validSet = musicTracks.map { it.id }.toSet()
            selectedIds = selectedIds.intersect(validSet)
        }
    }

    val exitSelectionMode: () -> Unit = {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    BackHandler(enabled = isActiveTab && isSelectionMode, onBack = exitSelectionMode)

    val folderByTrackId = remember(musicTracks) {
        musicTracks.associate { track ->
            track.id to DeviceAudioScanner.resolveMetaForTrack(context, track).folderName
        }
    }
    val availableFolders = remember(folderByTrackId) {
        folderByTrackId.values.filter { it.isNotBlank() }.distinct().sorted()
    }

    LaunchedEffect(availableFolders, selectedFolder) {
        if (selectedFolder != "ALL_FOLDERS" && selectedFolder !in availableFolders) {
            selectedFolder = "ALL_FOLDERS"
        }
    }

    val categoryOptions = remember(strings) {
        buildList {
            add(CategoryFilterItem("ALL", strings.allMusicChip))
            add(CategoryFilterItem("FAV", strings.favoritesChip))
            TrackCategory.entries
                .filter { it != TrackCategory.QURAN }
                .forEach { cat ->
                    add(CategoryFilterItem(cat.id, if (strings.isArabic) cat.labelAr else cat.labelEn))
                }
        }
    }

    val filteredUnsortedTracks = remember(musicTracks, searchQuery, selectedFilter, selectedFolder, folderByTrackId) {
        musicTracks.filter { track ->
            val matchesCategory = when (selectedFilter) {
                "ALL" -> true
                "FAV" -> track.favorite
                else -> track.category == selectedFilter
            }
            val folderName = folderByTrackId[track.id].orEmpty()
            val matchesFolder = selectedFolder == "ALL_FOLDERS" || folderName == selectedFolder

            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                track.title.lowercase().contains(q) ||
                track.artist.lowercase().contains(q) ||
                track.album.lowercase().contains(q) ||
                folderName.lowercase().contains(q)

            matchesCategory && matchesFolder && matchesQuery
        }
    }

    val sortedTracks = remember(filteredUnsortedTracks, selectedSort, customOrderIds) {
        TrackSortStore.sortTracks(
            tracks = filteredUnsortedTracks,
            option = selectedSort,
            customOrderIds = customOrderIds,
            dateLookupMs = { DeviceAudioScanner.resolveTrackDateMs(context, it) }
        )
    }

    val selectedTracksOrdered = remember(sortedTracks, musicTracks, selectedIds) {
        val visibleOrdered = sortedTracks.filter { it.id in selectedIds }
        val hiddenOrdered = musicTracks.filter { it.id in selectedIds && visibleOrdered.none { v -> v.id == it.id } }
        visibleOrdered + hiddenOrdered
    }

    val toggleTrackSelection: (String) -> Unit = { id ->
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    val lazyListState = rememberLazyListState()
    val reorderController = rememberReorderableTrackListController(lazyListState)

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            MultiSelectActionBar(
                selectedCount = selectedIds.size,
                totalVisibleCount = sortedTracks.size,
                scopeTagPrefix = "library",
                onSelectAllOrClear = {
                    val visibleIds = sortedTracks.map { it.id }.toSet()
                    selectedIds = if (visibleIds.isNotEmpty() && visibleIds.all { it in selectedIds }) {
                        selectedIds - visibleIds
                    } else {
                        selectedIds + visibleIds
                    }
                },
                onAddToPlaylistClick = {
                    if (selectedTracksOrdered.isNotEmpty()) {
                        showBatchPlaylistDialog = true
                    }
                },
                onDeleteSelectedClick = {
                    if (selectedTracksOrdered.isNotEmpty()) {
                        showBatchDeleteDialog = true
                    }
                },
                onExitSelectionMode = exitSelectionMode
            )
        }

        ReorderableTrackListBox(
            controller = reorderController,
            tracks = sortedTracks,
            onMoveTrack = { movedId, targetId ->
                val fullCurrentSorted = TrackSortStore.sortTracks(
                    tracks = musicTracks,
                    option = selectedSort,
                    customOrderIds = customOrderIds,
                    dateLookupMs = { DeviceAudioScanner.resolveTrackDateMs(context, it) }
                )
                val newOrder = TrackSortStore.moveTrackInOrder(
                    baseOrderIds = customOrderIds,
                    fallbackTracksInCurrentSort = fullCurrentSorted,
                    movedTrackId = movedId,
                    targetTrackId = targetId
                )
                TrackSortStore.saveMusicCustomOrder(context, newOrder)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("library_tracks_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 148.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Hero Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .naghamDesignBorder(shapeOverride = designTokens.heroShape)
                            .clip(designTokens.heroShape)
                            .border(1.dp, extColors.cardBorder, designTokens.heroShape)
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
                                Text(
                                    text = strings.navLibrary,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = extColors.onHeroOverlay
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (onOpenPlayer != null) {
                                        Surface(
                                            onClick = onOpenPlayer,
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                                            shape = designTokens.pillShape,
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("library_open_player_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.PlayCircle,
                                                    contentDescription = strings.navPlayer,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = strings.navPlayer,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.90f),
                                        shape = designTokens.pillShape
                                    ) {
                                        Text(
                                            text = strings.tracksCount(musicTracks.size),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
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

                // Quran Isolation Notice Pill
                if (isolatedQuranCount > 0) {
                    item {
                        Surface(
                            onClick = onJumpToQuranSanctuary,
                            shape = designTokens.cardShape,
                            color = designTokens.cardContainerColor(extColors.quranContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .naghamDesignBorder(accentColor = extColors.quranAccent)
                                .testTag("quran_isolation_notice_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VerifiedUser,
                                    contentDescription = null,
                                    tint = extColors.quranAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = strings.quranIsolatedNotice(isolatedQuranCount),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = extColors.onQuranContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Search Field
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(strings.searchPlaceholder) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        },
                        singleLine = true,
                        shape = designTokens.pillShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("library_search_input")
                    )
                }

                // Unified Single-Row Primary Toolbar (Select • Folders • Sort • Category)
                // All sub-options remain hidden until their button is clicked!
                item {
                    TrackSortBar(
                        selectedSort = selectedSort,
                        onSelectSort = { option ->
                            val seedIds = sortedTracks.map { it.id } + musicTracks.map { it.id }
                            TrackSortStore.setMusicSortOption(context, option, seedIds)
                        },
                        scopeTagPrefix = "library",
                        isSelectionMode = isSelectionMode,
                        onToggleSelectionMode = {
                            if (isSelectionMode) {
                                exitSelectionMode()
                            } else {
                                isSelectionMode = true
                            }
                        },
                        onSelectAllRequest = {
                            selectedIds = sortedTracks.map { it.id }.toSet()
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
                        },
                        categoryOptions = categoryOptions,
                        selectedCategoryId = selectedFilter,
                        onSelectCategory = { catId -> selectedFilter = catId }
                    )
                }

                if (sortedTracks.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .naghamDesignBorder(),
                            colors = CardDefaults.cardColors(
                                containerColor = designTokens.cardContainerColor(MaterialTheme.colorScheme.surfaceVariant)
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
                    items(sortedTracks, key = { it.id }) { track ->
                        MusicTrackCard(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            isPlaying = track.id == currentTrackId && isPlaying,
                            isArabic = strings.isArabic,
                            isSelectionMode = isSelectionMode,
                            isSelected = track.id in selectedIds,
                            onToggleSelect = { toggleTrackSelection(track.id) },
                            dragHandleController = if (isSelectionMode) null else reorderController,
                            onClick = {
                                if (isSelectionMode) {
                                    toggleTrackSelection(track.id)
                                } else {
                                    onPlayMusicTrack(sortedTracks, track.id)
                                }
                            },
                            onLongClick = {
                                if (isSelectionMode) {
                                    toggleTrackSelection(track.id)
                                } else {
                                    onLongPressTrack(track)
                                }
                            },
                            onToggleFavorite = { onToggleFavorite(track) },
                            onEditClick = { onEditTrack(track) }
                        )
                    }
                }
            }
        }
    }

    if (showExcludedFoldersDialog) {
        ManageExcludedFoldersDialog(
            onDismiss = { showExcludedFoldersDialog = false }
        )
    }

    if (showBatchPlaylistDialog && selectedTracksOrdered.isNotEmpty()) {
        BatchAddToPlaylistDialog(
            selectedTracks = selectedTracksOrdered,
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

    if (showBatchDeleteDialog && selectedTracksOrdered.isNotEmpty()) {
        BatchDeleteConfirmDialog(
            selectedCount = selectedTracksOrdered.size,
            onDismiss = { showBatchDeleteDialog = false },
            onConfirmDelete = {
                onBatchDeleteTracks(selectedTracksOrdered)
                exitSelectionMode()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicTrackCard(
    track: CloudTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isArabic: Boolean,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    dragHandleController: ReorderableTrackListController? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onEditClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val extColors = LocalNaghamColors.current
    val designTokens = LocalNaghamDesign.current
    val localMeta = remember(track.sourceUri, track.album) {
        DeviceAudioScanner.resolveMetaForTrack(context, track)
    }
    val itemAccent = if (track.quranAudio) extColors.quranAccent else MaterialTheme.colorScheme.primary
    val highlighted = isCurrent || isSelected

    val baseContainer = when {
        isSelected -> itemAccent.copy(alpha = 0.24f)
        isCurrent && track.quranAudio -> extColors.quranContainer
        isCurrent -> MaterialTheme.colorScheme.primaryContainer
        track.quranAudio -> extColors.quranContainer
        designTokens.style == AppDesignStyle.GLASS -> Color.White.copy(alpha = 0.05f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .reorderableTrackItem(track.id, dragHandleController)
            .naghamDesignBorder(accentColor = itemAccent, isHighlighted = highlighted)
            .clip(designTokens.cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .testTag("track_item_card_${track.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected || designTokens.style == AppDesignStyle.GLASS && !highlighted) {
                baseContainer
            } else {
                designTokens.cardContainerColor(baseContainer, highlighted)
            }
        ),
        shape = designTokens.cardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                TrackSelectionCheckbox(
                    trackId = track.id,
                    selected = isSelected,
                    accentColor = itemAccent,
                    onToggle = onToggleSelect
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else if (dragHandleController != null) {
                TrackDragHandle(
                    trackId = track.id,
                    controller = dragHandleController,
                    tint = if (track.quranAudio) extColors.quranAccent else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else {
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Rounded-square album/folder artwork (Image 1 Right)
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                TrackArtwork(
                    track = track,
                    shape = RoundedCornerShape(14.dp),
                    showFolderBadge = false,
                    modifier = Modifier.fillMaxSize()
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = itemAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Clean stacked Title + Artist (Image 1 Right — no cluttered chip under every track)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (highlighted) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (highlighted) itemAccent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = listOfNotNull(
                    track.artist.takeIf { it.isNotBlank() },
                    localMeta.folderName.takeIf { it.isNotBlank() && it != track.artist }
                ).joinToString(" • ")
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (track.durationMs > 0L) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatDurationMs(track.durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isSelectionMode) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("fav_button_${track.id}")
                ) {
                    Icon(
                        imageVector = if (track.favorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (track.favorite) itemAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.testTag("edit_track_button_${track.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Track & Lyrics",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

fun parseSafeHexColor(hex: String, fallback: Color = AmberGoldPrimary): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}
