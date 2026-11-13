package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CloudPlaylist
import com.example.data.model.CloudTrack
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.LocalNaghamDesign
import com.example.ui.theme.naghamDesignBorder

@Composable
fun MultiSelectActionBar(
    selectedCount: Int,
    totalVisibleCount: Int,
    scopeTagPrefix: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onSelectAllOrClear: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onDeleteSelectedClick: () -> Unit,
    onRemoveFromPlaylistClick: (() -> Unit)? = null,
    onExitSelectionMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val tokens = LocalNaghamDesign.current
    val allSelected = totalVisibleCount > 0 && selectedCount >= totalVisibleCount

    Surface(
        shape = tokens.cardShape,
        color = tokens.cardContainerColor(
            base = MaterialTheme.colorScheme.surfaceContainerHigh,
            isHighlighted = true
        ),
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .naghamDesignBorder(accentColor = accentColor, isHighlighted = true)
            .testTag("${scopeTagPrefix}_selection_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onExitSelectionMode,
                        modifier = Modifier.testTag("${scopeTagPrefix}_exit_selection_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.cancelAction
                        )
                    }
                    Text(
                        text = strings.selectedFilesCount(selectedCount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TextButton(
                    onClick = onSelectAllOrClear,
                    modifier = Modifier.testTag("${scopeTagPrefix}_select_all_button")
                ) {
                    Icon(
                        imageVector = if (allSelected) Icons.Default.RemoveDone else Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (allSelected) strings.clearSelectionBtn else strings.selectAllBtn,
                        color = accentColor
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddToPlaylistClick,
                    enabled = selectedCount > 0,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${scopeTagPrefix}_batch_add_playlist_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.batchAddToPlaylistBtn,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onRemoveFromPlaylistClick != null) {
                    OutlinedButton(
                        onClick = onRemoveFromPlaylistClick,
                        enabled = selectedCount > 0,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${scopeTagPrefix}_batch_remove_playlist_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.batchRemoveFromPlaylistBtn,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onDeleteSelectedClick,
                    enabled = selectedCount > 0,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${scopeTagPrefix}_batch_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.batchDeleteBtn,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TrackSelectionCheckbox(
    trackId: String,
    selected: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(48.dp)
            .padding(4.dp)
            .clip(CircleShape)
            .background(
                if (selected) accentColor.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
            )
            .semantics(mergeDescendants = true) {}
            .clickable(onClick = onToggle)
            .testTag("select_checkbox_$trackId"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = strings.selectModeBtn,
            tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun BatchAddToPlaylistDialog(
    selectedTracks: List<CloudTrack>,
    playlists: List<CloudPlaylist>,
    onDismiss: () -> Unit,
    onAddToExistingPlaylist: (List<CloudTrack>, CloudPlaylist) -> Unit,
    onCreateNewPlaylistWithTracks: (String, List<CloudTrack>) -> Unit
) {
    val strings = LocalAppStrings.current
    val tokens = LocalNaghamDesign.current
    var newPlaylistName by remember { mutableStateOf("") }
    val validPlaylists = remember(playlists) {
        playlists.filter { it.id.isNotBlank() }.distinctBy { it.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = tokens.cardShape,
        modifier = Modifier.testTag("batch_add_to_playlist_dialog"),
        title = {
            Text(
                text = strings.batchAddDialogTitle(selectedTracks.size),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (validPlaylists.isEmpty()) {
                    Text(
                        text = strings.noCompatiblePlaylists,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 210.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(validPlaylists, key = { it.id }) { pl ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(tokens.cardShape)
                                    .clickable {
                                        onAddToExistingPlaylist(selectedTracks, pl)
                                        onDismiss()
                                    }
                                    .testTag("batch_playlist_item_${pl.id}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                shape = tokens.cardShape
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
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text(strings.quickNewPlaylistHint) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("batch_new_playlist_input")
                    )
                    Button(
                        onClick = {
                            val fallback = selectedTracks.firstOrNull()?.title?.take(24) ?: "Nagham Mix"
                            val finalName = newPlaylistName.trim().ifBlank { fallback }
                            onCreateNewPlaylistWithTracks(finalName, selectedTracks)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("batch_create_playlist_confirm_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.saveAction, maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelAction)
            }
        }
    )
}

@Composable
fun BatchDeleteConfirmDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    val tokens = LocalNaghamDesign.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = tokens.cardShape,
        modifier = Modifier.testTag("batch_delete_confirm_dialog"),
        title = { Text(strings.batchDeleteConfirmTitle) },
        text = { Text(strings.batchDeleteConfirmMsg(selectedCount)) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmDelete()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_batch_delete_button")
            ) {
                Text(strings.batchDeleteBtn)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_batch_delete_button")
            ) {
                Text(strings.cancelAction)
            }
        }
    )
}
