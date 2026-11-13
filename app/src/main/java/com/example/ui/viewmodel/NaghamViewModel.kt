package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerController
import com.example.audio.PlaybackState
import com.example.data.importer.ExternalPlaylistImporter
import com.example.data.importer.ImportedPlaylistResult
import com.example.data.model.AudioClassifier
import com.example.data.model.CloudPlaylist
import com.example.data.model.CloudTrack
import com.example.data.model.EqualizerPreset
import com.example.data.model.TrackCategory
import com.example.data.model.UserSettings
import com.example.data.remote.LyricsService
import com.example.data.repository.MusicRepository
import com.example.data.scanner.DeviceAudioScanner
import com.example.data.scanner.DeviceLibraryPlaylist
import com.example.data.scanner.RecentPlaybackStore
import com.example.data.scanner.ScannedDeviceTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class NaghamViewModel(
    private val repository: MusicRepository,
    private val playerController: AudioPlayerController,
    private val currentUserId: String,
    initialDisplayName: String
) : ViewModel() {

    private val safeInitialDisplayName = initialDisplayName.trim().ifBlank { "مستمع نغم" }

    private val _optimisticDarkTheme = MutableStateFlow<Boolean?>(null)
    private val _optimisticEyeComfort = MutableStateFlow<Boolean?>(null)
    private val _optimisticLanguage = MutableStateFlow<String?>(null)
    private val _optimisticAutoIsolate = MutableStateFlow<Boolean?>(null)

    val settingsState: StateFlow<UiState<UserSettings>> = combine(
        repository.observeUserSettings(currentUserId),
        _optimisticDarkTheme,
        _optimisticEyeComfort,
        _optimisticLanguage,
        _optimisticAutoIsolate
    ) { s, darkOverride, eyeOverride, langOverride, autoIsoOverride ->
        val base = s ?: UserSettings(userId = currentUserId, displayName = safeInitialDisplayName)
        val resolved = base.copy(
            displayName = base.displayName.ifBlank { safeInitialDisplayName },
            darkTheme = darkOverride ?: base.darkTheme,
            eyeComfortMode = eyeOverride ?: base.eyeComfortMode,
            languageCode = langOverride ?: base.languageCode,
            autoIsolateQuran = autoIsoOverride ?: base.autoIsolateQuran
        )
        UiState.Success(resolved) as UiState<UserSettings>
    }
        .onEach { ui ->
            if (ui is UiState.Success) {
                try {
                    playerController.updateEqualizer(
                        presetId = ui.data.eqPreset,
                        bandsMb = ui.data.eqBands,
                        bassStrength = ui.data.bassBoostStrength
                    )
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed syncing equalizer state: ${t.message}")
                }
            }
        }
        .catch { error ->
            Log.w(TAG, "Error observing user settings", error)
            emit(UiState.Error(error.message ?: "Failed to load user settings"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val tracksState: StateFlow<UiState<List<CloudTrack>>> = repository.observeTracks(currentUserId)
        .map<List<CloudTrack>, UiState<List<CloudTrack>>> { list ->
            val sorted = list
                .filter { it.id.isNotBlank() }
                .distinctBy { it.id }
                .sortedBy { it.title.lowercase() }
            UiState.Success(sorted)
        }
        .onEach { ui ->
            if (ui is UiState.Success) {
                try {
                    playerController.refreshQueueMetadata(ui.data)
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed refreshing queue metadata: ${t.message}")
                }
            }
        }
        .catch { error ->
            Log.w(TAG, "Error observing tracks", error)
            emit(UiState.Error(error.message ?: "Failed to observe tracks"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val playlistsState: StateFlow<UiState<List<CloudPlaylist>>> = repository.observePlaylists(currentUserId)
        .map<List<CloudPlaylist>, UiState<List<CloudPlaylist>>> { list ->
            val sorted = list
                .filter { it.id.isNotBlank() }
                .distinctBy { it.id }
                .sortedBy { it.name.lowercase() }
            UiState.Success(sorted)
        }
        .catch { error ->
            Log.w(TAG, "Error observing playlists", error)
            emit(UiState.Error(error.message ?: "Failed to observe playlists"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val playbackState: StateFlow<PlaybackState> = playerController.state
    val recentTimestamps: StateFlow<Map<String, Long>> = RecentPlaybackStore.recentTimestamps

    private val _deviceLibraryPlaylists = MutableStateFlow<List<DeviceLibraryPlaylist>>(emptyList())
    val deviceLibraryPlaylists: StateFlow<List<DeviceLibraryPlaylist>> = _deviceLibraryPlaylists.asStateFlow()

    private val _lyricsDownloading = MutableStateFlow(false)
    val lyricsDownloading: StateFlow<Boolean> = _lyricsDownloading.asStateFlow()

    private val _isScanningStorage = MutableStateFlow(false)
    val isScanningStorage: StateFlow<Boolean> = _isScanningStorage.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        playerController.onTrackPlayedCallback = { playedTrack ->
            incrementTrackPlayCount(playedTrack)
        }
        launchSafe("initializeUserAndPurgeLegacyStarters") {
            repository.initializeUserAndPurgeLegacyStarters(currentUserId, safeInitialDisplayName)
        }
    }

    private fun launchSafe(
        opName: String,
        onFinally: () -> Unit = {},
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Log.e(TAG, "Caught error in $opName: ${t.message}", t)
            } finally {
                onFinally()
            }
        }
    }

    override fun onCleared() {
        if (playerController.onTrackPlayedCallback != null) {
            playerController.onTrackPlayedCallback = null
        }
        super.onCleared()
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun currentSettingsSnapshot(): UserSettings {
        return (settingsState.value as? UiState.Success)?.data
            ?: UserSettings(userId = currentUserId, displayName = safeInitialDisplayName)
    }

    fun currentTracksSnapshot(): List<CloudTrack> {
        return (tracksState.value as? UiState.Success)?.data ?: emptyList()
    }

    fun currentPlaylistsSnapshot(): List<CloudPlaylist> {
        return (playlistsState.value as? UiState.Success)?.data ?: emptyList()
    }

    fun setLanguage(languageCode: String) {
        val normalized = if (languageCode == "en") "en" else "ar"
        val current = currentSettingsSnapshot()
        if (current.languageCode == normalized && _optimisticLanguage.value == normalized) return
        _optimisticLanguage.value = normalized
        launchSafe("setLanguage") {
            repository.saveUserSettings(current.copy(languageCode = normalized))
        }
    }

    fun setDarkTheme(isDark: Boolean) {
        val current = currentSettingsSnapshot()
        if (current.darkTheme == isDark && _optimisticDarkTheme.value == isDark) return
        _optimisticDarkTheme.value = isDark
        launchSafe("setDarkTheme") {
            repository.saveUserSettings(current.copy(darkTheme = isDark))
        }
    }

    fun toggleEyeComfortMode() {
        val current = currentSettingsSnapshot()
        val nextEye = !current.eyeComfortMode
        _optimisticEyeComfort.value = nextEye
        launchSafe("toggleEyeComfortMode") {
            repository.saveUserSettings(current.copy(eyeComfortMode = nextEye))
        }
    }

    fun toggleAutoIsolateQuran() {
        val current = currentSettingsSnapshot()
        val nextIso = !current.autoIsolateQuran
        _optimisticAutoIsolate.value = nextIso
        launchSafe("toggleAutoIsolateQuran") {
            repository.saveUserSettings(current.copy(autoIsolateQuran = nextIso))
        }
    }

    fun refreshDeviceLibraryPlaylists(context: Context) {
        val appContext = context.applicationContext ?: context
        launchSafe("refreshDeviceLibraryPlaylists") {
            _deviceLibraryPlaylists.value = DeviceAudioScanner.scanDeviceLibraryPlaylists(appContext)
        }
    }

    /**
     * Automatically scans MediaStore & any persisted folders and silently adds newly detected
     * Music & Quran files without requiring any manual button clicks.
     */
    fun autoSyncDeviceAudio(context: Context, silent: Boolean = true) {
        if (_isScanningStorage.value) return
        val appContext = context.applicationContext ?: context
        val autoIsolate = currentSettingsSnapshot().autoIsolateQuran
        val isAr = currentSettingsSnapshot().languageCode == "ar"
        _isScanningStorage.value = true

        launchSafe(
            opName = "autoSyncDeviceAudio",
            onFinally = { _isScanningStorage.value = false }
        ) {
            val combined = mutableListOf<ScannedDeviceTrack>()
            if (DeviceAudioScanner.hasAudioScanPermission(appContext)) {
                combined.addAll(
                    DeviceAudioScanner.scanPhoneMediaStore(
                        context = appContext,
                        autoIsolateQuran = autoIsolate
                    )
                )
                _deviceLibraryPlaylists.value = DeviceAudioScanner.scanDeviceLibraryPlaylists(appContext)
            }
            combined.addAll(
                DeviceAudioScanner.scanPersistedFolders(
                    context = appContext,
                    autoIsolateQuran = autoIsolate
                )
            )

            if (combined.isNotEmpty()) {
                val distinctPairs = combined
                    .distinctBy { it.track.sourceUri }
                    .map { it.deterministicId to it.track }
                val (addedMusic, addedQuran) = repository.syncScannedDeviceTracks(
                    userId = currentUserId,
                    scanned = distinctPairs
                )
                if (addedMusic + addedQuran > 0) {
                    _statusBanner.value = if (isAr) {
                        "تم اكتشاف وإضافة $addedMusic أغنية وعزل $addedQuran ملف قرآن كريم تلقائياً"
                    } else {
                        "Auto-added $addedMusic songs & isolated $addedQuran Quran files"
                    }
                } else if (!silent) {
                    _statusBanner.value = if (isAr) {
                        "المكتبة محدّثة تلقائياً (${distinctPairs.size} ملف صوتي)"
                    } else {
                        "Library automatically synced (${distinctPairs.size} audio files)"
                    }
                }
            }
        }
    }

    fun playMusic(tracks: List<CloudTrack>, startTrackId: String? = null) {
        playerController.playMusicQueue(tracks, startTrackId)
    }

    fun playQuran(tracks: List<CloudTrack>, startTrackId: String? = null) {
        playerController.playQuranQueue(tracks, startTrackId)
    }

    fun playPlaylist(tracks: List<CloudTrack>, startTrackId: String? = null) {
        playerController.playPlaylistQueue(tracks, startTrackId)
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun skipNext() = playerController.skipNext()
    fun skipPrevious() = playerController.skipPrevious()
    fun skipToPreviousTrack() = playerController.skipToPreviousTrack()
    fun seekTo(posMs: Long) = playerController.seekTo(posMs)
    fun toggleShuffle() = playerController.toggleShuffle()
    fun cycleRepeatMode() = playerController.cycleRepeatMode()
    fun setPlaybackSpeed(speed: Float) = playerController.setPlaybackSpeed(speed)

    fun setEqualizerEnabled(enabled: Boolean) {
        playerController.setEqualizerEnabled(enabled)
    }

    fun resetEqualizerToFlat() {
        selectEqualizerPreset(EqualizerPreset.FLAT)
    }

    fun selectEqualizerPreset(preset: EqualizerPreset) {
        playerController.updateEqualizer(
            presetId = preset.id,
            bandsMb = preset.bandsMb,
            bassStrength = preset.defaultBass
        )
        persistEqualizerToCloud(preset.id, preset.bandsMb, preset.defaultBass)
    }

    fun updateEqualizerBand(bandIndex: Int, levelMb: Long) {
        val cur = playbackState.value
        val updatedBands = cur.eqBandsMb.toMutableList().apply {
            if (bandIndex in indices) this[bandIndex] = levelMb.coerceIn(-1500L, 1500L)
        }
        playerController.updateEqualizer(
            presetId = EqualizerPreset.CUSTOM.id,
            bandsMb = updatedBands,
            bassStrength = cur.bassBoostStrength
        )
    }

    fun updateBassBoost(strength: Long) {
        val cur = playbackState.value
        playerController.updateEqualizer(
            presetId = EqualizerPreset.CUSTOM.id,
            bandsMb = cur.eqBandsMb,
            bassStrength = strength.coerceIn(0L, 1000L)
        )
    }

    fun syncCurrentEqualizerToCloud() {
        val cur = playbackState.value
        persistEqualizerToCloud(cur.eqPresetId, cur.eqBandsMb, cur.bassBoostStrength)
        val isAr = currentSettingsSnapshot().languageCode == "ar"
        _statusBanner.value = if (isAr) "تمت مزامنة إعدادات موازن الصوت سحابياً" else "Equalizer synced to cloud"
    }

    private fun persistEqualizerToCloud(presetId: String, bands: List<Long>, bass: Long) {
        val current = currentSettingsSnapshot()
        launchSafe("persistEqualizerToCloud") {
            repository.saveUserSettings(
                current.copy(
                    eqPreset = presetId,
                    eqBands = bands,
                    bassBoostStrength = bass
                )
            )
        }
    }

    private fun incrementTrackPlayCount(track: CloudTrack) {
        val fresh = currentTracksSnapshot().firstOrNull { it.id == track.id } ?: track
        if (fresh.id.isBlank() || fresh.createdAt == null) return
        launchSafe("incrementTrackPlayCount") {
            repository.updateTrack(fresh.copy(playCount = fresh.playCount + 1L))
        }
    }

    fun toggleTrackFavorite(track: CloudTrack) {
        if (track.id.isBlank()) return
        launchSafe("toggleTrackFavorite") {
            repository.updateTrack(track.copy(favorite = !track.favorite))
        }
    }

    fun saveEditedTrack(updatedTrack: CloudTrack) {
        if (updatedTrack.id.isBlank()) return
        launchSafe("saveEditedTrack") {
            val res = repository.updateTrack(updatedTrack)
            val isAr = currentSettingsSnapshot().languageCode == "ar"
            _statusBanner.value = if (res.isSuccess) {
                if (isAr) "تم حفظ تعديلات المقطع والكلمات ومزامنتها" else "Track details & lyrics synced"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to update track"
            }
        }
    }

    /**
     * Long-press action: Move a track between the isolated Quran Sanctuary tab and the Music tab
     * while preserving it inside any existing playlists.
     */
    fun moveTrackBetweenQuranAndMusic(track: CloudTrack, moveToQuran: Boolean) {
        if (track.id.isBlank()) return
        val isAr = currentSettingsSnapshot().languageCode == "ar"
        val targetCategory = if (moveToQuran) TrackCategory.QURAN.id else TrackCategory.ARABIC_TARAB.id
        val targetAccent = if (moveToQuran) "#38B28B" else "#E8A855"

        launchSafe("moveTrackBetweenQuranAndMusic") {
            val updated = track.copy(
                category = targetCategory,
                quranAudio = moveToQuran,
                accentHex = targetAccent
            )
            val res = repository.updateTrack(updated)
            if (res.isSuccess) {
                _statusBanner.value = if (moveToQuran) {
                    if (isAr) "تم نقل وعزل «${track.title}» في تبويب القرآن الكريم" else "Moved '${track.title}' to Quran tab"
                } else {
                    if (isAr) "تم نقل «${track.title}» إلى تبويب الموسيقى" else "Moved '${track.title}' to Music tab"
                }
            }
        }
    }

    /**
     * Long-press action: Add a track (Music or Quran Surah) to an existing playlist.
     */
    fun addTrackToPlaylist(track: CloudTrack, playlist: CloudPlaylist) {
        if (track.id.isBlank() || playlist.id.isBlank()) return
        val isAr = currentSettingsSnapshot().languageCode == "ar"
        if (track.id in playlist.trackIds) {
            _statusBanner.value = if (isAr) "الملف موجود بالفعل في «${playlist.name}»" else "Already in '${playlist.name}'"
            return
        }
        launchSafe("addTrackToPlaylist") {
            val updatedIds = (playlist.trackIds + track.id).distinct()
            val res = repository.updatePlaylist(playlist.copy(trackIds = updatedIds))
            if (res.isSuccess) {
                _statusBanner.value = if (isAr) {
                    "تمت إضافة «${track.title}» إلى قائمة «${playlist.name}»"
                } else {
                    "Added '${track.title}' to '${playlist.name}'"
                }
            }
        }
    }

    /**
     * Long-press action: Quickly create a new playlist containing the selected track.
     */
    fun createPlaylistWithTrack(name: String, track: CloudTrack) {
        if (track.id.isBlank()) return
        val isAr = currentSettingsSnapshot().languageCode == "ar"
        launchSafe("createPlaylistWithTrack") {
            val res = repository.createPlaylist(
                CloudPlaylist(
                    name = name.ifBlank { track.title.take(30) },
                    description = "",
                    trackIds = listOf(track.id),
                    sourceApp = "Nagham",
                    quranPlaylist = track.quranAudio,
                    coverHex = if (track.quranAudio) "#38B28B" else "#E8A855"
                )
            )
            if (res.isSuccess) {
                _statusBanner.value = if (isAr) {
                    "تم إنشاء القائمة وإضافة «${track.title}»"
                } else {
                    "Created playlist with '${track.title}'"
                }
            }
        }
    }

    fun deleteTrack(context: Context, track: CloudTrack) {
        if (track.id.isBlank()) return
        val isAr = currentSettingsSnapshot().languageCode == "ar"
        DeviceAudioScanner.markTrackDeletedByUser(context, track.id, track.sourceUri)
        launchSafe("deleteTrack") {
            val res = repository.deleteTrack(track.id)
            if (res.isSuccess) {
                val containingPlaylists = currentPlaylistsSnapshot().filter { track.id in it.trackIds }
                for (pl in containingPlaylists) {
                    repository.updatePlaylist(pl.copy(trackIds = pl.trackIds.filterNot { it == track.id }))
                }
                _statusBanner.value = if (isAr) {
                    "تم حذف «${track.title}» من المكتبة"
                } else {
                    "Deleted '${track.title}' from library"
                }
            }
        }
    }

    fun deleteTrackById(context: Context, trackId: String) {
        if (trackId.isBlank()) return
        val found = currentTracksSnapshot().firstOrNull { it.id == trackId }
        if (found != null) {
            deleteTrack(context, found)
        } else {
            DeviceAudioScanner.markTrackDeletedByUser(context, trackId, "")
            launchSafe("deleteTrackById") {
                repository.deleteTrack(trackId)
            }
        }
    }

    /**
     * Batch action: Delete multiple selected tracks from library and clean up their references
     * from any containing playlists in one pass per playlist.
     */
    fun deleteTracksBatch(context: Context, tracks: List<CloudTrack>) {
        val valid = tracks.filter { it.id.isNotBlank() }.distinctBy { it.id }
        if (valid.isEmpty()) return
        val isAr = currentSettingsSnapshot().languageCode == "ar"
        val deletedIds = valid.map { it.id }.toSet()

        for (trk in valid) {
            DeviceAudioScanner.markTrackDeletedByUser(context, trk.id, trk.sourceUri)
        }

        launchSafe("deleteTracksBatch") {
            var deletedCount = 0
            for (trk in valid) {
                val res = repository.deleteTrack(trk.id)
                if (res.isSuccess) deletedCount++
            }
            if (deletedCount > 0) {
                val affectedPlaylists = currentPlaylistsSnapshot().filter { pl ->
                    pl.trackIds.any { it in deletedIds }
                }
                for (pl in affectedPlaylists) {
                    repository.updatePlaylist(
                        pl.copy(trackIds = pl.trackIds.filterNot { it in deletedIds })
                    )
                }
                _statusBanner.value = if (isAr) {
                    "تم مسح $deletedCount ملفات من المكتبة"
                } else {
                    "Deleted $deletedCount files from library"
                }
            }
        }
    }

    /**
     * Batch action: Add multiple selected tracks (Songs and/or Holy Quran Surahs) to an existing playlist.
     */
    fun addTracksToPlaylistBatch(tracks: List<CloudTrack>, playlist: CloudPlaylist) {
        val validIds = tracks.map { it.id }.filter { it.isNotBlank() }.distinct()
        if (validIds.isEmpty() || playlist.id.isBlank()) return
        val isAr = currentSettingsSnapshot().languageCode == "ar"
        val mergedIds = (playlist.trackIds + validIds).distinct().take(200)
        val newlyAddedCount = mergedIds.size - playlist.trackIds.distinct().size

        if (newlyAddedCount <= 0) {
            _statusBanner.value = if (isAr) {
                "جميع الملفات المحددة موجودة بالفعل في «${playlist.name}»"
            } else {
                "All selected files are already in '${playlist.name}'"
            }
            return
        }

        launchSafe("addTracksToPlaylistBatch") {
            val res = repository.updatePlaylist(playlist.copy(trackIds = mergedIds))
            if (res.isSuccess) {
                _statusBanner.value = if (isAr) {
                    "تمت إضافة $newlyAddedCount ملفات إلى قائمة «${playlist.name}»"
                } else {
                    "Added $newlyAddedCount files to '${playlist.name}'"
                }
            }
        }
    }

    /**
     * Batch action: Create a brand-new playlist containing all selected tracks in order.
     */
    fun createPlaylistWithTracksBatch(name: String, tracks: List<CloudTrack>) {
        val valid = tracks.filter { it.id.isNotBlank() }.distinctBy { it.id }
        if (valid.isEmpty()) return
        val isAr = currentSettingsSnapshot().languageCode == "ar"
        val ids = valid.map { it.id }.take(200)
        val allQuran = valid.all { it.quranAudio }
        val finalName = name.trim().ifBlank { valid.first().title.take(30) }

        launchSafe("createPlaylistWithTracksBatch") {
            val res = repository.createPlaylist(
                CloudPlaylist(
                    name = finalName,
                    description = "",
                    trackIds = ids,
                    sourceApp = "Nagham",
                    quranPlaylist = allQuran,
                    coverHex = if (allQuran) "#38B28B" else "#E8A855"
                )
            )
            if (res.isSuccess) {
                _statusBanner.value = if (isAr) {
                    "تم إنشاء قائمة «$finalName» وإضافة ${ids.size} ملفات"
                } else {
                    "Created '$finalName' with ${ids.size} files"
                }
            }
        }
    }

    fun downloadLyricsForEditor(
        title: String,
        artist: String,
        onResult: (Result<String>) -> Unit
    ) {
        _lyricsDownloading.value = true
        launchSafe(
            opName = "downloadLyricsForEditor",
            onFinally = { _lyricsDownloading.value = false }
        ) {
            val result = LyricsService.downloadLyrics(title, artist)
            onResult(result)
        }
    }

    fun importDeviceAudioFiles(
        context: Context,
        uris: List<Uri>,
        forceQuranIsolation: Boolean = false
    ) {
        if (uris.isEmpty()) return
        val appContext = context.applicationContext ?: context
        val autoIsolate = currentSettingsSnapshot().autoIsolateQuran
        val isAr = currentSettingsSnapshot().languageCode == "ar"
        launchSafe("importDeviceAudioFiles") {
            var quranCount = 0
            var musicCount = 0
            for (uri in uris) {
                val extracted = ExternalPlaylistImporter.extractDeviceAudioTrack(
                    context = appContext,
                    uri = uri,
                    autoIsolateQuran = autoIsolate
                )
                val finalTrack = if (forceQuranIsolation) {
                    extracted.copy(
                        category = TrackCategory.QURAN.id,
                        quranAudio = true,
                        accentHex = "#38B28B"
                    )
                } else {
                    extracted
                }
                DeviceAudioScanner.registerLocalMeta(
                    context = appContext,
                    sourceUri = finalTrack.sourceUri,
                    folderName = finalTrack.album.ifBlank { "Music" },
                    albumArtUri = ""
                )
                repository.createTrack(finalTrack)
                if (finalTrack.quranAudio) quranCount++ else musicCount++
            }
            _statusBanner.value = if (isAr) {
                "تمت إضافة $musicCount مقطع موسيقي وعزل $quranCount ملف قرآن كريم"
            } else {
                "Imported $musicCount music tracks & isolated $quranCount Quran files"
            }
        }
    }

    fun createCustomPlaylist(
        name: String,
        description: String,
        trackIds: List<String>,
        isQuranPlaylist: Boolean = false,
        sourceApp: String = "Nagham"
    ) {
        launchSafe("createCustomPlaylist") {
            repository.createPlaylist(
                CloudPlaylist(
                    name = name,
                    description = description,
                    trackIds = trackIds,
                    sourceApp = sourceApp,
                    quranPlaylist = isQuranPlaylist,
                    coverHex = if (isQuranPlaylist) "#38B28B" else "#E8A855"
                )
            )
        }
    }

    fun updatePlaylistTracks(playlist: CloudPlaylist, newTrackIds: List<String>) {
        if (playlist.id.isBlank()) return
        launchSafe("updatePlaylistTracks") {
            repository.updatePlaylist(playlist.copy(trackIds = newTrackIds))
        }
    }

    fun deletePlaylist(playlistId: String) {
        if (playlistId.isBlank()) return
        launchSafe("deletePlaylist") {
            repository.deletePlaylist(playlistId)
        }
    }

    fun importExternalPlaylistFromUri(context: Context, uri: Uri, customName: String) {
        val appContext = context.applicationContext ?: context
        launchSafe("importExternalPlaylistFromUri") {
            val parsed = ExternalPlaylistImporter.parsePlaylistFromUri(
                context = appContext,
                uri = uri,
                fallbackPlaylistName = customName.ifBlank { "External Playlist" }
            )
            processImportedPlaylist(parsed)
        }
    }

    fun importExternalPlaylistFromText(rawText: String, playlistName: String) {
        launchSafe("importExternalPlaylistFromText") {
            val parsed = ExternalPlaylistImporter.parseRawPlaylistText(
                rawText = rawText,
                playlistName = playlistName.ifBlank { "Shared App Playlist" },
                sourceApp = "External App Share"
            )
            processImportedPlaylist(parsed)
        }
    }

    /**
     * Imports a playlist directly from the phone's MediaStore library, keeping ALL tracks
     * including Holy Quran Surahs in their exact order without deleting any of them.
     */
    fun importDeviceLibraryPlaylist(
        context: Context,
        devicePlaylist: DeviceLibraryPlaylist,
        customNameOverride: String = ""
    ) {
        val appContext = context.applicationContext ?: context
        val targetName = customNameOverride.trim().ifBlank { devicePlaylist.name }
        launchSafe("importDeviceLibraryPlaylist") {
            if (devicePlaylist.memberAudioIds.isNotEmpty()) {
                var existingById = currentTracksSnapshot().associateBy { it.id }
                val missingIds = devicePlaylist.memberAudioIds.any { "dev_ms_$it" !in existingById }
                if (missingIds && DeviceAudioScanner.hasAudioScanPermission(appContext)) {
                    val scanned = DeviceAudioScanner.scanPhoneMediaStore(
                        context = appContext,
                        autoIsolateQuran = currentSettingsSnapshot().autoIsolateQuran
                    )
                    if (scanned.isNotEmpty()) {
                        repository.syncScannedDeviceTracks(
                            userId = currentUserId,
                            scanned = scanned.distinctBy { it.track.sourceUri }.map { it.deterministicId to it.track }
                        )
                        val merged = HashMap(existingById)
                        for (item in scanned) {
                            merged[item.deterministicId] = item.track.copy(id = item.deterministicId)
                        }
                        existingById = merged
                    }
                }

                // Preserve ALL tracks in order — both songs AND Quran Surahs
                val resolvedTrackIds = devicePlaylist.memberAudioIds
                    .map { "dev_ms_$it" }
                    .filter { it in existingById }
                    .distinct()

                if (resolvedTrackIds.isNotEmpty()) {
                    val allQuran = resolvedTrackIds.all { existingById[it]?.quranAudio == true }
                    repository.createPlaylist(
                        CloudPlaylist(
                            name = targetName,
                            description = "Imported from Device Library",
                            trackIds = resolvedTrackIds,
                            sourceApp = "Device Library",
                            quranPlaylist = allQuran,
                            coverHex = if (allQuran) "#38B28B" else "#D98B5F"
                        )
                    )
                    val isAr = currentSettingsSnapshot().languageCode == "ar"
                    _statusBanner.value = if (isAr) {
                        "تم استيراد قائمة «$targetName» بكامل محتوياتها (${resolvedTrackIds.size} ملف صوتي)"
                    } else {
                        "Imported '$targetName' with all ${resolvedTrackIds.size} tracks intact"
                    }
                }
            } else if (devicePlaylist.fileUri != null) {
                val parsed = ExternalPlaylistImporter.parsePlaylistFromUri(
                    context = appContext,
                    uri = devicePlaylist.fileUri,
                    fallbackPlaylistName = targetName
                )
                processImportedPlaylist(parsed.copy(playlistName = targetName, sourceApp = "Device Library"))
            }
        }
    }

    /**
     * Resolves or creates all tracks in an imported/external playlist and stores them inside ONE
     * unified playlist in exact sequence — NEVER deleting or stripping out Holy Quran Surahs.
     */
    private suspend fun processImportedPlaylist(parsed: ImportedPlaylistResult) {
        val existingTracks = currentTracksSnapshot()
        val orderedTrackIds = mutableListOf<String>()
        var quranCount = 0

        for (entry in parsed.entries) {
            val match = ExternalPlaylistImporter.matchEntryToExistingTrack(entry, existingTracks)
            if (match != null) {
                orderedTrackIds.add(match.id)
                if (match.quranAudio) quranCount++
            } else if (entry.isPlayableStream ||
                entry.sourceUriOrQuery.startsWith("content://", true) ||
                entry.sourceUriOrQuery.startsWith("file://", true)
            ) {
                val cls = AudioClassifier.classify(
                    title = entry.title,
                    artist = entry.artist,
                    uriOrPath = entry.sourceUriOrQuery,
                    autoIsolateQuran = currentSettingsSnapshot().autoIsolateQuran
                )
                val created = repository.createTrack(
                    CloudTrack(
                        title = entry.title,
                        artist = entry.artist,
                        album = parsed.playlistName,
                        genre = parsed.sourceApp,
                        category = cls.category.id,
                        quranAudio = cls.isQuran,
                        durationMs = entry.durationMs,
                        sourceType = if (entry.sourceUriOrQuery.startsWith("http", true)) "STREAM_URL" else "EXTERNAL_IMPORT",
                        sourceUri = entry.sourceUriOrQuery,
                        accentHex = cls.suggestedAccentHex
                    )
                )
                created.getOrNull()?.let { id ->
                    orderedTrackIds.add(id)
                    if (cls.isQuran) quranCount++
                }
            }
        }

        val distinctOrderedIds = orderedTrackIds.distinct()
        if (distinctOrderedIds.isNotEmpty()) {
            val allQuran = quranCount == distinctOrderedIds.size
            repository.createPlaylist(
                CloudPlaylist(
                    name = parsed.playlistName,
                    description = "Imported from ${parsed.sourceApp}",
                    trackIds = distinctOrderedIds,
                    sourceApp = parsed.sourceApp,
                    quranPlaylist = allQuran,
                    coverHex = if (allQuran) "#38B28B" else "#D98B5F"
                )
            )
        }

        val isAr = currentSettingsSnapshot().languageCode == "ar"
        _statusBanner.value = if (isAr) {
            "تم استيراد القائمة بكامل عناصرها (${distinctOrderedIds.size} ملف صوتي بما فيها سور القرآن)"
        } else {
            "Imported playlist with all ${distinctOrderedIds.size} tracks intact"
        }
    }

    private fun importSharedAudioUrisAsPlaylist(
        context: Context,
        uris: List<Uri>,
        playlistName: String
    ) {
        if (uris.isEmpty()) return
        val appContext = context.applicationContext ?: context
        val autoIsolate = currentSettingsSnapshot().autoIsolateQuran
        val isAr = currentSettingsSnapshot().languageCode == "ar"

        launchSafe("importSharedAudioUrisAsPlaylist") {
            val existingByUri = currentTracksSnapshot().associateBy { it.sourceUri }
            val resolvedIds = mutableListOf<String>()
            var quranCount = 0

            for (uri in uris) {
                val existing = existingByUri[uri.toString()]
                if (existing != null) {
                    resolvedIds.add(existing.id)
                    if (existing.quranAudio) quranCount++
                } else {
                    val extracted = ExternalPlaylistImporter.extractDeviceAudioTrack(
                        context = appContext,
                        uri = uri,
                        autoIsolateQuran = autoIsolate
                    )
                    DeviceAudioScanner.registerLocalMeta(
                        context = appContext,
                        sourceUri = extracted.sourceUri,
                        folderName = extracted.album.ifBlank { "Shared" },
                        albumArtUri = ""
                    )
                    val created = repository.createTrack(extracted)
                    created.getOrNull()?.let { id ->
                        resolvedIds.add(id)
                        if (extracted.quranAudio) quranCount++
                    }
                }
            }

            val distinctIds = resolvedIds.distinct()
            if (distinctIds.isNotEmpty()) {
                val allQuran = quranCount == distinctIds.size
                repository.createPlaylist(
                    CloudPlaylist(
                        name = playlistName,
                        description = "Shared from External App",
                        trackIds = distinctIds,
                        sourceApp = "External App Share",
                        quranPlaylist = allQuran,
                        coverHex = if (allQuran) "#38B28B" else "#D98B5F"
                    )
                )
                _statusBanner.value = if (isAr) {
                    "تمت إضافة القائمة المشاركة (${distinctIds.size} ملف صوتي مع الاحتفاظ بسور القرآن)"
                } else {
                    "Added shared playlist (${distinctIds.size} tracks including Quran Surahs)"
                }
            }
        }
    }

    fun handleExternalIntent(context: Context, intent: Intent?) {
        if (intent == null) return
        try {
            when (intent.action) {
                Intent.ACTION_VIEW -> {
                    val dataUri = intent.data ?: return
                    val mime = intent.type.orEmpty().lowercase()
                    val path = dataUri.toString().lowercase()
                    if (mime.contains("mpegurl") || mime.contains("scpls") ||
                        path.endsWith(".m3u") || path.endsWith(".m3u8") || path.endsWith(".pls")
                    ) {
                        importExternalPlaylistFromUri(context, dataUri, "Imported Playlist")
                    } else {
                        importDeviceAudioFiles(context, listOf(dataUri))
                    }
                }
                Intent.ACTION_SEND -> {
                    val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                    }
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (streamUri != null) {
                        val mime = intent.type.orEmpty().lowercase()
                        val path = streamUri.toString().lowercase()
                        if (mime.contains("mpegurl") || mime.contains("scpls") ||
                            path.endsWith(".m3u") || path.endsWith(".m3u8") || path.endsWith(".pls") ||
                            mime == "text/plain"
                        ) {
                            importExternalPlaylistFromUri(context, streamUri, "Shared Playlist")
                        } else {
                            importSharedAudioUrisAsPlaylist(context, listOf(streamUri), "Shared Playlist")
                        }
                    } else if (!sharedText.isNullOrBlank()) {
                        importExternalPlaylistFromText(sharedText, "Shared Playlist")
                    }
                }
                Intent.ACTION_SEND_MULTIPLE -> {
                    val streamUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    }.orEmpty()
                    if (streamUris.isNotEmpty()) {
                        importSharedAudioUrisAsPlaylist(context, streamUris, "Shared Playlist")
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error handling external intent: ${t.message}")
        }
    }

    private companion object {
        const val TAG = "NaghamViewModel"
    }
}
