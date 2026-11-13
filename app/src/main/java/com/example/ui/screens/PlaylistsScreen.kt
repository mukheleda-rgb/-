package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CloudPlaylist
import com.example.data.model.CloudTrack
import com.example.data.scanner.DeviceAudioScanner
import com.example.data.scanner.TrackSortOption
import com.example.data.scanner.TrackSortStore
import com.example.ui.components.BatchAddToPlaylistDialog
import com.example.ui.components.BatchDeleteConfirmDialog
import com.example.ui.components.MultiSelectActionBar
import com.example.ui.components.ReorderableTrackListBox
import com.example.ui.components.TrackSortBar
import com.example.ui.components.rememberReorderableTrackListController
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.LocalNaghamDesign
import com.example.ui.theme.naghamDesignBorder

@Composable
fun PlaylistsScreen(
    playlists: List<CloudPlaylist>,
    allTracks: List<CloudTrack>,
    currentTrackId: String? = null,
    isPlaying: Boolean = false,
    onPlayMusicQueue: (List<CloudTrack>, String?) -> Unit,
    onCreatePlaylist: (String, String, List<String>) -> Unit,
    onUpdatePlaylistTracks: (CloudPlaylist, List<String>) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onOpenImportExternalDialog: () -> Unit,
    onEditTrack: (CloudTrack) -> Unit,
    onToggleFavorite: (CloudTrack) -> Unit,
    onLongPressTrack: (CloudTrack) -> Unit = {},
    onBatchDeleteTracks: (List<CloudTrack>) -> Unit = {},
    onBatchAddTracksToPlaylist: (List<CloudTrack>, CloudPlaylist) -> Unit = { _, _ -> },
    onBatchCreatePlaylistWithTracks: (String, List<CloudTrack>) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
    isActiveTab: Boolean = true
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val designTokens = LocalNaghamDesign.current

    LaunchedEffect(context) {
        TrackSortStore.ensureLoaded(context)
    }

    val playlistSortMap by TrackSortStore.playlistSortOptions.collectAsStateWithLifecycle()

    // Keep ALL playlists and ALL tracks (both Songs and Holy Quran Surahs) intact!
    val validPlaylists = remember(playlists) {
        playlists.filter { it.id.isNotBlank() }.distinctBy { it.id }
    }
    val selectableTracks = remember(allTracks) {
        allTracks.filter { it.id.isNotBlank() }.distinctBy { it.id }
    }
    val tracksById = remember(selectableTracks) {
        selectableTracks.associateBy { it.id }
    }

    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showManageTracksDialog by remember { mutableStateOf<CloudPlaylist?>(null) }

    val selectedPlaylist = validPlaylists.firstOrNull { it.id == selectedPlaylistId }
    BackHandler(enabled = isActiveTab && selectedPlaylist == null, onBack = onBack)

    AnimatedContent(
        targetState = selectedPlaylist,
        transitionSpec = {
            val openingDetail = targetState != null && initialState == null
            (slideInHorizontally(tween(240)) { if (openingDetail) it / 4 else -it / 4 } + fadeIn(tween(220)))
                .togetherWith(
                    slideOutHorizontally(tween(200)) { if (openingDetail) -it / 4 else it / 4 } + fadeOut(tween(180))
                )
        },
        contentKey = { it?.id ?: "playlists_overview" },
        label = "playlists_detail_transition",
        modifier = Modifier.fillMaxSize()
    ) { activePlaylist ->
        if (activePlaylist != null) {
            var isSelectionMode by remember(activePlaylist.id) { mutableStateOf(false) }
            var selectedTrackIds by remember(activePlaylist.id) { mutableStateOf<Set<String>>(emptySet()) }
            var showBatchPlaylistDialog by remember(activePlaylist.id) { mutableStateOf(false) }
            var showBatchDeleteDialog by remember(activePlaylist.id) { mutableStateOf(false) }

            val exitSelectionMode: () -> Unit = {
                isSelectionMode = false
                selectedTrackIds = emptySet()
            }

            BackHandler(enabled = isActiveTab && !isSelectionMode) { selectedPlaylistId = null }
            BackHandler(enabled = isActiveTab && isSelectionMode, onBack = exitSelectionMode)

            val detailLazyListState = rememberLazyListState()
            val detailReorderController = rememberReorderableTrackListController(detailLazyListState)

            // Optimistic ordered track IDs during live drag so swaps reflect instantaneously
            var orderedIds by remember(activePlaylist.id) {
                mutableStateOf(activePlaylist.trackIds.distinct())
            }
            LaunchedEffect(activePlaylist.id, activePlaylist.trackIds) {
                if (detailReorderController.draggedTrackId == null) {
                    orderedIds = activePlaylist.trackIds.distinct()
                }
            }

            val currentSort = TrackSortStore.getPlaylistSortOption(activePlaylist.id, playlistSortMap)
            val rawPlaylistTracks = remember(orderedIds, tracksById) {
                orderedIds.mapNotNull { tracksById[it] }
            }
            val sortedPlaylistTracks = remember(rawPlaylistTracks, currentSort, orderedIds) {
                TrackSortStore.sortTracks(
                    tracks = rawPlaylistTracks,
                    option = currentSort,
                    customOrderIds = orderedIds,
                    dateLookupMs = { DeviceAudioScanner.resolveTrackDateMs(context, it) }
                )
            }

            val selectedTracksOrdered = remember(sortedPlaylistTracks, selectedTrackIds) {
                sortedPlaylistTracks.filter { it.id in selectedTrackIds }
            }

            val toggleTrackSelect: (String) -> Unit = { id ->
                selectedTrackIds = if (id in selectedTrackIds) selectedTrackIds - id else selectedTrackIds + id
            }

            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    MultiSelectActionBar(
                        selectedCount = selectedTrackIds.size,
                        totalVisibleCount = sortedPlaylistTracks.size,
                        scopeTagPrefix = "playlist",
                        onSelectAllOrClear = {
                            val visibleIds = sortedPlaylistTracks.map { it.id }.toSet()
                            selectedTrackIds = if (visibleIds.isNotEmpty() && visibleIds.all { it in selectedTrackIds }) {
                                emptySet()
                            } else {
                                visibleIds
                            }
                        },
                        onAddToPlaylistClick = {
                            if (selectedTracksOrdered.isNotEmpty()) {
                                showBatchPlaylistDialog = true
                            }
                        },
                        onRemoveFromPlaylistClick = {
                            if (selectedTrackIds.isNotEmpty()) {
                                val remaining = orderedIds.filterNot { it in selectedTrackIds }
                                orderedIds = remaining
                                onUpdatePlaylistTracks(activePlaylist, remaining)
                                exitSelectionMode()
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
                    controller = detailReorderController,
                    tracks = sortedPlaylistTracks,
                    onMoveTrack = { movedId, targetId ->
                        val newOrder = TrackSortStore.moveTrackInOrder(
                            baseOrderIds = if (currentSort == TrackSortOption.CUSTOM) orderedIds else sortedPlaylistTracks.map { it.id },
                            fallbackTracksInCurrentSort = sortedPlaylistTracks,
                            movedTrackId = movedId,
                            targetTrackId = targetId
                        )
                        orderedIds = newOrder
                        TrackSortStore.setPlaylistSortOption(context, activePlaylist.id, TrackSortOption.CUSTOM)
                        onUpdatePlaylistTracks(activePlaylist, newOrder)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        state = detailLazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("playlist_detail_tracks_list"),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 148.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { selectedPlaylistId = null },
                                    modifier = Modifier.testTag("playlist_detail_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activePlaylist.name,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "${strings.tracksCount(sortedPlaylistTracks.size)} • ${activePlaylist.sourceApp}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onDeletePlaylist(activePlaylist.id)
                                        selectedPlaylistId = null
                                    },
                                    modifier = Modifier.testTag("delete_playlist_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = strings.deleteAction,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        if (activePlaylist.description.isNotBlank()) {
                            item {
                                Text(
                                    text = activePlaylist.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (sortedPlaylistTracks.isNotEmpty()) {
                                            onPlayMusicQueue(sortedPlaylistTracks, sortedPlaylistTracks.first().id)
                                        }
                                    },
                                    enabled = sortedPlaylistTracks.isNotEmpty(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("play_playlist_all_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.playAll)
                                }
                                FilledTonalButton(
                                    onClick = { showManageTracksDialog = activePlaylist },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("manage_playlist_tracks_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.addTracksToPlaylist)
                                }
                            }
                        }

                        item {
                            TrackSortBar(
                                selectedSort = currentSort,
                                onSelectSort = { option ->
                                    TrackSortStore.setPlaylistSortOption(context, activePlaylist.id, option)
                                },
                                scopeTagPrefix = "playlist",
                                isSelectionMode = isSelectionMode,
                                onToggleSelectionMode = {
                                    if (isSelectionMode) {
                                        exitSelectionMode()
                                    } else {
                                        isSelectionMode = true
                                    }
                                },
                                onSelectAllRequest = {
                                    selectedTrackIds = sortedPlaylistTracks.map { it.id }.toSet()
                                }
                            )
                        }

                        items(sortedPlaylistTracks, key = { it.id }) { track ->
                            MusicTrackCard(
                                track = track,
                                isCurrent = track.id == currentTrackId,
                                isPlaying = track.id == currentTrackId && isPlaying,
                                isArabic = strings.isArabic,
                                isSelectionMode = isSelectionMode,
                                isSelected = track.id in selectedTrackIds,
                                onToggleSelect = { toggleTrackSelect(track.id) },
                                dragHandleController = if (isSelectionMode) null else detailReorderController,
                                onClick = {
                                    if (isSelectionMode) {
                                        toggleTrackSelect(track.id)
                                    } else {
                                        onPlayMusicQueue(sortedPlaylistTracks, track.id)
                                    }
                                },
                                onLongClick = {
                                    if (isSelectionMode) {
                                        toggleTrackSelect(track.id)
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

            if (showBatchPlaylistDialog && selectedTracksOrdered.isNotEmpty()) {
                BatchAddToPlaylistDialog(
                    selectedTracks = selectedTracksOrdered,
                    playlists = validPlaylists.filter { it.id != activePlaylist.id },
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
                        val remaining = orderedIds.filterNot { it in selectedTrackIds }
                        orderedIds = remaining
                        onBatchDeleteTracks(selectedTracksOrdered)
                        exitSelectionMode()
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("playlists_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 148.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = strings.navPlaylists,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_playlist_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.createPlaylist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = onOpenImportExternalDialog,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_playlist_button")
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.importExternalPlaylist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                items(validPlaylists, key = { it.id }) { playlist ->
                    val resolvedTracks = playlist.trackIds.distinct().mapNotNull { tracksById[it] }
                    val coverColor = parseSafeHexColor(playlist.coverHex)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .naghamDesignBorder(accentColor = coverColor)
                            .clip(designTokens.cardShape)
                            .clickable { selectedPlaylistId = playlist.id }
                            .testTag("playlist_card_${playlist.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = designTokens.cardContainerColor(MaterialTheme.colorScheme.surfaceVariant)
                        ),
                        shape = designTokens.cardShape
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(coverColor.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = coverColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (playlist.description.isNotBlank()) {
                                    Text(
                                        text = playlist.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${strings.tracksCount(resolvedTracks.size)} • ${playlist.sourceApp}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = {
                                    val sortOpt = TrackSortStore.getPlaylistSortOption(playlist.id, playlistSortMap)
                                    val sortedToPlay = TrackSortStore.sortTracks(
                                        tracks = resolvedTracks,
                                        option = sortOpt,
                                        customOrderIds = playlist.trackIds,
                                        dateLookupMs = { DeviceAudioScanner.resolveTrackDateMs(context, it) }
                                    )
                                    if (sortedToPlay.isNotEmpty()) {
                                        onPlayMusicQueue(sortedToPlay, sortedToPlay.first().id)
                                    }
                                },
                                modifier = Modifier.testTag("quick_play_playlist_${playlist.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = strings.playAll,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        val selectedIds = remember { mutableStateListOf<String>() }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            shape = designTokens.cardShape,
            title = { Text(strings.createPlaylist) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(strings.playlistNameHint) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_name_input")
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(strings.playlistDescHint) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = strings.addTracksToPlaylist,
                        style = MaterialTheme.typography.labelLarge
                    )
                    LazyColumn(modifier = Modifier.height(170.dp)) {
                        items(selectableTracks, key = { it.id }) { t ->
                            val checked = t.id in selectedIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selectedIds.remove(t.id) else selectedIds.add(t.id)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreatePlaylist(name, desc, selectedIds.toList())
                            showCreateDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_playlist_button")
                ) {
                    Text(strings.saveAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(strings.cancelAction)
                }
            }
        )
    }

    showManageTracksDialog?.let { targetPlaylist ->
        val selectedIds = remember(targetPlaylist) {
            mutableStateListOf<String>().apply { addAll(targetPlaylist.trackIds) }
        }
        AlertDialog(
            onDismissRequest = { showManageTracksDialog = null },
            shape = designTokens.cardShape,
            title = { Text(strings.addTracksToPlaylist) },
            text = {
                LazyColumn(modifier = Modifier.height(240.dp)) {
                    items(selectableTracks, key = { it.id }) { t ->
                        val checked = t.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selectedIds.remove(t.id) else selectedIds.add(t.id)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(t.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${t.artist} • ${if (t.quranAudio) strings.navQuran else strings.navLibrary}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePlaylistTracks(targetPlaylist, selectedIds.toList())
                        showManageTracksDialog = null
                    }
                ) {
                    Text(strings.saveAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManageTracksDialog = null }) {
                    Text(strings.cancelAction)
                }
            }
        )
    }
}
