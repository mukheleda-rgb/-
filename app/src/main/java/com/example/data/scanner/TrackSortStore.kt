package com.example.data.scanner

import android.content.Context
import com.example.data.model.CloudTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class TrackSortOption(val id: String) {
    NAME("NAME"),
    DATE("DATE"),
    DURATION("DURATION"),
    CUSTOM("CUSTOM");

    companion object {
        fun fromId(id: String?): TrackSortOption {
            return entries.firstOrNull { it.id == id } ?: NAME
        }
    }
}

object TrackSortStore {

    private const val PREFS_NAME = "nagham_track_sort_prefs"
    private const val KEY_MUSIC_SORT = "music_sort_option"
    private const val KEY_QURAN_SORT = "quran_sort_option"
    private const val KEY_MUSIC_ORDER = "music_custom_order_json"
    private const val KEY_QURAN_ORDER = "quran_custom_order_json"
    private const val KEY_PLAYLIST_SORTS = "playlist_sort_options_json"

    @Volatile
    private var loaded = false
    private val lock = Any()

    private val _musicSortOption = MutableStateFlow(TrackSortOption.NAME)
    val musicSortOption: StateFlow<TrackSortOption> = _musicSortOption.asStateFlow()

    private val _quranSortOption = MutableStateFlow(TrackSortOption.NAME)
    val quranSortOption: StateFlow<TrackSortOption> = _quranSortOption.asStateFlow()

    private val _musicCustomOrder = MutableStateFlow<List<String>>(emptyList())
    val musicCustomOrder: StateFlow<List<String>> = _musicCustomOrder.asStateFlow()

    private val _quranCustomOrder = MutableStateFlow<List<String>>(emptyList())
    val quranCustomOrder: StateFlow<List<String>> = _quranCustomOrder.asStateFlow()

