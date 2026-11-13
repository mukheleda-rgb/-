package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CloudPlaylist
import com.example.data.model.CloudTrack
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.LocalNaghamColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackLongPressSheet(
    track: CloudTrack,
    playlists: List<CloudPlaylist>,
    onDismiss: () -> Unit,
    onAddTrackToPlaylist: (CloudTrack, CloudPlaylist) -> Unit,
    onCreatePlaylistWithTrack: (String, CloudTrack) -> Unit,
    onMoveTrackIsolation: (CloudTrack, Boolean) -> Unit,
    onEditTrack: (CloudTrack) -> Unit,
    onDeleteTrack: (CloudTrack) -> Unit,
    onSelectMultipleFromHere: ((CloudTrack) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        TrackLongPressSheetContent(
            track = track,
            playlists = playlists,
            onDismiss = onDismiss,
            onAddTrackToPlaylist = onAddTrackToPlaylist,
            onCreatePlaylistWithTrack = onCreatePlaylistWithTrack,
            onMoveTrackIsolation = onMoveTrackIsolation,
            onEditTrack = onEditTrack,
            onDeleteTrack = onDeleteTrack,
            onSelectMultipleFromHere = onSelectMultipleFromHere
        )
    }
}

@Composable
fun TrackLongPressSheetContent(
    track: CloudTrack,
    playlists: List<CloudPlaylist>,
    onDismiss: () -> Unit,
    onAddTrackToPlaylist: (CloudTrack, CloudPlaylist) -> Unit,
    onCreatePlaylistWithTrack: (String, CloudTrack) -> Unit,
    onMoveTrackIsolation: (CloudTrack, Boolean) -> Unit,
    onEditTrack: (CloudTrack) -> Unit,
    onDeleteTrack: (CloudTrack) -> Unit,
    onSelectMultipleFromHere: ((CloudTrack) -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    val extColors = LocalNaghamColors.current
    var selectingPlaylist by remember(track.id) { mutableStateOf(false) }
    var newPlaylistName by remember(track.id) { mutableStateOf("") }

    val compatiblePlaylists = remember(playlists) {
        playlists.filter { it.id.isNotBlank() }.distinctBy { it.id }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("track_long_press_sheet"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Track Header Summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectingPlaylist) {
                IconButton(
                    onClick = { selectingPlaylist = false },
                    modifier = Modifier.testTag("sheet_back_to_actions_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.cancelAction
                    )
                }
            }
            TrackArtwork(
                track = track,
                shape = RoundedCornerShape(14.dp),
                showFolderBadge = false,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (selectingPlaylist) strings.selectPlaylistTitle else track.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} • ${if (track.quranAudio) strings.navQuran else strings.navLibrary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        if (!selectingPlaylist) {
            if (onSelectMultipleFromHere != null) {
                SheetActionRow(
                    icon = Icons.Default.Checklist,
                    label = strings.actionSelectMultiple,
                    tint = MaterialTheme.colorScheme.secondary,
                    testTag = "long_press_select_multiple",
                    onClick = {
                        onSelectMultipleFromHere(track)
                        onDismiss()
                    }
                )
            }

            // 1. Add to Playlist
            SheetActionRow(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                label = strings.actionAddToPlaylist,
                tint = MaterialTheme.colorScheme.primary,
                testTag = "long_press_add_to_playlist",
                onClick = { selectingPlaylist = true }
            )

            // 2. Move between Music Tab and Isolated Quran Tab
            if (track.quranAudio) {
                SheetActionRow(
                    icon = Icons.Default.LibraryMusic,
                    label = strings.actionMoveToMusic,
                    tint = MaterialTheme.colorScheme.primary,
                    testTag = "long_press_move_category",
                    onClick = {
                        onMoveTrackIsolation(track, false)
                        onDismiss()
                    }
                )
            } else {
                SheetActionRow(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = strings.actionMoveToQuran,
                    tint = extColors.quranAccent,
                    testTag = "long_press_move_category",
                    onClick = {
                        onMoveTrackIsolation(track, true)
                        onDismiss()
                    }
                )
            }

            // 3. Edit Details & Lyrics
            SheetActionRow(
                icon = Icons.Default.Edit,
                label = strings.actionEditTrack,
                tint = MaterialTheme.colorScheme.onSurface,
                testTag = "long_press_edit_track",
                onClick = {
                    onEditTrack(track)
                    onDismiss()
                }
            )

            // 4. Delete Track
            SheetActionRow(
                icon = Icons.Default.DeleteOutline,
                label = strings.actionDeleteTrack,
                tint = MaterialTheme.colorScheme.error,
                testTag = "long_press_delete_track",
                onClick = {
                    onDeleteTrack(track)
                    onDismiss()
                }
            )
        } else {
            if (compatiblePlaylists.isEmpty()) {
                Text(
                    text = strings.noCompatiblePlaylists,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(compatiblePlaylists, key = { it.id }) { pl ->
                        val alreadyIn = track.id in pl.trackIds
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onAddTrackToPlaylist(track, pl)
                                    onDismiss()
                                }
                                .testTag("sheet_playlist_item_${pl.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pl.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = strings.tracksCount(pl.trackIds.size),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (alreadyIn) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = strings.alreadyInPlaylistBadge,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Create New Playlist with this Track
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text(strings.quickNewPlaylistHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_new_playlist_input")
                )
                Button(
                    onClick = {
                        val targetName = newPlaylistName.trim().ifBlank { track.title.take(30) }
                        onCreatePlaylistWithTrack(targetName, track)
                        onDismiss()
                    },
                    modifier = Modifier.testTag("quick_create_playlist_with_track_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.quickNewPlaylistBtn, maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = tint.takeIf { it == MaterialTheme.colorScheme.error }
                    ?: MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
