package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.scanner.FolderFilterStore
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.LocalNaghamDesign

private val COMMON_UNWANTED_FOLDER_PRESETS = listOf(
    "WhatsApp Audio",
    "WhatsApp Voice Notes",
    "Telegram Audio",
    "Recordings",
    "CallRecordings",
    "Notifications",
    "Ringtones"
)

@Composable
fun ManageExcludedFoldersDialog(
    onDismiss: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val designTokens = LocalNaghamDesign.current

    LaunchedEffect(context) {
        FolderFilterStore.ensureLoaded(context)
    }

    val autoExclude by FolderFilterStore.autoExcludeUnwanted.collectAsStateWithLifecycle()
    val customExcluded by FolderFilterStore.customExcludedFolders.collectAsStateWithLifecycle()
    val allowedOverrides by FolderFilterStore.explicitlyAllowedFolders.collectAsStateWithLifecycle()
    val discoveredFolders by FolderFilterStore.discoveredFolders.collectAsStateWithLifecycle()

    val allKnownFolders = remember(discoveredFolders, customExcluded, allowedOverrides) {
        (discoveredFolders + customExcluded + allowedOverrides + COMMON_UNWANTED_FOLDER_PRESETS)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = designTokens.cardShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.testTag("manage_excluded_folders_dialog"),
        title = {
            Text(
                text = strings.excludedFoldersDialogTitle,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = designTokens.cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.excludeUnwantedFoldersTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = strings.excludeUnwantedFoldersDesc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = autoExclude,
                            onCheckedChange = { enabled ->
                                FolderFilterStore.setAutoExcludeUnwanted(context, enabled)
                            },
                            modifier = Modifier.testTag("dialog_auto_exclude_unwanted_switch")
                        )
                    }
                }

                Text(
                    text = strings.excludedFoldersDialogDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .testTag("excluded_folders_dialog_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allKnownFolders, key = { it.lowercase() }) { folder ->
                        val isHidden = FolderFilterStore.isFolderExcluded(
                            folderName = folder,
                            autoExclude = autoExclude,
                            customExcluded = customExcluded,
                            allowedOverrides = allowedOverrides
                        )

                        Surface(
                            shape = designTokens.cardShape,
                            color = if (isHidden) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isHidden) {
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                                } else {
                                    accentColor.copy(alpha = 0.25f)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_folder_row_$folder")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isHidden) Icons.Default.FolderOff else Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (isHidden) MaterialTheme.colorScheme.error else accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = folder,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isHidden) {
                                        Text(
                                            text = strings.folderHiddenBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledTonalButton(
                                    onClick = {
                                        if (isHidden) {
                                            FolderFilterStore.restoreFolder(context, folder)
                                        } else {
                                            FolderFilterStore.excludeFolder(context, folder)
                                        }
                                    },
                                    modifier = Modifier.testTag("dialog_toggle_folder_$folder")
                                ) {
                                    Icon(
                                        imageVector = if (isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isHidden) strings.restoreFolderAction else strings.hideFolderAction,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_excluded_folders_dialog_button")
            ) {
                Text(strings.saveAction)
            }
        }
    )
}
