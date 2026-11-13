package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CloudPlaylist
import com.example.data.model.CloudTrack
import com.example.data.model.EqualizerPreset
import com.example.data.model.TrackCategory
import com.example.data.model.UserSettings
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MusicRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeDocId(rawId: String?): String {
        val cleaned = rawId
            ?.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            ?.take(120)
            .orEmpty()
        return cleaned.ifBlank { UUID.randomUUID().toString() }
    }

    private fun sanitizeHex(rawHex: String, fallback: String): String {
        val trimmed = rawHex.trim()
        return if (trimmed.length in 4..9 && trimmed.startsWith("#")) {
            trimmed
        } else {
            fallback
        }
    }

    private fun sanitizeTrackPayload(
        track: CloudTrack,
        docId: String,
        uid: String,
        createdAt: Timestamp?
    ): CloudTrack {
        val validCategories = setOf(
            "ARABIC_TARAB", "CHILL_NIGHT", "CLASSICAL",
            "ENERGETIC", "PODCAST", "OTHER_MUSIC", "QURAN"
        )
        val validSourceTypes = setOf(
            "STUDIO_STARTER", "LOCAL_URI", "STREAM_URL", "EXTERNAL_IMPORT"
        )
        val resolvedCategory = when {
            track.quranAudio || track.category == TrackCategory.QURAN.id -> TrackCategory.QURAN.id
            track.category in validCategories -> track.category
            else -> TrackCategory.OTHER_MUSIC.id
        }
        val isQuranConsistent = resolvedCategory == TrackCategory.QURAN.id
        val resolvedSourceType = if (track.sourceType in validSourceTypes) track.sourceType else "LOCAL_URI"

        return track.copy(
            id = docId,
            userId = uid,
            title = track.title.trim().ifBlank { "Untitled Track" }.take(200),
            artist = track.artist.trim().ifBlank { "Unknown Artist" }.take(150),
            album = track.album.trim().take(150),
            genre = track.genre.trim().take(80),
            category = resolvedCategory,
            quranAudio = isQuranConsistent,
            durationMs = track.durationMs.coerceIn(0L, 86_400_000L),
            sourceType = resolvedSourceType,
            sourceUri = track.sourceUri.trim().ifBlank { "content://media/external/audio/media/0" }.take(2000),
            lyrics = track.lyrics.take(15_000),
            playCount = track.playCount.coerceIn(0L, 10_000_000L),
            accentHex = sanitizeHex(track.accentHex, if (isQuranConsistent) "#38B28B" else "#E8A855"),
            createdAt = createdAt,
            updatedAt = null
        )
    }

    fun observeUserSettings(userId: String): Flow<UserSettings?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observeTracks(userId: String): Flow<List<CloudTrack>> {
        val path = "users/$userId/tracks"
        return db.collection("users").document(userId).collection("tracks")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudTrack::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observePlaylists(userId: String): Flow<List<CloudPlaylist>> {
        val path = "users/$userId/playlists"
        return db.collection("users").document(userId).collection("playlists")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudPlaylist::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun getTrackById(ownerUid: String, trackId: String): Result<CloudTrack?> {
        val docRef = db.collection("users").document(ownerUid).collection("tracks").document(trackId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    CloudTrack::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveUserSettings(settings: UserSettings): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existingSnap = docRef.get().await()
            val normalizedBands = if (settings.eqBands.size == 5) {
                settings.eqBands.map { it.coerceIn(-1500L, 1500L) }
            } else {
                EqualizerPreset.WARM_OUD.bandsMb
            }
            val validPresets = setOf(
                "FLAT", "WARM_OUD", "BASS_BOOST", "VOCAL_CLEAR",
                "NIGHT_COMFORT", "CLASSICAL", "CUSTOM"
            )
            val safeDisplayName = settings.displayName.trim().ifBlank { "مستمع نغم" }.take(100)
            val safeLang = if (settings.languageCode == "en") "en" else "ar"
            val safePreset = if (settings.eqPreset in validPresets) settings.eqPreset else EqualizerPreset.WARM_OUD.id
            val safeBass = settings.bassBoostStrength.coerceIn(0L, 1000L)

            val payload = if (existingSnap.exists()) {
                val existingCreated = existingSnap.getTimestamp(
                    "createdAt",
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                settings.copy(
                    id = uid,
                    userId = uid,
                    displayName = safeDisplayName,
                    languageCode = safeLang,
                    eqPreset = safePreset,
                    eqBands = normalizedBands,
                    bassBoostStrength = safeBass,
                    createdAt = existingCreated,
                    updatedAt = null
                )
            } else {
                settings.copy(
                    id = uid,
                    userId = uid,
                    displayName = safeDisplayName,
                    languageCode = safeLang,
                    eqPreset = safePreset,
                    eqBands = normalizedBands,
                    bassBoostStrength = safeBass,
                    createdAt = null,
                    updatedAt = null
                )
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createTrack(track: CloudTrack, customId: String? = null): Result<String> {
        val uid = requireUserId()
        val docId = sanitizeDocId(customId)
        val docRef = db.collection("users").document(uid).collection("tracks").document(docId)
        val sanitized = sanitizeTrackPayload(track, docId = docId, uid = uid, createdAt = null)
        return try {
            docRef.set(sanitized).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTrack(track: CloudTrack): Result<Unit> {
        val uid = requireUserId()
        require(track.id.isNotBlank()) { "Track ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("tracks").document(track.id)
        return try {
            val resolvedCreatedAt = track.createdAt ?: docRef.get().await().getTimestamp(
                "createdAt",
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            requireNotNull(resolvedCreatedAt) { "Track createdAt must not be null for update" }
            val updated = sanitizeTrackPayload(track, docId = track.id, uid = uid, createdAt = resolvedCreatedAt)
            docRef.set(updated).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTrack(trackId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("tracks").document(trackId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createPlaylist(playlist: CloudPlaylist, customId: String? = null): Result<String> {
        val uid = requireUserId()
        val docId = sanitizeDocId(customId)
        val docRef = db.collection("users").document(uid).collection("playlists").document(docId)
        val sanitized = playlist.copy(
            id = docId,
            userId = uid,
            name = playlist.name.trim().ifBlank { "قائمة جديدة" }.take(120),
            description = playlist.description.trim().take(300),
            trackIds = playlist.trackIds.filter { it.isNotBlank() && it.length <= 128 }.distinct().take(200),
            sourceApp = playlist.sourceApp.trim().ifBlank { "Nagham" }.take(100),
            coverHex = sanitizeHex(playlist.coverHex, "#E8A855"),
            createdAt = null,
            updatedAt = null
        )
        return try {
            docRef.set(sanitized).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePlaylist(playlist: CloudPlaylist): Result<Unit> {
        val uid = requireUserId()
        require(playlist.id.isNotBlank()) { "Playlist ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("playlists").document(playlist.id)
        return try {
            val resolvedCreatedAt = playlist.createdAt ?: docRef.get().await().getTimestamp(
                "createdAt",
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            requireNotNull(resolvedCreatedAt) { "Playlist createdAt must not be null for update" }
            val updated = playlist.copy(
                userId = uid,
                name = playlist.name.trim().ifBlank { "قائمة جديدة" }.take(120),
                description = playlist.description.trim().take(300),
                trackIds = playlist.trackIds.filter { it.isNotBlank() && it.length <= 128 }.distinct().take(200),
                sourceApp = playlist.sourceApp.trim().ifBlank { "Nagham" }.take(100),
                coverHex = sanitizeHex(playlist.coverHex, "#E8A855"),
                createdAt = resolvedCreatedAt,
                updatedAt = null
            )
            docRef.set(updated).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(playlistId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("playlists").document(playlistId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun initializeUserAndPurgeLegacyStarters(userId: String, displayName: String) {
        val settingsRef = db.collection("users").document(userId)
        val tracksCol = settingsRef.collection("tracks")
        val playlistsCol = settingsRef.collection("playlists")

        val legacyTrackIds = setOf(
            "starter_oud_hijaz",
            "starter_midnight_lofi",
            "starter_andalus_strings",
            "starter_desert_horizon",
            "quran_surah_fatihah",
            "quran_surah_ikhlas"
        )
        val legacyPlaylistIds = setOf(
            "playlist_night_comfort",
            "playlist_quran_sanctuary"
        )

        try {
            val existingSettings = settingsRef.get().await()
            if (!existingSettings.exists()) {
                saveUserSettings(
                    UserSettings(
                        userId = userId,
                        displayName = displayName.trim().ifBlank { "مستمع نغم" }.take(100),
                        languageCode = "ar",
                        darkTheme = true,
                        eyeComfortMode = true,
                        autoIsolateQuran = true
                    )
                )
            }

            // Actively purge any pre-generated starter tracks from previous versions
            val snap = tracksCol.whereEqualTo("userId", userId).get().await()
            for (doc in snap.documents) {
                val sourceType = doc.getString("sourceType").orEmpty()
                val sourceUri = doc.getString("sourceUri").orEmpty()
                if (doc.id in legacyTrackIds ||
                    sourceType == "STUDIO_STARTER" ||
                    sourceUri.startsWith("starter://")
                ) {
                    doc.reference.delete().await()
                }
            }

            // Actively purge legacy starter playlists
            val plSnap = playlistsCol.whereEqualTo("userId", userId).get().await()
            for (doc in plSnap.documents) {
                if (doc.id in legacyPlaylistIds) {
                    doc.reference.delete().await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId/init")
        }
    }

    suspend fun syncScannedDeviceTracks(
        userId: String,
        scanned: List<Pair<String, CloudTrack>>
    ): Pair<Int, Int> {
        if (scanned.isEmpty()) return 0 to 0
        val uid = auth.currentUser?.uid ?: return 0 to 0
        if (uid != userId) return 0 to 0

        val tracksCol = db.collection("users").document(uid).collection("tracks")
        var addedMusic = 0
        var addedQuran = 0
        try {
            val existingSnap = tracksCol.whereEqualTo("userId", uid).get().await()
            val existingIds = existingSnap.documents.map { it.id }.toHashSet()
            val existingUris = existingSnap.documents.mapNotNull { it.getString("sourceUri") }.toHashSet()

            val toCreate = scanned.mapNotNull { (rawId, candidate) ->
                val docId = sanitizeDocId(rawId)
                if (docId in existingIds || candidate.sourceUri in existingUris) {
                    null
                } else {
                    existingIds.add(docId)
                    existingUris.add(candidate.sourceUri)
                    docId to sanitizeTrackPayload(candidate, docId = docId, uid = uid, createdAt = null)
                }
            }

            // Commit in fast Firestore WriteBatches of 200 to prevent main/IO exhaustion on 1000+ song phones
            for (chunk in toCreate.chunked(200)) {
                try {
                    val batch = db.batch()
                    for ((docId, sanitizedTrack) in chunk) {
                        batch.set(tracksCol.document(docId), sanitizedTrack)
                    }
                    batch.commit().await()
                    for ((_, sanitizedTrack) in chunk) {
                        if (sanitizedTrack.quranAudio) addedQuran++ else addedMusic++
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (batchErr: Exception) {
                    handleFirestoreError(batchErr, OperationType.CREATE, "users/$uid/tracks")
                    // Fallback per-item so one unusual document never blocks the rest
                    for ((docId, sanitizedTrack) in chunk) {
                        val res = createTrack(sanitizedTrack, customId = docId)
                        if (res.isSuccess) {
                            if (sanitizedTrack.quranAudio) addedQuran++ else addedMusic++
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/tracks")
        }
        return addedMusic to addedQuran
    }
}
