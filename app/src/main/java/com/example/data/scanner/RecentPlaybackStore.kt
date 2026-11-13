package com.example.data.scanner

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

object RecentPlaybackStore {
    private const val PREFS_NAME = "nagham_recent_playback"
    private const val KEY_JSON = "recent_timestamps"

    private val _recentTimestamps = MutableStateFlow<Map<String, Long>>(emptyMap())
    val recentTimestamps: StateFlow<Map<String, Long>> = _recentTimestamps.asStateFlow()
    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        try {
            val raw = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_JSON, null) ?: return
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, Long>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optLong(k, 0L)
            }
            _recentTimestamps.value = map
        } catch (_: Exception) {
        }
    }

    fun recordPlayed(context: Context, trackId: String, epochMs: Long = System.currentTimeMillis()) {
        if (trackId.isBlank()) return
        ensureLoaded(context)
        val updated = _recentTimestamps.value.toMutableMap().apply {
            this[trackId] = epochMs
        }
        // Keep top 120 recent entries
        val trimmed = updated.entries
            .sortedByDescending { it.value }
            .take(120)
            .associate { it.key to it.value }

        _recentTimestamps.value = trimmed
        try {
            val json = JSONObject()
            for ((k, v) in trimmed) {
                json.put(k, v)
            }
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_JSON, json.toString())
                .apply()
        } catch (_: Exception) {
        }
    }
}
