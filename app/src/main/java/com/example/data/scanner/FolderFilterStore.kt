package com.example.data.scanner

import android.content.Context
import android.net.Uri
import com.example.data.model.CloudTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manages automatic and manual exclusion of unwanted audio folders such as
 * WhatsApp Audio, WhatsApp Voice Notes, Telegram, Voice Recordings, Call Recordings,
 * Notifications, Ringtones, and Alarms.
 */
object FolderFilterStore {

    private const val PREFS_NAME = "nagham_folder_filter_prefs"
    private const val KEY_AUTO_EXCLUDE_UNWANTED = "auto_exclude_unwanted_v1"
    private const val KEY_CUSTOM_EXCLUDED = "custom_excluded_folders_v1"
    private const val KEY_ALLOWED_OVERRIDES = "allowed_override_folders_v1"
    private const val KEY_DISCOVERED_FOLDERS = "discovered_folders_v1"

    /**
     * Built-in normalized folder names & path segments associated with messaging voice notes,
     * WhatsApp media, call/voice recorders, and short system tones.
     */
    private val UNWANTED_EXACT_OR_SEGMENT_PATTERNS = setOf(
        "whatsapp",
        "whatsapp audio",
        "whatsapp voice notes",
        "whatsapp documents",
        "whatsapp statuses",
        ".statuses",
        "statuses",
        "telegram",
        "telegram audio",
        "telegram documents",
        "viber",
        "messenger",
        "instagram",
        "snapchat",
        "callrecordings",
        "call recordings",
        "call_rec",
        "recordings",
        "voice recorder",
        "voicerecorder",
        "sound recorder",
        "sound_recorder",
        "voice notes",
        "notifications",
        "ringtones",
        "alarms"
    )

    private val WHATSAPP_FILE_REGEX = Regex(
        pattern = "^(aud|ptt)-\\d{8}-wa\\d+.*",
        option = RegexOption.IGNORE_CASE
    )

    private val lock = Any()
    @Volatile
    private var loaded = false

    private val _autoExcludeUnwanted = MutableStateFlow(true)
    val autoExcludeUnwanted: StateFlow<Boolean> = _autoExcludeUnwanted.asStateFlow()

    private val _customExcludedFolders = MutableStateFlow<Set<String>>(emptySet())
    val customExcludedFolders: StateFlow<Set<String>> = _customExcludedFolders.asStateFlow()

    private val _explicitlyAllowedFolders = MutableStateFlow<Set<String>>(emptySet())
    val explicitlyAllowedFolders: StateFlow<Set<String>> = _explicitlyAllowedFolders.asStateFlow()

    private val _discoveredFolders = MutableStateFlow<Set<String>>(emptySet())
    val discoveredFolders: StateFlow<Set<String>> = _discoveredFolders.asStateFlow()

    @Volatile
    private var cachedCustomSource: Set<String> = emptySet()
    @Volatile
    private var cachedCustomNorms: Set<String> = emptySet()

    @Volatile
    private var cachedAllowedSource: Set<String> = emptySet()
    @Volatile
    private var cachedAllowedNorms: Set<String> = emptySet()

    private val builtInUnwantedCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>(256)

    private fun updateCustomExcludedInternal(updated: Set<String>) {
        _customExcludedFolders.value = updated
        cachedCustomSource = updated
        cachedCustomNorms = updated.map { normalize(it) }.toSet()
    }

