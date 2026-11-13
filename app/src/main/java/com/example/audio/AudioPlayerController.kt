package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.CloudTrack
import com.example.data.model.EqualizerPreset
import com.example.data.scanner.RecentPlaybackStore
import com.example.ui.theme.ThemePreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

enum class RepeatMode {
    OFF, ALL, ONE
}

data class PlaybackState(
    val currentTrack: CloudTrack? = null,
    val queue: List<CloudTrack> = emptyList(),
    val isQuranMode: Boolean = false,
    val isMixedPlaylistQueue: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val playbackSpeed: Float = 1.0f,
    val eqEnabled: Boolean = true,
    val eqPresetId: String = EqualizerPreset.WARM_OUD.id,
    val eqBandsMb: List<Long> = EqualizerPreset.WARM_OUD.bandsMb,
    val bassBoostStrength: Long = EqualizerPreset.WARM_OUD.defaultBass,
    val hardwareEqAvailable: Boolean = true,
    val eqBandFrequenciesLabel: List<String> = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz"),
    val playbackNotice: String? = null
)

class AudioPlayerController(@Volatile private var appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var tickerJob: Job? = null

    private val _state = MutableStateFlow(
        PlaybackState(eqEnabled = ThemePreferences.getEqEnabled(appContext, fallback = true))
    )
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    var onTrackPlayedCallback: ((CloudTrack) -> Unit)? = null

    private var noisyReceiverRegistered = false
    private val audioOutputDisconnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                handleAudioOutputDisconnected()
            } else if (action == AudioManager.ACTION_HEADSET_PLUG && !isInitialStickyBroadcast) {
                val plugState = intent.getIntExtra("state", -1)
                if (plugState == 0) {
                    handleAudioOutputDisconnected()
                }
            }
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            val disconnectedExternalOutput = removedDevices.any { device ->
                device.isSink && when (device.type) {
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_USB_DEVICE,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_SPEAKER -> true
                    else -> false
                }
            }
            if (disconnectedExternalOutput) {
                handleAudioOutputDisconnected()
            }
        }
    }

    init {
        registerAudioDisconnectReceiver()
    }

    private fun registerAudioDisconnectReceiver() {
        if (noisyReceiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction(AudioManager.ACTION_HEADSET_PLUG)
            }
            ContextCompat.registerReceiver(
                appContext,
                audioOutputDisconnectReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.registerAudioDeviceCallback(audioDeviceCallback, null)
            noisyReceiverRegistered = true
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to register audio disconnect receiver: ${t.message}")
        }
    }

    /**
     * Invoked automatically when wired headphones or Bluetooth audio devices disconnect.
     * Returns true if playback was paused, or false if playback continued per user setting.
     */
    fun handleAudioOutputDisconnected(): Boolean {
        val shouldPause = ThemePreferences.getPauseOnHeadsetDisconnect(appContext, fallback = true)
        if (!shouldPause) {
            return false
        }
        val wasPlaying = _state.value.isPlaying || (runCatching { mediaPlayer?.isPlaying == true }.getOrDefault(false))
        if (wasPlaying) {
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Error pausing on audio output disconnect: ${t.message}")
            }
            _state.update { it.copy(isPlaying = false) }
            return true
        }
        return false
    }

    internal fun setSimulatedPlayingStateForTest(track: CloudTrack, isPlaying: Boolean) {
        _state.update {
            it.copy(
                currentTrack = track,
                queue = listOf(track),
                isPlaying = isPlaying
            )
        }
    }

    fun playMusicQueue(tracks: List<CloudTrack>, startTrackId: String? = null) {
        // Strict isolation on Home & Music Library tabs: never include Quran audio in Music tab queues
        val musicOnly = tracks.filter { !it.quranAudio && it.id.isNotBlank() }
        if (musicOnly.isEmpty()) return
        val ordered = if (_state.value.shuffleEnabled) {
            val startTrack = musicOnly.firstOrNull { it.id == startTrackId } ?: musicOnly.first()
            listOf(startTrack) + (musicOnly.filter { it.id != startTrack.id }.shuffled())
        } else {
            musicOnly
        }
        val target = ordered.firstOrNull { it.id == startTrackId } ?: ordered.first()
        _state.update {
            it.copy(
                queue = ordered,
                isQuranMode = false,
                isMixedPlaylistQueue = false,
                playbackNotice = null
            )
        }
        loadAndPlay(target, isQuran = false)
    }

    fun playQuranQueue(tracks: List<CloudTrack>, startTrackId: String? = null) {
        // Strict isolation on Quran Sanctuary tab: contains ONLY Quran tracks, shuffle disabled
        val quranOnly = tracks.filter { it.quranAudio && it.id.isNotBlank() }
        if (quranOnly.isEmpty()) return
        val target = quranOnly.firstOrNull { it.id == startTrackId } ?: quranOnly.first()
        _state.update {
            it.copy(
                queue = quranOnly,
                isQuranMode = true,
                isMixedPlaylistQueue = false,
                shuffleEnabled = false,
                playbackNotice = null
            )
        }
        loadAndPlay(target, isQuran = true)
    }

    /**
     * Plays a user or imported Playlist queue while preserving ALL tracks in the playlist,
     * including Holy Quran Surahs alongside other audio files without filtering them out.
     */
    fun playPlaylistQueue(tracks: List<CloudTrack>, startTrackId: String? = null) {
        val valid = tracks.filter { it.id.isNotBlank() }
        if (valid.isEmpty()) return
        val hasAnyQuran = valid.any { it.quranAudio }
        val ordered = if (_state.value.shuffleEnabled && !hasAnyQuran) {
            val startTrack = valid.firstOrNull { it.id == startTrackId } ?: valid.first()
            listOf(startTrack) + (valid.filter { it.id != startTrack.id }.shuffled())
        } else {
            valid
        }
        val target = ordered.firstOrNull { it.id == startTrackId } ?: ordered.first()
        _state.update {
            it.copy(
                queue = ordered,
                isQuranMode = target.quranAudio,
                isMixedPlaylistQueue = true,
                shuffleEnabled = if (hasAnyQuran) false else it.shuffleEnabled,
                playbackNotice = null
            )
        }
        loadAndPlay(target, isQuran = target.quranAudio)
    }

    fun refreshQueueMetadata(updatedTracks: List<CloudTrack>) {
        val current = _state.value.currentTrack ?: return
        val mapById = updatedTracks.associateBy { it.id }
        val freshCurrent = mapById[current.id]
        val isMixed = _state.value.isMixedPlaylistQueue
        val updatedQueue = _state.value.queue.mapNotNull { qTrack ->
            val fresh = mapById[qTrack.id] ?: return@mapNotNull null
            if (isMixed || _state.value.isQuranMode == fresh.quranAudio) fresh else null
        }

        if (freshCurrent == null || (!isMixed && freshCurrent.quranAudio != _state.value.isQuranMode)) {
            if (updatedQueue.isNotEmpty()) {
                val next = updatedQueue.first()
                _state.update {
                    it.copy(
                        queue = updatedQueue,
                        isQuranMode = next.quranAudio
                    )
                }
                loadAndPlay(next, next.quranAudio)
            } else {
                stopAndClear()
            }
            return
        }

        _state.update {
            it.copy(
                currentTrack = freshCurrent,
                isQuranMode = freshCurrent.quranAudio,
                queue = updatedQueue
            )
        }
    }

    fun stopAndClear() {
        releasePlayerOnly()
        _state.update {
            it.copy(
                currentTrack = null,
                queue = emptyList(),
                isMixedPlaylistQueue = false,
                isPlaying = false,
                isBuffering = false,
                currentPositionMs = 0L
            )
        }
        NaghamMediaService.stopServiceSafely(appContext)
    }

    private fun loadAndPlay(track: CloudTrack, isQuran: Boolean) {
        scope.launch {
            releasePlayerOnly()
            _state.update {
                it.copy(
                    currentTrack = track,
                    isQuranMode = isQuran,
                    isBuffering = true,
                    isPlaying = false,
                    currentPositionMs = 0L,
                    durationMs = track.durationMs.coerceAtLeast(1000L),
                    playbackNotice = null
                )
            }

            try {
                val mp = MediaPlayer()
                mediaPlayer = mp
                try {
                    mp.setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
                } catch (_: Throwable) {
                }
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(
                            if (isQuran) AudioAttributes.CONTENT_TYPE_SPEECH
                            else AudioAttributes.CONTENT_TYPE_MUSIC
                        )
                        .build()
                )

                val uriString = track.sourceUri
                if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                    mp.setDataSource(appContext, Uri.parse(uriString))
                } else {
                    mp.setDataSource(uriString)
                }

                mp.setOnPreparedListener { player ->
                    try {
                        attachAudioEffects(player.audioSessionId, isQuran)
                        applyPlaybackSpeedInternal(player, _state.value.playbackSpeed)
                        player.start()
                        val resolvedDur = player.duration.toLong().takeIf { it > 0 } ?: track.durationMs
                        _state.update {
                            it.copy(
                                isBuffering = false,
                                isPlaying = true,
                                durationMs = resolvedDur.coerceAtLeast(1000L)
                            )
                        }
                        if (!isQuran) {
                            RecentPlaybackStore.recordPlayed(appContext, track.id)
                        }
                        onTrackPlayedCallback?.invoke(track)
                        NaghamMediaService.ensureStartedForPlayback(appContext)
                        startPositionTicker()
                    } catch (t: Throwable) {
                        Log.w(TAG, "Error starting prepared MediaPlayer: ${t.message}")
                        _state.update {
                            it.copy(
                                isBuffering = false,
                                isPlaying = false,
                                playbackNotice = t.localizedMessage ?: "Audio playback error"
                            )
                        }
                    }
                }

                mp.setOnCompletionListener {
                    handleTrackCompletion()
                }

                mp.setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer error ($what, $extra) on ${track.sourceUri}")
                    _state.update {
                        it.copy(
                            isBuffering = false,
                            isPlaying = false,
                            playbackNotice = "تعذر تشغيل هذا الملف الصوتي — تأكد من وجود الملف في ذاكرة الهاتف"
                        )
                    }
                    true
                }

                mp.prepareAsync()
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to prepare MediaPlayer", t)
                _state.update {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        playbackNotice = t.localizedMessage ?: "Audio playback error"
                    )
                }
            }
        }
    }

    private fun attachAudioEffects(audioSessionId: Int, isQuran: Boolean) {
        releaseEffects()
        if (audioSessionId <= 0) return

        var eqAvailable = false
        var detectedLabels: List<String>? = null

        val shouldEnableDsp = isQuran || _state.value.eqEnabled

        try {
            val eq = Equalizer(0, audioSessionId).apply { enabled = shouldEnableDsp }
            equalizer = eq
            eqAvailable = true
            val numBands = eq.numberOfBands.toInt()
            val labels = (0 until numBands.coerceAtMost(5)).map { bandIdx ->
                val hz = eq.getCenterFreq(bandIdx.toShort()) / 1000
                if (hz >= 1000) "${String.format(Locale.US, "%.1f", hz / 1000f)}kHz" else "${hz}Hz"
            }
            if (labels.size == 5) {
                detectedLabels = labels
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Hardware Equalizer unavailable on this audio HAL: ${t.message}")
        }

        try {
            val bass = BassBoost(0, audioSessionId).apply { enabled = shouldEnableDsp }
            bassBoost = bass
        } catch (t: Throwable) {
            Log.w(TAG, "Hardware BassBoost unavailable on this audio HAL: ${t.message}")
        }

        _state.update {
            it.copy(
                hardwareEqAvailable = eqAvailable,
                eqBandFrequenciesLabel = detectedLabels ?: it.eqBandFrequenciesLabel
            )
        }

        if (isQuran) {
            applyBandsToHardware(EqualizerPreset.VOCAL_CLEAR.bandsMb, 0L)
        } else if (_state.value.eqEnabled) {
            applyBandsToHardware(_state.value.eqBandsMb, _state.value.bassBoostStrength)
        }
    }

    private fun applyBandsToHardware(bandsMb: List<Long>, bassStrength: Long) {
        try {
            val eq = equalizer
            if (eq != null) {
                val range = eq.bandLevelRange
                val minMb = range.getOrElse(0) { -1500 }.toInt()
                val maxMb = range.getOrElse(1) { 1500 }.toInt()
                val count = eq.numberOfBands.toInt().coerceAtMost(bandsMb.size)
                for (i in 0 until count) {
                    val clamped = bandsMb[i].toInt().coerceIn(minMb, maxMb).toShort()
                    eq.setBandLevel(i.toShort(), clamped)
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed applying EQ band level: ${t.message}")
        }

        try {
            val bb = bassBoost
            if (bb != null && bb.strengthSupported) {
                bb.setStrength(bassStrength.toInt().coerceIn(0, 1000).toShort())
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed applying BassBoost strength: ${t.message}")
        }
    }

    fun updateEqualizer(presetId: String, bandsMb: List<Long>, bassStrength: Long) {
        val normalized = if (bandsMb.size == 5) bandsMb else EqualizerPreset.WARM_OUD.bandsMb
        val clampedBass = bassStrength.coerceIn(0L, 1000L)
        val cur = _state.value
        if (cur.eqPresetId == presetId && cur.eqBandsMb == normalized && cur.bassBoostStrength == clampedBass) {
            return
        }
        _state.update {
            it.copy(
                eqPresetId = presetId,
                eqBandsMb = normalized,
                bassBoostStrength = clampedBass
            )
        }
        if (!_state.value.isQuranMode && _state.value.eqEnabled) {
            applyBandsToHardware(normalized, clampedBass)
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        ThemePreferences.saveEqEnabled(appContext, enabled)
        _state.update { it.copy(eqEnabled = enabled) }
        val activeForDsp = _state.value.isQuranMode || enabled
        try {
            equalizer?.enabled = activeForDsp
        } catch (t: Throwable) {
            Log.w(TAG, "Failed toggling Equalizer enabled state: ${t.message}")
        }
        try {
            bassBoost?.enabled = activeForDsp
        } catch (t: Throwable) {
            Log.w(TAG, "Failed toggling BassBoost enabled state: ${t.message}")
        }
        if (enabled && !_state.value.isQuranMode) {
            applyBandsToHardware(_state.value.eqBandsMb, _state.value.bassBoostStrength)
        }
    }

    fun play() {
        val mp = mediaPlayer
        if (mp == null) {
            val current = _state.value.currentTrack ?: return
            loadAndPlay(current, current.quranAudio)
            return
        }
        try {
            if (!mp.isPlaying) {
                mp.start()
                _state.update { it.copy(isPlaying = true) }
                NaghamMediaService.ensureStartedForPlayback(appContext)
                startPositionTicker()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "play error", t)
        }
    }

    fun pause() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) {
                mp.pause()
                _state.update { it.copy(isPlaying = false) }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "pause error", t)
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: run {
            val current = _state.value.currentTrack
            if (current != null) {
                loadAndPlay(current, current.quranAudio)
            }
            return
        }
        try {
            if (mp.isPlaying) {
                pause()
            } else {
                play()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "togglePlayPause error", t)
        }
    }

    fun seekTo(positionMs: Long) {
        val mp = mediaPlayer ?: return
        try {
            val target = positionMs.coerceIn(0L, _state.value.durationMs.coerceAtLeast(1000L))
            mp.seekTo(target.toInt())
            _state.update { it.copy(currentPositionMs = target) }
        } catch (t: Throwable) {
            Log.w(TAG, "seekTo error", t)
        }
    }

    fun skipNext() {
        val q = _state.value.queue
        if (q.isEmpty()) return
        val curId = _state.value.currentTrack?.id
        val idx = q.indexOfFirst { it.id == curId }
        val nextIdx = if (idx < 0 || idx + 1 >= q.size) 0 else idx + 1
        val nextTrack = q[nextIdx]
        loadAndPlay(nextTrack, nextTrack.quranAudio)
    }

    fun skipPrevious() {
        val q = _state.value.queue
        if (q.isEmpty()) return
        if (_state.value.currentPositionMs > 3500L) {
            seekTo(0L)
            return
        }
        skipToPreviousTrack()
    }

    fun skipToPreviousTrack() {
        val q = _state.value.queue
        if (q.isEmpty()) return
        val curId = _state.value.currentTrack?.id
        val idx = q.indexOfFirst { it.id == curId }
        val prevIdx = if (idx <= 0) q.lastIndex else idx - 1
        val prevTrack = q[prevIdx]
        loadAndPlay(prevTrack, prevTrack.quranAudio)
    }

    fun toggleShuffle() {
        if (_state.value.isQuranMode || _state.value.queue.any { it.quranAudio }) return
        val newShuffle = !_state.value.shuffleEnabled
        val current = _state.value.currentTrack
        val reordered = if (newShuffle && _state.value.queue.size > 1) {
            val rest = _state.value.queue.filter { it.id != current?.id }.shuffled()
            listOfNotNull(current) + rest
        } else {
            _state.value.queue
        }
        _state.update { it.copy(shuffleEnabled = newShuffle, queue = reordered) }
    }

    fun cycleRepeatMode() {
        val nextMode = when (_state.value.repeatMode) {
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
            RepeatMode.OFF -> RepeatMode.ALL
        }
        _state.update { it.copy(repeatMode = nextMode) }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.75f, 1.5f)
        _state.update { it.copy(playbackSpeed = clamped) }
        mediaPlayer?.let { mp ->
            if (_state.value.isPlaying) {
                applyPlaybackSpeedInternal(mp, clamped)
            }
        }
    }

    private fun applyPlaybackSpeedInternal(mp: MediaPlayer, speed: Float) {
        try {
            mp.playbackParams = PlaybackParams().setSpeed(speed)
        } catch (t: Throwable) {
            Log.w(TAG, "PlaybackParams speed unsupported: ${t.message}")
        }
    }

    private fun handleTrackCompletion() {
        val st = _state.value
        val cur = st.currentTrack ?: return
        when (st.repeatMode) {
            RepeatMode.ONE -> loadAndPlay(cur, cur.quranAudio)
            RepeatMode.ALL -> skipNext()
            RepeatMode.OFF -> {
                val idx = st.queue.indexOfFirst { it.id == cur.id }
                if (idx >= 0 && idx + 1 < st.queue.size) {
                    val nextTrack = st.queue[idx + 1]
                    loadAndPlay(nextTrack, nextTrack.quranAudio)
                } else {
                    _state.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
                }
            }
        }
    }

    private fun startPositionTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null && _state.value.isPlaying) {
                    try {
                        val pos = mp.currentPosition.toLong().coerceAtLeast(0L)
                        _state.update { it.copy(currentPositionMs = pos) }
                    } catch (_: Throwable) {
                    }
                }
                delay(250L)
            }
        }
    }

    private fun releaseEffects() {
        try {
            equalizer?.release()
        } catch (_: Throwable) {
        }
        equalizer = null
        try {
            bassBoost?.release()
        } catch (_: Throwable) {
        }
        bassBoost = null
    }

    private fun releasePlayerOnly() {
        tickerJob?.cancel()
        releaseEffects()
        try {
            mediaPlayer?.stop()
        } catch (_: Throwable) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Throwable) {
        }
        mediaPlayer = null
    }

    fun releaseAll() {
        releasePlayerOnly()
        NaghamMediaService.stopServiceSafely(appContext)
    }

    companion object {
        private const val TAG = "AudioPlayerController"

        @Volatile
        private var sharedInstance: AudioPlayerController? = null

        fun getInstance(context: Context): AudioPlayerController {
            val resolvedContext = context.applicationContext ?: context
            return sharedInstance?.also { it.appContext = resolvedContext } ?: synchronized(this) {
                sharedInstance?.also { it.appContext = resolvedContext }
                    ?: AudioPlayerController(resolvedContext).also {
                        sharedInstance = it
                    }
            }
        }
    }
}
