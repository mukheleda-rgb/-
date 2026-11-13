package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CloudTrack
import com.example.data.model.TrackCategory
import com.example.data.scanner.DeviceLibraryPlaylist
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.LocalNaghamDesign

@Composable
fun TrackEditorDialog(
    track: CloudTrack,
    isDownloadingLyrics: Boolean,
    onDismiss: () -> Unit,
    onSave: (CloudTrack) -> Unit,
    onDelete: (String) -> Unit,
    onDownloadLyrics: (String, String, (Result<String>) -> Unit) -> Unit,
    onPickLrcFileRequest: ((String) -> Unit) -> Unit
) {
    val strings = LocalAppStrings.current
    val designTokens = LocalNaghamDesign.current
    var title by remember(track) { mutableStateOf(track.title) }
    var artist by remember(track) { mutableStateOf(track.artist) }
    var album by remember(track) { mutableStateOf(track.album) }
    var genre by remember(track) { mutableStateOf(track.genre) }
    var categoryId by remember(track) { mutableStateOf(track.category) }
    var isQuran by remember(track) { mutableStateOf(track.quranAudio) }
    var lyrics by remember(track) { mutableStateOf(track.lyrics) }
    var downloadStatus by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = designTokens.cardShape,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.editTrackTitle, style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.cancelAction)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.fieldTitle) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_track_title_input")
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(strings.fieldArtist) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_track_artist_input")
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text(strings.fieldAlbum) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text(strings.fieldGenre) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Holy Quran Isolation Switch
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.isolateAsQuranLabel, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = strings.isolateAsQuranHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isQuran,
                            onCheckedChange = { checked ->
                                isQuran = checked
                                categoryId = if (checked) TrackCategory.QURAN.id else TrackCategory.ARABIC_TARAB.id
                            },
                            modifier = Modifier.testTag("quran_isolation_switch")
                        )
                    }
                }

                if (!isQuran) {
                    Text(strings.fieldCategory, style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TrackCategory.entries
                            .filter { it != TrackCategory.QURAN }
                            .forEach { cat ->
                                FilterChip(
                                    selected = categoryId == cat.id,
                                    onClick = { categoryId = cat.id },
                                    label = { Text(if (strings.isArabic) cat.labelAr else cat.labelEn) }
                                )
                            }
                    }
                }

                // Lyrics Download & Edit Section
                Text(strings.lyricsSectionTitle, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            downloadStatus = null
                            onDownloadLyrics(title, artist) { res ->
                                res.fold(
                                    onSuccess = { fetched ->
                                        lyrics = fetched
                                        downloadStatus = if (strings.isArabic) "تم تحميل الكلمات بنجاح!" else "Lyrics downloaded!"
                                    },
                                    onFailure = { err ->
                                        downloadStatus = err.localizedMessage ?: "Lyrics not found"
                                    }
                                )
                            }
                        },
                        enabled = !isDownloadingLyrics,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_lyrics_button")
                    ) {
                        if (isDownloadingLyrics) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.downloadLyricsBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = {
                            onPickLrcFileRequest { importedText ->
                                lyrics = importedText
                            }
                        }
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.importLrcFileBtn)
                    }
                }

                if (downloadStatus != null) {
                    Text(
                        text = downloadStatus.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = lyrics,
                    onValueChange = { lyrics = it },
                    placeholder = { Text(strings.lyricsPlaceholder) },
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_track_lyrics_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalCategory = if (isQuran) TrackCategory.QURAN.id else categoryId
                    onSave(
                        track.copy(
                            title = title,
                            artist = artist,
                            album = album,
                            genre = genre,
                            category = finalCategory,
                            quranAudio = isQuran,
                            lyrics = lyrics,
                            accentHex = if (isQuran) "#38B28B" else track.accentHex
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("save_track_details_button")
            ) {
                Text(strings.saveAction)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDelete(track.id)
                    onDismiss()
                }
            ) {
                Text(strings.deleteAction, color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
fun ExternalPlaylistImportDialog(
    devicePlaylists: List<DeviceLibraryPlaylist> = emptyList(),
    onImportDevicePlaylist: (DeviceLibraryPlaylist, String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onPickFile: (String) -> Unit,
    onImportRawText: (String, String) -> Unit
) {
    val strings = LocalAppStrings.current
    val designTokens = LocalNaghamDesign.current
    var playlistName by remember { mutableStateOf("") }
    var rawText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = designTokens.cardShape,
        title = { Text(strings.importDialogTitle) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.importDialogDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text(strings.playlistNameHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (devicePlaylists.isNotEmpty()) {
                    Text(
                        text = strings.deviceLibraryPlaylistsTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(devicePlaylists, key = { it.id }) { devPl ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        onImportDevicePlaylist(devPl, playlistName)
                                        onDismiss()
                                    }
                                    .testTag("device_playlist_item_${devPl.id}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = devPl.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (devPl.memberAudioIds.isNotEmpty()) {
                                            Text(
                                                text = strings.tracksCount(devPl.memberAudioIds.size),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = strings.importNow,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                FilledTonalButton(
                    onClick = { onPickFile(playlistName) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_m3u_file_button")
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.pickM3uFile)
                }

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text(strings.pasteSharedTextHint) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("paste_playlist_text_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rawText.isNotBlank()) {
                        onImportRawText(rawText, playlistName)
                        onDismiss()
                    }
                },
                enabled = rawText.isNotBlank(),
                modifier = Modifier.testTag("confirm_import_playlist_button")
            ) {
                Text(strings.importNow)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelAction)
            }
        }
    )
}

fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return String.format("%02d:%02d", min, sec)
}