    private fun updateAllowedOverridesInternal(updated: Set<String>) {
        _explicitlyAllowedFolders.value = updated
        cachedAllowedSource = updated
        cachedAllowedNorms = updated.map { normalize(it) }.toSet()
    }

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            loaded = true
            try {
                val appCtx = context.applicationContext ?: context
                val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                _autoExcludeUnwanted.value = prefs.getBoolean(KEY_AUTO_EXCLUDE_UNWANTED, true)
                val loadedCustom = prefs.getStringSet(KEY_CUSTOM_EXCLUDED, emptySet())
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.toSet()
                    .orEmpty()
                val loadedAllowed = prefs.getStringSet(KEY_ALLOWED_OVERRIDES, emptySet())
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.toSet()
                    .orEmpty()
                updateCustomExcludedInternal(loadedCustom)
                updateAllowedOverridesInternal(loadedAllowed)
                _discoveredFolders.value = prefs.getStringSet(KEY_DISCOVERED_FOLDERS, emptySet())
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.toSet()
                    .orEmpty()
            } catch (_: Throwable) {
            }
        }
    }

    private fun normalize(name: String): String =
        name.trim().lowercase(Locale.ROOT)

    fun isBuiltInUnwantedFolder(folderName: String, pathOrUri: String = ""): Boolean {
        val cacheKey = "$folderName|$pathOrUri"
        builtInUnwantedCache[cacheKey]?.let { return it }

        val result = computeIsBuiltInUnwantedFolder(folderName, pathOrUri)
        if (builtInUnwantedCache.size > 4096) {
            builtInUnwantedCache.clear()
        }
        builtInUnwantedCache[cacheKey] = result
        return result
    }

    private fun computeIsBuiltInUnwantedFolder(folderName: String, pathOrUri: String): Boolean {
        val normFolder = normalize(folderName)
        if (normFolder.isNotBlank()) {
            if (normFolder in UNWANTED_EXACT_OR_SEGMENT_PATTERNS) return true
            if (normFolder.contains("whatsapp") ||
                normFolder.contains("telegram") ||
                normFolder.contains("voice note") ||
                normFolder.contains("call record") ||
                normFolder.contains("sound_recorder") ||
                normFolder.contains("sound recorder") ||
                normFolder.contains("voice recorder")
            ) {
                return true
            }
        }

        if (pathOrUri.isNotBlank()) {
            val decoded = try {
                Uri.decode(pathOrUri).orEmpty()
            } catch (_: Throwable) {
                pathOrUri
            }
            val lowerPath = decoded.lowercase(Locale.ROOT).replace('\\', '/')
            val segments = lowerPath.split('/').map { it.substringAfterLast(':').trim() }.filter { it.isNotBlank() }
            for (seg in segments) {
                if (seg in UNWANTED_EXACT_OR_SEGMENT_PATTERNS) return true
                if (seg.contains("whatsapp") ||
                    seg.contains("telegram audio") ||
                    seg.contains("voice notes") ||
                    seg.contains("sound_recorder") ||
                    seg.contains("callrecordings")
                ) {
                    return true
                }
            }
            val fileName = segments.lastOrNull()?.substringBeforeLast('.').orEmpty()
            if (fileName.isNotBlank() && WHATSAPP_FILE_REGEX.matches(fileName)) {
                return true
            }
        }
        return false
    }

    fun isFolderExcluded(
        folderName: String,
        pathOrUri: String = "",
        autoExclude: Boolean = _autoExcludeUnwanted.value,
        customExcluded: Set<String> = _customExcludedFolders.value,
        allowedOverrides: Set<String> = _explicitlyAllowedFolders.value
    ): Boolean {
        val trimmed = folderName.trim()
        val norm = normalize(trimmed)

        val customNorms = if (customExcluded === cachedCustomSource) {
            cachedCustomNorms
        } else {
            customExcluded.map { normalize(it) }.toSet()
        }
        if (norm.isNotBlank() && norm in customNorms) {
            return true
        }

        val allowedNorms = if (allowedOverrides === cachedAllowedSource) {
            cachedAllowedNorms
        } else {
            allowedOverrides.map { normalize(it) }.toSet()
        }
        if (norm.isNotBlank() && norm in allowedNorms) {
            return false
        }

        if (autoExclude && isBuiltInUnwantedFolder(trimmed, pathOrUri)) {
            return true
        }
        return false
    }

    fun isTrackExcluded(
        context: Context,
        track: CloudTrack,
        autoExclude: Boolean = _autoExcludeUnwanted.value,
        customExcluded: Set<String> = _customExcludedFolders.value,
        allowedOverrides: Set<String> = _explicitlyAllowedFolders.value
    ): Boolean {
        val meta = DeviceAudioScanner.resolveMetaForTrack(context, track)
        val folder = meta.folderName.ifBlank { track.genre.ifBlank { track.album } }
        return isFolderExcluded(
            folderName = folder,
            pathOrUri = "${track.sourceUri} ${track.title}",
            autoExclude = autoExclude,
            customExcluded = customExcluded,
            allowedOverrides = allowedOverrides
        )
    }

    fun recordDiscoveredFolders(context: Context, folders: Collection<String>) {
        ensureLoaded(context)
        val cleaned = folders.map { it.trim() }.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return
        val current = _discoveredFolders.value
        val merged = (current + cleaned)
        if (merged.size == current.size) return
        _discoveredFolders.value = merged
        try {
            val appCtx = context.applicationContext ?: context
            appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_DISCOVERED_FOLDERS, merged)
                .apply()
        } catch (_: Throwable) {
        }
    }

    fun setAutoExcludeUnwanted(context: Context, enabled: Boolean) {
        ensureLoaded(context)
        _autoExcludeUnwanted.value = enabled
        try {
            val appCtx = context.applicationContext ?: context
            appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_AUTO_EXCLUDE_UNWANTED, enabled)
                .apply()
        } catch (_: Throwable) {
        }
    }

    fun excludeFolder(context: Context, folderName: String) {
        ensureLoaded(context)
        val clean = folderName.trim()
        if (clean.isBlank()) return
        val norm = normalize(clean)

        val updatedCustom = _customExcludedFolders.value + clean
        val updatedAllowed = _explicitlyAllowedFolders.value.filterNot { normalize(it) == norm }.toSet()
        val updatedDiscovered = _discoveredFolders.value + clean

        updateCustomExcludedInternal(updatedCustom)
        updateAllowedOverridesInternal(updatedAllowed)
        _discoveredFolders.value = updatedDiscovered

        persistFolderSets(context, updatedCustom, updatedAllowed, updatedDiscovered)
    }

    fun restoreFolder(context: Context, folderName: String) {
        ensureLoaded(context)
        val clean = folderName.trim()
        if (clean.isBlank()) return
        val norm = normalize(clean)

        val updatedCustom = _customExcludedFolders.value.filterNot { normalize(it) == norm }.toSet()
        val updatedAllowed = if (isBuiltInUnwantedFolder(clean)) {
            _explicitlyAllowedFolders.value + clean
        } else {
            _explicitlyAllowedFolders.value
        }
        val updatedDiscovered = _discoveredFolders.value + clean

        updateCustomExcludedInternal(updatedCustom)
        updateAllowedOverridesInternal(updatedAllowed)
        _discoveredFolders.value = updatedDiscovered

        persistFolderSets(context, updatedCustom, updatedAllowed, updatedDiscovered)
    }

    fun resetDefaults(context: Context) {
        synchronized(lock) {
            loaded = true
            _autoExcludeUnwanted.value = true
            updateCustomExcludedInternal(emptySet())
            updateAllowedOverridesInternal(emptySet())
            _discoveredFolders.value = emptySet()
            builtInUnwantedCache.clear()
            try {
                val appCtx = context.applicationContext ?: context
                appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()
            } catch (_: Throwable) {
            }
        }
    }

    private fun persistFolderSets(
        context: Context,
        custom: Set<String>,
        allowed: Set<String>,
        discovered: Set<String>
    ) {
        try {
            val appCtx = context.applicationContext ?: context
            appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_CUSTOM_EXCLUDED, custom)
                .putStringSet(KEY_ALLOWED_OVERRIDES, allowed)
                .putStringSet(KEY_DISCOVERED_FOLDERS, discovered)
                .apply()
        } catch (_: Throwable) {
        }
    }
}
