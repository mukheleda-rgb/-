package com.example.data.scanner

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.data.model.AudioClassifier
import com.example.data.model.CloudTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class LocalTrackMediaMeta(
    val folderName: String,
    val albumArtUri: String,
    val dateAddedMs: Long = 0L
)

data class ScannedDeviceTrack(
    val deterministicId: String,
    val track: CloudTrack,
    val folderName: String,
    val albumArtUri: String
)

data class DeviceLibraryPlaylist(
    val id: String,
    val name: String,
    val entryCount: Int,
    val fileUri: Uri? = null,
    val memberAudioIds: List<Long> = emptyList()
)

object DeviceAudioScanner {

    private const val PREFS_INDEX = "nagham_local_media_index"
    private const val PREFS_DELETED = "nagham_deleted_local_tracks"
    private const val KEY_DELETED_SET = "deleted_keys"
    private const val MAX_DURATION_MS = 86_400_000L

    private val memoryMetaMap = ConcurrentHashMap<String, LocalTrackMediaMeta>()
    private val fallbackMetaCache = ConcurrentHashMap<String, LocalTrackMediaMeta>()
    @Volatile
    private var prefsLoaded = false
    private val indexLoadLock = Any()

    fun hasAudioScanPermission(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun markTrackDeletedByUser(context: Context, trackId: String, sourceUri: String) {
        try {
            val appCtx = context.applicationContext ?: context
            val prefs = appCtx.getSharedPreferences(PREFS_DELETED, Context.MODE_PRIVATE)
            val current = prefs.getStringSet(KEY_DELETED_SET, emptySet())?.toMutableSet() ?: mutableSetOf()
            if (trackId.isNotBlank()) current.add("id:$trackId")
            if (sourceUri.isNotBlank()) current.add("uri:$sourceUri")
            prefs.edit().putStringSet(KEY_DELETED_SET, current).apply()
        } catch (_: Throwable) {
        }
    }

    private fun getDeletedSnapshot(context: Context): Set<String> {
        return try {
            val appCtx = context.applicationContext ?: context
            appCtx.getSharedPreferences(PREFS_DELETED, Context.MODE_PRIVATE)
                .getStringSet(KEY_DELETED_SET, emptySet())
                ?.toSet()
                .orEmpty()
        } catch (_: Throwable) {
            emptySet()
        }
    }

    fun isDeletedByUser(context: Context, deterministicId: String, sourceUri: String): Boolean {
        val set = getDeletedSnapshot(context)
        return ("id:$deterministicId" in set) || ("uri:$sourceUri" in set)
    }

    fun deleteLegacyStarterAudioFiles(context: Context) {
        try {
            val dir = File(context.filesDir, "starter_audio")
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        } catch (_: Throwable) {
        }
    }

    private fun ensureIndexLoaded(context: Context) {
        if (prefsLoaded) return
        synchronized(indexLoadLock) {
            if (prefsLoaded) return
            prefsLoaded = true
            try {
                val appCtx = context.applicationContext ?: context
                val prefs = appCtx.getSharedPreferences(PREFS_INDEX, Context.MODE_PRIVATE)
                for ((uriKey, rawJson) in prefs.all) {
                    val str = rawJson as? String ?: continue
                    try {
                        val obj = JSONObject(str)
                        memoryMetaMap[uriKey] = LocalTrackMediaMeta(
                            folderName = obj.optString("f", ""),
                            albumArtUri = obj.optString("a", ""),
                            dateAddedMs = obj.optLong("d", 0L)
                        )
                    } catch (_: Throwable) {
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    fun registerLocalMeta(
        context: Context,
        sourceUri: String,
        folderName: String,
        albumArtUri: String,
        dateAddedMs: Long = System.currentTimeMillis()
    ) {
        if (sourceUri.isBlank()) return
        ensureIndexLoaded(context)
        val existingDate = memoryMetaMap[sourceUri]?.dateAddedMs?.takeIf { it > 0L }
        val resolvedDate = existingDate ?: dateAddedMs.coerceAtLeast(0L)
        val meta = LocalTrackMediaMeta(
            folderName = folderName,
            albumArtUri = albumArtUri,
            dateAddedMs = resolvedDate
        )
        val previous = memoryMetaMap.put(sourceUri, meta)
        if (previous == meta) return
        try {
            val appCtx = context.applicationContext ?: context
            val json = JSONObject().apply {
                put("f", folderName)
                put("a", albumArtUri)
                put("d", resolvedDate)
            }.toString()
            appCtx.getSharedPreferences(PREFS_INDEX, Context.MODE_PRIVATE)
                .edit()
                .putString(sourceUri, json)
                .apply()
        } catch (_: Throwable) {
        }
    }

    private fun registerLocalMetaBatch(context: Context, updates: Map<String, LocalTrackMediaMeta>) {
        if (updates.isEmpty()) return
        ensureIndexLoaded(context)
        val changed = HashMap<String, LocalTrackMediaMeta>()
        for ((uriKey, meta) in updates) {
            if (uriKey.isBlank()) continue
            val prev = memoryMetaMap.put(uriKey, meta)
            if (prev != meta) {
                changed[uriKey] = meta
            }
        }
        if (changed.isEmpty()) return
        try {
            val appCtx = context.applicationContext ?: context
            val editor = appCtx.getSharedPreferences(PREFS_INDEX, Context.MODE_PRIVATE).edit()
            val entriesToWrite = if (memoryMetaMap.size > 4000) {
                changed.entries.take(1500)
            } else {
                changed.entries
            }
            for ((uriKey, meta) in entriesToWrite) {
                val json = JSONObject().apply {
                    put("f", meta.folderName)
                    put("a", meta.albumArtUri)
                    put("d", meta.dateAddedMs)
                }.toString()
                editor.putString(uriKey, json)
            }
            editor.apply()
        } catch (_: Throwable) {
        }
    }

    fun resolveMetaForTrack(context: Context, track: CloudTrack): LocalTrackMediaMeta {
        ensureIndexLoaded(context)
        memoryMetaMap[track.sourceUri]?.let { return it }
        val cacheKey = "${track.id}|${track.sourceUri}|${track.album}|${track.quranAudio}"
        fallbackMetaCache[cacheKey]?.let { return it }

        val decoded = try {
            Uri.decode(track.sourceUri).orEmpty()
        } catch (_: Throwable) {
            track.sourceUri
        }
        val inferredFolder = decoded
            .substringBeforeLast('/', "")
            .substringAfterLast('/', "")
            .substringAfterLast(':', "")
            .trim()
            .ifBlank {
                if (track.album.isNotBlank()) track.album else if (track.quranAudio) "القرآن الكريم" else "Music"
            }

        val resolved = LocalTrackMediaMeta(
            folderName = inferredFolder,
            albumArtUri = "",
            dateAddedMs = track.createdAt?.toDate()?.time ?: 0L
        )
        if (fallbackMetaCache.size > 4000) {
            fallbackMetaCache.clear()
        }
        fallbackMetaCache[cacheKey] = resolved
        return resolved
    }

    fun resolveTrackDateMs(context: Context, track: CloudTrack): Long {
        val localDate = resolveMetaForTrack(context, track).dateAddedMs
        if (localDate > 0L) return localDate
        return track.createdAt?.toDate()?.time ?: 0L
    }

    suspend fun scanPhoneMediaStore(
        context: Context,
        autoIsolateQuran: Boolean
    ): List<ScannedDeviceTrack> = withContext(Dispatchers.IO) {
        deleteLegacyStarterAudioFiles(context)
        ensureIndexLoaded(context)
        FolderFilterStore.ensureLoaded(context)
        val deletedSet = getDeletedSnapshot(context)
        val pendingMeta = HashMap<String, LocalTrackMediaMeta>()
        val discoveredFolders = HashSet<String>()
        val results = mutableListOf<ScannedDeviceTrack>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.DATE_ADDED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf("2000")
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
                } else -1
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else -1

                if (idCol < 0) return@use

                val albumArtBase = Uri.parse("content://media/external/audio/albumart")

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    if (mediaId < 0L) continue
                    val displayName = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else ""
                    val cleanFileName = displayName.substringBeforeLast('.').replace('_', ' ').trim()

                    val rawTitle = if (titleCol >= 0) cursor.getString(titleCol).orEmpty().trim() else ""
                    val title = rawTitle.takeIf { it.isNotBlank() && !it.equals("<unknown>", true) }
                        ?: cleanFileName.ifBlank { "Track $mediaId" }

                    val rawArtist = if (artistCol >= 0) cursor.getString(artistCol).orEmpty().trim() else ""
                    val artist = rawArtist.takeIf { it.isNotBlank() && !it.equals("<unknown>", true) }
                        ?: "ملف محلي"

                    val rawAlbum = if (albumCol >= 0) cursor.getString(albumCol).orEmpty().trim() else ""
                    val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else 0L
                    val rawDuration = if (durCol >= 0) cursor.getLong(durCol) else 0L
                    val durationMs = rawDuration.coerceIn(1_000L, MAX_DURATION_MS)
                    val dateAddedSec = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    val dateAddedMs = if (dateAddedSec > 1_000_000_000_000L) {
                        dateAddedSec
                    } else if (dateAddedSec > 0L) {
                        dateAddedSec * 1000L
                    } else {
                        0L
                    }

                    val bucketName = if (bucketCol >= 0) cursor.getString(bucketCol).orEmpty().trim() else ""
                    val relativePath = if (relPathCol >= 0) cursor.getString(relPathCol).orEmpty().trim() else ""
                    val folderName = bucketName.ifBlank {
                        relativePath.trim('/').substringAfterLast('/').ifBlank { "Music" }
                    }
                    if (folderName.isNotBlank()) {
                        discoveredFolders.add(folderName)
                    }
                    if (FolderFilterStore.isFolderExcluded(
                            folderName = folderName,
                            pathOrUri = "$relativePath/$displayName"
                        )
                    ) {
                        continue
                    }

                    val album = rawAlbum.takeIf { it.isNotBlank() && !it.equals("<unknown>", true) }
                        ?: folderName

                    val contentUri = ContentUris.withAppendedId(collection, mediaId)
                    val albumArtUri = if (albumId > 0L) {
                        ContentUris.withAppendedId(albumArtBase, albumId).toString()
                    } else {
                        ""
                    }

                    val uriStr = contentUri.toString()
                    val deterministicId = "dev_ms_$mediaId"
                    if (("id:$deterministicId" in deletedSet) || ("uri:$uriStr" in deletedSet)) continue

                    val cls = AudioClassifier.classify(
                        title = title,
                        artist = artist,
                        album = album,
                        genre = folderName,
                        uriOrPath = "$relativePath $displayName",
                        autoIsolateQuran = autoIsolateQuran
                    )

                    pendingMeta[uriStr] = LocalTrackMediaMeta(
                        folderName = folderName,
                        albumArtUri = albumArtUri,
                        dateAddedMs = dateAddedMs
                    )

                    results.add(
                        ScannedDeviceTrack(
                            deterministicId = deterministicId,
                            folderName = folderName,
                            albumArtUri = albumArtUri,
                            track = CloudTrack(
                                id = deterministicId,
                                title = title,
                                artist = artist,
                                album = album,
                                genre = folderName,
                                category = cls.category.id,
                                quranAudio = cls.isQuran,
                                durationMs = durationMs,
                                sourceType = "LOCAL_URI",
                                sourceUri = uriStr,
                                accentHex = cls.suggestedAccentHex
                            )
                        )
                    )
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
        }

        registerLocalMetaBatch(context, pendingMeta)
        FolderFilterStore.recordDiscoveredFolders(context, discoveredFolders)

        results
    }

    /**
     * Scans playlists saved in the phone's MediaStore library (both MediaStore.Audio.Playlists
     * and .m3u/.m3u8/.pls playlist files), keeping ALL entries including Holy Quran Surahs.
     */
    @Suppress("DEPRECATION")
    suspend fun scanDeviceLibraryPlaylists(context: Context): List<DeviceLibraryPlaylist> = withContext(Dispatchers.IO) {
        if (!hasAudioScanPermission(context)) return@withContext emptyList()
        val discovered = LinkedHashMap<String, DeviceLibraryPlaylist>()

        // 1. MediaStore.Audio.Playlists + Members
        try {
            val playlistsUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
            context.contentResolver.query(
                playlistsUri,
                arrayOf(MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME),
                null,
                null,
                "${MediaStore.Audio.Playlists.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Audio.Playlists._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Audio.Playlists.NAME)
                if (idCol >= 0) {
                    while (cursor.moveToNext()) {
                        val plId = cursor.getLong(idCol)
                        if (plId <= 0L) continue
                        val name = (if (nameCol >= 0) cursor.getString(nameCol) else null)
                            ?.trim()
                            .orEmpty()
                            .ifBlank { "Playlist $plId" }

                        val memberAudioIds = mutableListOf<Long>()
                        try {
                            val membersUri = MediaStore.Audio.Playlists.Members.getContentUri("external", plId)
                            context.contentResolver.query(
                                membersUri,
                                arrayOf(MediaStore.Audio.Playlists.Members.AUDIO_ID),
                                null,
                                null,
                                MediaStore.Audio.Playlists.Members.PLAY_ORDER
                            )?.use { mCursor ->
                                val audioIdCol = mCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID)
                                if (audioIdCol >= 0) {
                                    while (mCursor.moveToNext()) {
                                        val audioId = mCursor.getLong(audioIdCol)
                                        if (audioId > 0L) memberAudioIds.add(audioId)
                                    }
                                }
                            }
                        } catch (_: Throwable) {
                        }

                        if (memberAudioIds.isNotEmpty()) {
                            val key = "ms_pl_$plId"
                            discovered[key] = DeviceLibraryPlaylist(
                                id = key,
                                name = name,
                                entryCount = memberAudioIds.size,
                                fileUri = ContentUris.withAppendedId(playlistsUri, plId),
                                memberAudioIds = memberAudioIds
                            )
                        }
                    }
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
        }

        // 2. Playlist files (.m3u, .m3u8, .pls) indexed in MediaStore.Files
        try {
            val filesUri = MediaStore.Files.getContentUri("external")
            val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            val args = arrayOf("%.m3u", "%.m3u8", "%.pls")
            context.contentResolver.query(
                filesUri,
                arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME),
                selection,
                args,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )?.use { fCursor ->
                val idCol = fCursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                val nameCol = fCursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                if (idCol >= 0) {
                    while (fCursor.moveToNext() && discovered.size < 30) {
                        val fileId = fCursor.getLong(idCol)
                        if (fileId <= 0L) continue
                        val fileName = (if (nameCol >= 0) fCursor.getString(nameCol) else null).orEmpty()
                        val cleanName = fileName.substringBeforeLast('.').trim()
                        if (cleanName.isBlank()) continue
                        if (discovered.values.any { it.name.equals(cleanName, ignoreCase = true) }) continue

                        val key = "file_pl_$fileId"
                        discovered[key] = DeviceLibraryPlaylist(
                            id = key,
                            name = cleanName,
                            entryCount = 0,
                            fileUri = ContentUris.withAppendedId(filesUri, fileId),
                            memberAudioIds = emptyList()
                        )
                    }
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
        }

        discovered.values.toList()
    }

    suspend fun scanPersistedFolders(
        context: Context,
        autoIsolateQuran: Boolean
    ): List<ScannedDeviceTrack> = withContext(Dispatchers.IO) {
        val all = mutableListOf<ScannedDeviceTrack>()
        val persisted = try {
            context.contentResolver.persistedUriPermissions.filter { it.isReadPermission }
        } catch (_: Throwable) {
            emptyList()
        }
        for (perm in persisted) {
            try {
                all.addAll(
                    scanFolderTree(
                        context = context,
                        treeUri = perm.uri,
                        autoIsolateQuran = autoIsolateQuran,
                        forceQuranIsolation = false
                    )
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
            }
        }
        all
    }

    suspend fun scanFolderTree(
        context: Context,
        treeUri: Uri,
        autoIsolateQuran: Boolean,
        forceQuranIsolation: Boolean = false
    ): List<ScannedDeviceTrack> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Throwable) {
        }

        val rootDocId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: Throwable) {
            return@withContext emptyList()
        }
        if (rootDocId.isNullOrBlank()) return@withContext emptyList()

        FolderFilterStore.ensureLoaded(context)
        val rootFolderName = rootDocId.substringAfterLast(':').substringAfterLast('/').ifBlank { "Folder" }
        val deletedSet = getDeletedSnapshot(context)
        val pendingMeta = HashMap<String, LocalTrackMediaMeta>()
        val discoveredFolders = HashSet<String>()
        val discovered = mutableListOf<ScannedDeviceTrack>()

        fun traverseDirectory(parentDocId: String, currentFolderName: String, depth: Int) {
            if (depth > 5) return
            if (currentFolderName.isNotBlank()) {
                discoveredFolders.add(currentFolderName)
            }
            if (depth > 0 && FolderFilterStore.isFolderExcluded(currentFolderName, currentFolderName)) {
                return
            }
            val childrenUri = try {
                DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
            } catch (_: Throwable) {
                return
            }
            val audioRows = mutableListOf<Triple<Uri, String, Long>>()
            var folderCoverImageUri = ""

            try {
                context.contentResolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(0) ?: continue
                        val name = cursor.getString(1).orEmpty()
                        val mime = cursor.getString(2).orEmpty().lowercase()
                        val lastMod = if (cursor.columnCount > 3 && !cursor.isNull(3)) cursor.getLong(3) else 0L
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

                        when {
                            mime == DocumentsContract.Document.MIME_TYPE_DIR -> {
                                val subName = name.ifBlank { currentFolderName }
                                traverseDirectory(docId, subName, depth + 1)
                            }
                            mime.startsWith("image/") || isImageExtension(name) -> {
                                if (folderCoverImageUri.isEmpty() ||
                                    name.contains("cover", true) ||
                                    name.contains("folder", true) ||
                                    name.contains("album", true)
                                ) {
                                    folderCoverImageUri = docUri.toString()
                                }
                            }
                            mime.startsWith("audio/") || isAudioExtension(name) -> {
                                audioRows.add(Triple(docUri, name, lastMod))
                            }
                        }
                    }
                }
            } catch (_: Throwable) {
            }

            for ((audioUri, fileName, lastModifiedMs) in audioRows) {
                if (FolderFilterStore.isFolderExcluded(currentFolderName, "$currentFolderName/$fileName")) {
                    continue
                }
                val uriStr = audioUri.toString()
                val safeHash = uriStr.hashCode().toUInt().toString(16)
                val deterministicId = "dev_dir_$safeHash"
                if (("id:$deterministicId" in deletedSet) || ("uri:$uriStr" in deletedSet)) continue

                val cleanName = fileName.substringBeforeLast('.').replace('_', ' ').trim()
                    .ifBlank { "Audio File" }
                var title = cleanName
                var artist = currentFolderName
                var album = currentFolderName
                var durationMs = 15_000L

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, audioUri)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { title = it.trim() }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { artist = it.trim() }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { album = it.trim() }
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()?.coerceIn(1_000L, MAX_DURATION_MS) ?: 15_000L
                } catch (_: Throwable) {
                } finally {
                    try {
                        retriever.release()
                    } catch (_: Throwable) {
                    }
                }

                val cls = AudioClassifier.classify(
                    title = title,
                    artist = artist,
                    album = album,
                    genre = currentFolderName,
                    uriOrPath = "$currentFolderName/$fileName",
                    autoIsolateQuran = autoIsolateQuran
                )
                val isQuran = forceQuranIsolation || cls.isQuran
                val catId = if (isQuran) "QURAN" else cls.category.id

                pendingMeta[uriStr] = LocalTrackMediaMeta(
                    folderName = currentFolderName,
                    albumArtUri = folderCoverImageUri,
                    dateAddedMs = lastModifiedMs
                )

                discovered.add(
                    ScannedDeviceTrack(
                        deterministicId = deterministicId,
                        folderName = currentFolderName,
                        albumArtUri = folderCoverImageUri,
                        track = CloudTrack(
                            id = deterministicId,
                            title = title,
                            artist = artist,
                            album = album,
                            genre = currentFolderName,
                            category = catId,
                            quranAudio = isQuran,
                            durationMs = durationMs,
                            sourceType = "LOCAL_URI",
                            sourceUri = uriStr,
                            accentHex = if (isQuran) "#38B28B" else cls.suggestedAccentHex
                        )
                    )
                )
            }
        }

        try {
            traverseDirectory(rootDocId, rootFolderName, 0)
        } catch (_: Throwable) {
        }
        registerLocalMetaBatch(context, pendingMeta)
        FolderFilterStore.recordDiscoveredFolders(context, discoveredFolders)
        discovered
    }

    private fun isAudioExtension(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".mp3") ||
            lower.endsWith(".m4a") ||
            lower.endsWith(".wav") ||
            lower.endsWith(".flac") ||
            lower.endsWith(".ogg") ||
            lower.endsWith(".aac") ||
            lower.endsWith(".opus")
    }

    private fun isImageExtension(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") ||
            lower.endsWith(".jpeg") ||
            lower.endsWith(".png") ||
            lower.endsWith(".webp")
    }
}
