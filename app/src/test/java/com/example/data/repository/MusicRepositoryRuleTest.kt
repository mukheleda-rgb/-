package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CloudPlaylist
import com.example.data.model.CloudTrack
import com.example.data.model.EqualizerPreset
import com.example.data.model.TrackCategory
import com.example.data.model.UserSettings
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class MusicRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveTrack_authenticatedOwner_succeedsAndSyncs() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = MusicRepository(firestore)
        val uniqueId = "track_${UUID.randomUUID()}"

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTrack(
                CloudTrack(
                    title = "تقاسيم عود شرقية",
                    artist = "فرقة نغم",
                    album = "ليالي",
                    genre = "Arabic Oud",
                    category = TrackCategory.ARABIC_TARAB.id,
                    quranAudio = false,
                    durationMs = 24000L,
                    sourceType = "STUDIO_STARTER",
                    sourceUri = "starter://oud_hijaz",
                    lyrics = "[00:01.00] تقاسيم عود",
                    favorite = true,
                    playCount = 1L,
                    accentHex = "#E8A855"
                ),
                customId = uniqueId
            )
        }
        assertTrue("Expected track creation to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)

        val emittedTracks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTracks(aliceUid).first { list -> list.any { it.id == uniqueId } }
        }
        val synced = emittedTracks.first { it.id == uniqueId }
        assertEquals("تقاسيم عود شرقية", synced.title)
        assertEquals(false, synced.quranAudio)
    }

    @Test
    fun saveUserSettingsAndCreatePlaylist_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = MusicRepository(firestore)

        val settingsResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserSettings(
                UserSettings(
                    userId = aliceUid,
                    displayName = "Alice Listener",
                    languageCode = "ar",
                    darkTheme = true,
                    eyeComfortMode = true,
                    autoIsolateQuran = true,
                    eqPreset = EqualizerPreset.NIGHT_COMFORT.id,
                    eqBands = EqualizerPreset.NIGHT_COMFORT.bandsMb,
                    bassBoostStrength = 200L
                )
            )
        }
        assertTrue("Expected settings save to succeed: ${settingsResult.exceptionOrNull()}", settingsResult.isSuccess)

        val playlistId = "pl_${UUID.randomUUID()}"
        val playlistResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createPlaylist(
                CloudPlaylist(
                    userId = aliceUid,
                    name = "قائمة الاسترخاء",
                    description = "مزامنة سحابية",
                    trackIds = listOf("starter_oud_hijaz"),
                    sourceApp = "M3U Import",
                    quranPlaylist = false,
                    coverHex = "#E8A855"
                ),
                customId = playlistId
            )
        }
        assertTrue("Expected playlist creation to succeed: ${playlistResult.exceptionOrNull()}", playlistResult.isSuccess)

        val emittedPlaylists = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePlaylists(aliceUid).first { list -> list.any { it.id == playlistId } }
        }
        assertTrue(emittedPlaylists.any { it.id == playlistId && it.sourceApp == "M3U Import" })
    }

    @Test
    fun getTrackById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = MusicRepository(firestore)
        val aliceTrackId = "track_${UUID.randomUUID()}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createTrack(
                CloudTrack(
                    title = "Alice Private Song",
                    artist = "Alice",
                    category = TrackCategory.CHILL_NIGHT.id,
                    quranAudio = false,
                    durationMs = 15000L,
                    sourceType = "STUDIO_STARTER",
                    sourceUri = "starter://chill_night"
                ),
                customId = aliceTrackId
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = MusicRepository(firestore)
        val crossRead = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getTrackById(aliceUid, aliceTrackId)
        }
        assertTrue("Expected cross-user read to fail", crossRead.isFailure)
        val fsEx = crossRead.exceptionOrNull() as? FirebaseFirestoreException
        assertNotNull("Expected FirebaseFirestoreException", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeTracks_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = MusicRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTracks("unauth_target_uid").first()
            }
            fail("Expected unauthenticated observeTracks to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_nagham@test.com"
        const val BOB_EMAIL = "bob_nagham@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