    private val _playlistSortOptions = MutableStateFlow<Map<String, TrackSortOption>>(emptyMap())
    val playlistSortOptions: StateFlow<Map<String, TrackSortOption>> = _playlistSortOptions.asStateFlow()

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            loaded = true
            try {
                val appCtx = context.applicationContext ?: context
                val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                _musicSortOption.value = TrackSortOption.fromId(
                    prefs.getString(KEY_MUSIC_SORT, TrackSortOption.NAME.id)
                )
                _quranSortOption.value = TrackSortOption.fromId(
                    prefs.getString(KEY_QURAN_SORT, TrackSortOption.NAME.id)
                )
                _musicCustomOrder.value = parseJsonStringList(prefs.getString(KEY_MUSIC_ORDER, null))
                _quranCustomOrder.value = parseJsonStringList(prefs.getString(KEY_QURAN_ORDER, null))
                _playlistSortOptions.value = parsePlaylistSortMap(prefs.getString(KEY_PLAYLIST_SORTS, null))
            } catch (_: Throwable) {
            }
        }
    }

    fun setMusicSortOption(
        context: Context,
        option: TrackSortOption,
        seedOrderIdsIfEmpty: List<String> = emptyList()
    ) {
        ensureLoaded(context)
        if (option == TrackSortOption.CUSTOM && _musicCustomOrder.value.isEmpty() && seedOrderIdsIfEmpty.isNotEmpty()) {
            val clean = seedOrderIdsIfEmpty.filter { it.isNotBlank() }.distinct()
            _musicCustomOrder.value = clean
            persistString(context, KEY_MUSIC_ORDER, encodeJsonStringList(clean))
        }
        _musicSortOption.value = option
        persistString(context, KEY_MUSIC_SORT, option.id)
    }

    fun setQuranSortOption(
        context: Context,
        option: TrackSortOption,
        seedOrderIdsIfEmpty: List<String> = emptyList()
    ) {
        ensureLoaded(context)
        if (option == TrackSortOption.CUSTOM && _quranCustomOrder.value.isEmpty() && seedOrderIdsIfEmpty.isNotEmpty()) {
            val clean = seedOrderIdsIfEmpty.filter { it.isNotBlank() }.distinct()
            _quranCustomOrder.value = clean
            persistString(context, KEY_QURAN_ORDER, encodeJsonStringList(clean))
        }
        _quranSortOption.value = option
        persistString(context, KEY_QURAN_SORT, option.id)
    }

    fun saveMusicCustomOrder(context: Context, orderedIds: List<String>) {
        ensureLoaded(context)
        val clean = orderedIds.filter { it.isNotBlank() }.distinct()
        _musicCustomOrder.value = clean
        _musicSortOption.value = TrackSortOption.CUSTOM
        try {
            val appCtx = context.applicationContext ?: context
            appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MUSIC_ORDER, encodeJsonStringList(clean))
                .putString(KEY_MUSIC_SORT, TrackSortOption.CUSTOM.id)
                .apply()
        } catch (_: Throwable) {
        }
    }

    fun saveQuranCustomOrder(context: Context, orderedIds: List<String>) {
        ensureLoaded(context)
        val clean = orderedIds.filter { it.isNotBlank() }.distinct()
        _quranCustomOrder.value = clean
        _quranSortOption.value = TrackSortOption.CUSTOM
        try {
            val appCtx = context.applicationContext ?: context
            appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_QURAN_ORDER, encodeJsonStringList(clean))
                .putString(KEY_QURAN_SORT, TrackSortOption.CUSTOM.id)
                .apply()
        } catch (_: Throwable) {
        }
    }

    fun getPlaylistSortOption(playlistId: String, map: Map<String, TrackSortOption> = _playlistSortOptions.value): TrackSortOption {
        return map[playlistId] ?: TrackSortOption.CUSTOM
    }

    fun setPlaylistSortOption(context: Context, playlistId: String, option: TrackSortOption) {
        if (playlistId.isBlank()) return
        ensureLoaded(context)
        val updated = _playlistSortOptions.value.toMutableMap().apply {
            put(playlistId, option)
        }
        _playlistSortOptions.value = updated
        persistString(context, KEY_PLAYLIST_SORTS, encodePlaylistSortMap(updated))
    }

    fun sortTracks(
        tracks: List<CloudTrack>,
        option: TrackSortOption,
        customOrderIds: List<String>,
        dateLookupMs: (CloudTrack) -> Long = { it.createdAt?.toDate()?.time ?: 0L }
    ): List<CloudTrack> {
        if (tracks.size <= 1) return tracks
        return when (option) {
            TrackSortOption.NAME -> tracks.sortedWith(
                compareBy<CloudTrack> { it.title.trim().lowercase() }
                    .thenBy { it.id }
            )
            TrackSortOption.DATE -> tracks.sortedWith(
                compareByDescending<CloudTrack> { dateLookupMs(it) }
                    .thenBy { it.title.trim().lowercase() }
                    .thenBy { it.id }
            )
            TrackSortOption.DURATION -> tracks.sortedWith(
                compareByDescending<CloudTrack> { it.durationMs }
                    .thenBy { it.title.trim().lowercase() }
                    .thenBy { it.id }
            )
            TrackSortOption.CUSTOM -> {
                if (customOrderIds.isEmpty()) return tracks
                val indexMap = HashMap<String, Int>(customOrderIds.size)
                customOrderIds.forEachIndexed { idx, id ->
                    if (id !in indexMap) indexMap[id] = idx
                }
                val inOrder = ArrayList<CloudTrack>(tracks.size)
                val remaining = ArrayList<CloudTrack>()
                for (track in tracks) {
                    if (track.id in indexMap) {
                        inOrder.add(track)
                    } else {
                        remaining.add(track)
                    }
                }
                inOrder.sortBy { indexMap[it.id] ?: Int.MAX_VALUE }
                inOrder + remaining
            }
        }
    }

    /**
     * Moves [movedTrackId] to the position of [targetTrackId] inside a full custom ID order,
     * preserving relative positions of any currently filtered-out items.
     */
    fun moveTrackInOrder(
        baseOrderIds: List<String>,
        fallbackTracksInCurrentSort: List<CloudTrack>,
        movedTrackId: String,
        targetTrackId: String
    ): List<String> {
        if (movedTrackId == targetTrackId) {
            return baseOrderIds.ifEmpty { fallbackTracksInCurrentSort.map { it.id } }
        }
        val merged = buildList {
            for (id in baseOrderIds) {
                if (id.isNotBlank()) add(id)
            }
            for (t in fallbackTracksInCurrentSort) {
                if (t.id.isNotBlank()) add(t.id)
            }
        }.distinct().toMutableList()

        val fromIdx = merged.indexOf(movedTrackId)
        val toIdx = merged.indexOf(targetTrackId)
        if (fromIdx < 0 || toIdx < 0 || fromIdx == toIdx) return merged

        val item = merged.removeAt(fromIdx)
        merged.add(toIdx, item)
        return merged
    }

    private fun persistString(context: Context, key: String, value: String) {
        try {
            val appCtx = context.applicationContext ?: context
            appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(key, value)
                .apply()
        } catch (_: Throwable) {
        }
    }

    private fun parseJsonStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i, "").trim()
                    if (v.isNotEmpty()) add(v)
                }
            }.distinct()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun encodeJsonStringList(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun parsePlaylistSortMap(raw: String?): Map<String, TrackSortOption> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val result = LinkedHashMap<String, TrackSortOption>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                result[k] = TrackSortOption.fromId(obj.optString(k))
            }
            result
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    private fun encodePlaylistSortMap(map: Map<String, TrackSortOption>): String {
        val obj = JSONObject()
        // Cap stored entries to keep preferences compact
        map.entries.take(300).forEach { (k, v) -> obj.put(k, v.id) }
        return obj.toString()
    }
}
