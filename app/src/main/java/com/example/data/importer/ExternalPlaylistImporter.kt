package com.example.data.importer

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.AudioClassifier
import com.example.data.model.CloudTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ParsedPlaylistEntry(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val sourceUriOrQuery: String,
    val isPlayableStream: Boolean
)

data class ImportedPlaylistResult(
    val playlistName: String,
    val sourceApp: String,
    val entries: List<ParsedPlaylistEntry>
)

object ExternalPlaylistImporter {

    suspend fun extractDeviceAudioTrack(
        context: Context,
        uri: Uri,
        autoIsolateQuran: Boolean
    ): CloudTrack = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }

        val fallbackName = queryDisplayName(context, uri)
            ?.substringBeforeLast('.')
            ?.replace('_', ' ')
            ?.trim()
            .orEmpty()
            .ifBlank { "Audio Track" }

        var title = fallbackName
        var artist = "Local Device"
        var album = ""
        var genre = ""
        var durationMs = 0L

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?.let { title = it.trim() }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?.let { artist = it.trim() }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }
                ?.let { album = it.trim() }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?.takeIf { it.isNotBlank() }
                ?.let { genre = it.trim() }
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        val classification = AudioClassifier.classify(
            title = title,
            artist = artist,
            album = album,
            genre = genre,
            uriOrPath = "$fallbackName ${uri.lastPathSegment.orEmpty()}",
            autoIsolateQuran = autoIsolateQuran
        )

        CloudTrack(
            title = title,
            artist = artist,
            album = album,
            genre = genre,
            category = classification.category.id,
            quranAudio = classification.isQuran,
            durationMs = durationMs.coerceAtLeast(10_000L),
            sourceType = "LOCAL_URI",
            sourceUri = uri.toString(),
            accentHex = classification.suggestedAccentHex
        )
    }

    suspend fun parsePlaylistFromUri(
        context: Context,
        uri: Uri,
        fallbackPlaylistName: String
    ): ImportedPlaylistResult = withContext(Dispatchers.IO) {
        val fileName = queryDisplayName(context, uri).orEmpty()
        val rawContent = context.contentResolver.openInputStream(uri)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()

        val inferredName = fileName.substringBeforeLast('.')
            .takeIf { it.isNotBlank() }
            ?: fallbackPlaylistName

        parseRawPlaylistText(
            rawText = rawContent,
            playlistName = inferredName,
            sourceApp = if (fileName.endsWith(".pls", ignoreCase = true)) "PLS External App" else "M3U External App"
        )
    }

    fun parseRawPlaylistText(
        rawText: String,
        playlistName: String,
        sourceApp: String = "External Music App"
    ): ImportedPlaylistResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.any { it.startsWith("[playlist]", ignoreCase = true) || it.startsWith("File1=", ignoreCase = true) }) {
            return parsePls(lines, playlistName, sourceApp)
        }
        return parseM3uOrSharedLines(lines, playlistName, sourceApp)
    }

    /**
     * Matches an imported playlist entry against ALL existing library tracks — including both
     * Songs and Holy Quran Surahs — so Quran Surahs in imported playlists are never dropped.
     */
    fun matchEntryToExistingTrack(
        entry: ParsedPlaylistEntry,
        allTracks: List<CloudTrack>
    ): CloudTrack? {
        if (allTracks.isEmpty()) return null
        val rawLoc = entry.sourceUriOrQuery.trim()
        val normalizedFileStem = rawLoc
            .replace('\\', '/')
            .substringAfterLast('/')
            .substringBefore('?')
            .substringBeforeLast('.')
            .replace('_', ' ')
            .trim()

        // 1. Exact URI match
        allTracks.firstOrNull {
            rawLoc.isNotEmpty() && it.sourceUri.equals(rawLoc, ignoreCase = true)
        }?.let { return it }

        // 2. Exact title match (includes Quran Surah names in Arabic or English)
        val targetTitle = entry.title.trim()
        if (targetTitle.isNotEmpty()) {
            allTracks.firstOrNull {
                it.title.trim().equals(targetTitle, ignoreCase = true)
            }?.let { return it }
        }

        // 3. File stem match against track title or sourceUri segment
        if (normalizedFileStem.length >= 2) {
            allTracks.firstOrNull {
                it.title.trim().equals(normalizedFileStem, ignoreCase = true) ||
                    it.sourceUri.substringAfterLast('/').substringBeforeLast('.')
                        .replace('_', ' ')
                        .equals(normalizedFileStem, ignoreCase = true)
            }?.let { return it }
        }

        // 4. Meaningful substring title match
        if (targetTitle.length >= 3) {
            allTracks.firstOrNull {
                it.title.contains(targetTitle, ignoreCase = true) ||
                    (it.title.length >= 3 && targetTitle.contains(it.title, ignoreCase = true))
            }?.let { return it }
        }

        return null
    }

    private fun parsePls(
        lines: List<String>,
        playlistName: String,
        sourceApp: String
    ): ImportedPlaylistResult {
        val files = mutableMapOf<Int, String>()
        val titles = mutableMapOf<Int, String>()
        val lengths = mutableMapOf<Int, Long>()

        val kvRegex = Regex("""^(File|Title|Length)(\d+)=(.*)$""", RegexOption.IGNORE_CASE)
        for (line in lines) {
            val m = kvRegex.find(line) ?: continue
            val key = m.groupValues[1].lowercase()
            val idx = m.groupValues[2].toIntOrNull() ?: continue
            val value = m.groupValues[3].trim()
            when (key) {
                "file" -> files[idx] = value
                "title" -> titles[idx] = value
                "length" -> lengths[idx] = (value.toLongOrNull()?.coerceAtLeast(0L) ?: 0L) * 1000L
            }
        }

        val entries = files.keys.sorted().mapNotNull { idx ->
            val loc = files[idx] ?: return@mapNotNull null
            val cleanFile = loc.replace('\\', '/').substringAfterLast('/').substringBeforeLast('.')
            val rawTitle = titles[idx].orEmpty().ifBlank { cleanFile }
            val (artist, song) = splitArtistAndTitle(rawTitle)
            ParsedPlaylistEntry(
                title = song,
                artist = artist,
                durationMs = lengths[idx]?.takeIf { it > 0L } ?: 20000L,
                sourceUriOrQuery = loc,
                isPlayableStream = isDirectlyPlayableLocation(loc)
            )
        }

        return ImportedPlaylistResult(
            playlistName = playlistName.ifBlank { "Imported PLS Playlist" },
            sourceApp = sourceApp,
            entries = entries
        )
    }

    private fun parseM3uOrSharedLines(
        lines: List<String>,
        playlistName: String,
        sourceApp: String
    ): ImportedPlaylistResult {
        val entries = mutableListOf<ParsedPlaylistEntry>()
        var pendingTitle: String? = null
        var pendingDurationMs = 20000L
        var detectedName = playlistName

        val extInfRegex = Regex("""^#EXTINF:\s*(-?\d+)[^,]*,(.*)$""", RegexOption.IGNORE_CASE)

        for (line in lines) {
            when {
                line.startsWith("#EXTM3U", ignoreCase = true) -> continue
                line.startsWith("#PLAYLIST:", ignoreCase = true) -> {
                    val custom = line.substringAfter(":").trim()
                    if (custom.isNotBlank()) detectedName = custom
                }
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    val m = extInfRegex.find(line)
                    if (m != null) {
                        val sec = m.groupValues[1].toLongOrNull() ?: 20L
                        pendingDurationMs = if (sec > 0) sec * 1000L else 20000L
                        pendingTitle = m.groupValues[2].trim()
                    }
                }
                line.startsWith("#") -> continue
                line.startsWith("http://", true) ||
                    line.startsWith("https://", true) ||
                    line.startsWith("content://", true) ||
                    line.startsWith("file://", true) ||
                    line.startsWith("/storage/", true) ||
                    line.startsWith("/sdcard/", true) -> {
                    val normalizedPath = if (line.startsWith("/")) "file://$line" else line
                    val fallbackStem = line.replace('\\', '/')
                        .substringAfterLast('/')
                        .substringBefore('?')
                        .substringBeforeLast('.')
                        .replace('_', ' ')
                        .ifBlank { "Audio Track" }
                    val label = pendingTitle?.takeIf { it.isNotBlank() } ?: fallbackStem
                    val (artist, song) = splitArtistAndTitle(label)
                    entries.add(
                        ParsedPlaylistEntry(
                            title = song,
                            artist = artist,
                            durationMs = pendingDurationMs,
                            sourceUriOrQuery = normalizedPath,
                            isPlayableStream = isDirectlyPlayableLocation(normalizedPath)
                        )
                    )
                    pendingTitle = null
                    pendingDurationMs = 20000L
                }
                else -> {
                    val cleaned = line.replace(Regex("""^\d+[\.\)\-]\s*"""), "").trim()
                    if (cleaned.length >= 2) {
                        val label = pendingTitle?.takeIf { it.isNotBlank() }
                            ?: cleaned.replace('\\', '/').substringAfterLast('/').substringBeforeLast('.')
                        val (artist, song) = splitArtistAndTitle(label)
                        entries.add(
                            ParsedPlaylistEntry(
                                title = song,
                                artist = artist,
                                durationMs = pendingDurationMs,
                                sourceUriOrQuery = cleaned,
                                isPlayableStream = isDirectlyPlayableLocation(cleaned)
                            )
                        )
                    }
                    pendingTitle = null
                    pendingDurationMs = 20000L
                }
            }
        }

        return ImportedPlaylistResult(
            playlistName = detectedName.ifBlank { "قائمة مستوردة" },
            sourceApp = sourceApp,
            entries = entries
        )
    }

    private fun isDirectlyPlayableLocation(loc: String): Boolean {
        val lower = loc.trim().lowercase()
        if (lower.startsWith("content://") || lower.startsWith("file://") || lower.startsWith("/storage/") || lower.startsWith("/sdcard/")) {
            return true
        }
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            val clean = lower.substringBefore('?')
            return clean.endsWith(".mp3") ||
                clean.endsWith(".wav") ||
                clean.endsWith(".ogg") ||
                clean.endsWith(".m4a") ||
                clean.endsWith(".aac") ||
                clean.endsWith(".flac") ||
                clean.endsWith(".opus")
        }
        return false
    }

    private fun splitArtistAndTitle(raw: String): Pair<String, String> {
        val separators = listOf(" - ", " – ", " — ", " | ")
        for (sep in separators) {
            if (raw.contains(sep)) {
                val left = raw.substringBefore(sep).trim()
                val right = raw.substringAfter(sep).trim()
                if (left.isNotBlank() && right.isNotBlank()) {
                    return left to right
                }
            }
        }
        return "مستورد / External" to raw.trim().ifBlank { "Track" }
    }

    fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else {
                        null
                    }
                }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }
}
