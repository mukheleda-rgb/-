package com.example

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioPlayerController
import com.example.audio.NaghamMediaService
import com.example.audio.PlaybackState
import com.example.data.firebase.configureAppCheck
import com.example.data.importer.ExternalPlaylistImporter
import com.example.data.model.AudioClassifier
import com.example.data.model.CloudPlaylist
import com.example.data.model.CloudTrack
import com.example.data.model.EqualizerPreset
import com.example.data.model.TrackCategory
import com.example.data.model.UserSettings
import com.example.data.scanner.DeviceAudioScanner
import com.example.data.scanner.FolderFilterStore
import com.example.data.scanner.RecentPlaybackStore
import com.example.data.scanner.TrackSortOption
import com.example.data.scanner.TrackSortStore
import com.example.ui.components.DraggableNavTab
import com.example.ui.components.HorizontalSpectrumBarStrip
import com.example.ui.components.LiquidSoundwaveBackdrop
import com.example.ui.components.PlayerArtworkWithEffects
import com.example.ui.components.ReorderableBottomNavigationBar
import com.example.ui.components.SmoothTabSwipeDeck
import com.example.ui.components.TrackLongPressSheetContent
import com.example.ui.components.rememberSmoothTabSwipeDeckState
import com.example.ui.navigation.BasicMusicNavHost
import com.example.ui.navigation.NaghamAppNavHost
import com.example.ui.navigation.PlayerScreen
import com.example.ui.navigation.ROUTE_EQUALIZER
import com.example.ui.navigation.ROUTE_PLAYER
import com.example.ui.navigation.ROUTE_TABS_HOST
import com.example.ui.navigation.navigateSingleTopTo
import com.example.ui.navigation.popOrNavigateToStart
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.EqualizerResponseCurveCanvas
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.FullNowPlayingOverlay
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.QuranSanctuaryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AppColorPalette
import com.example.ui.theme.AppDesignStyle
import com.example.ui.theme.LocalNaghamDesign
import com.example.ui.theme.NaghamDarkColorScheme
import com.example.ui.theme.NaghamLightColorScheme
import com.example.ui.theme.NaghamTheme
import com.example.ui.theme.NaghamThemedBackdrop
import com.example.ui.theme.PlayerThemeStyle
import com.example.ui.theme.PlayerVisualEffect
import com.example.ui.theme.ThemePreferences
import com.example.ui.theme.buildPaletteDarkScheme
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nagham Player", appName)
    }

    @Test
    fun `settings screen switches light and dark theme, color palettes, and language`() {
        var settings by mutableStateOf(
            UserSettings(
                userId = "u1",
                darkTheme = true,
                eyeComfortMode = false,
                languageCode = "ar"
            )
        )
        var palette by mutableStateOf(AppColorPalette.AMBER_GOLD)
        var effect by mutableStateOf(PlayerVisualEffect.VINYL_AURA)
        var currentBackground: Color = Color.Unspecified
        var currentPrimary: Color = Color.Unspecified

        composeTestRule.setContent {
            NaghamTheme(
                darkTheme = settings.darkTheme,
                eyeComfortMode = settings.eyeComfortMode,
                colorPalette = palette,
                languageCode = settings.languageCode
            ) {
                currentBackground = MaterialTheme.colorScheme.background
                currentPrimary = MaterialTheme.colorScheme.primary
                SettingsScreen(
                    settings = settings,
                    selectedPalette = palette,
                    selectedVisualEffect = effect,
                    isScanningStorage = false,
                    onDarkThemeChange = { settings = settings.copy(darkTheme = it) },
                    onToggleEyeComfort = { settings = settings.copy(eyeComfortMode = !settings.eyeComfortMode) },
                    onSelectPalette = { palette = it },
                    onSelectVisualEffect = { effect = it },
                    onLanguageChange = { settings = settings.copy(languageCode = it) },
                    onToggleAutoIsolateQuran = {},
                    onResetTabOrder = {},
                    onSignOut = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(NaghamDarkColorScheme.background, currentBackground)

        // Toggle Light mode in Settings
        composeTestRule.onNodeWithTag("theme_light_button").performClick()
        composeTestRule.waitForIdle()
        assertFalse(settings.darkTheme)
        assertEquals(NaghamLightColorScheme.background, currentBackground)

        // Toggle back to Dark mode and select Royal Sapphire color palette
        composeTestRule.onNodeWithTag("theme_dark_button").performClick()
        composeTestRule.onNodeWithTag("settings_screen_list")
            .performScrollToNode(hasTestTag("palette_option_ROYAL_SAPPHIRE"))
        composeTestRule.onNodeWithTag("palette_option_ROYAL_SAPPHIRE")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(AppColorPalette.ROYAL_SAPPHIRE, palette)
        assertEquals(buildPaletteDarkScheme(AppColorPalette.ROYAL_SAPPHIRE).primary, currentPrimary)

        // Switch language inside Settings
        composeTestRule.onNodeWithTag("settings_screen_list")
            .performScrollToNode(hasTestTag("lang_english_button"))
        composeTestRule.onNodeWithTag("lang_english_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals("en", settings.languageCode)
    }

    @Test
    fun `home screen renders bento and square track tiles, isolates Quran, and triggers long-press`() {
        val musicTrack1 = CloudTrack(
            id = "m1",
            title = "Oud Al Layl",
            artist = "Sharq Ensemble",
            quranAudio = false,
            playCount = 9L,
            sourceUri = "content://media/external/audio/media/1"
        )
        val musicTrack2 = CloudTrack(
            id = "m2",
            title = "Desert Breeze",
            artist = "Nour",
            quranAudio = false,
            playCount = 2L,
            sourceUri = "content://media/external/audio/media/2"
        )
        val quranTrack = CloudTrack(
            id = "q1",
            title = "سورة الكهف",
            artist = "القارئ",
            category = TrackCategory.QURAN.id,
            quranAudio = true,
            sourceUri = "content://media/external/audio/media/3"
        )

        var longPressed: CloudTrack? = null

        composeTestRule.setContent {
            NaghamTheme(darkTheme = true, eyeComfortMode = false, languageCode = "ar") {
                HomeScreen(
                    allTracks = listOf(musicTrack1, musicTrack2, quranTrack),
                    recentTimestamps = mapOf("m1" to 5_000_000L),
                    currentTrackId = "m1",
                    isPlaying = true,
                    isScanningStorage = false,
                    onPlayMusicTrack = { _, _ -> },
                    onLongPressTrack = { longPressed = it },
                    onJumpToQuranSanctuary = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_bento_recent_played").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_bento_recent_added").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_bento_most_played").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_bento_quran").assertIsDisplayed()

        // Ensure Quran track never appears inside Home music tiles
        assertEquals(0, composeTestRule.onAllNodesWithTag("home_tile_recent_added_q1").fetchSemanticsNodes().size)

        // Scroll to music tile and long-press it
        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("home_tile_recent_played_m1"))
        composeTestRule.onNodeWithTag("home_tile_recent_played_m1")
            .performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        assertNotNull(longPressed)
        assertEquals("m1", longPressed?.id)
    }

    @Test
    fun `long-press sheet supports playlist add, move between Quran and Music, edit, and delete`() {
        val sampleTrack = CloudTrack(
            id = "q_surah_1",
            title = "سورة يس",
            artist = "القارئ",
            quranAudio = true,
            sourceUri = "content://media/external/audio/media/10"
        )
        val playlist = CloudPlaylist(
            id = "pl1",
            name = "Morning Favorites",
            quranPlaylist = false,
            trackIds = emptyList()
        )

        var movedToQuran: Boolean? = null
        var addedToPlaylistId: String? = null

        composeTestRule.setContent {
            NaghamTheme(darkTheme = true, eyeComfortMode = false, languageCode = "ar") {
                TrackLongPressSheetContent(
                    track = sampleTrack,
                    playlists = listOf(playlist),
                    onDismiss = {},
                    onAddTrackToPlaylist = { _, pl -> addedToPlaylistId = pl.id },
                    onCreatePlaylistWithTrack = { _, _ -> },
                    onMoveTrackIsolation = { _, toQuran -> movedToQuran = toQuran },
                    onEditTrack = {},
                    onDeleteTrack = {}
                )
            }
        }

        // Move to Music action
        composeTestRule.onNodeWithTag("long_press_move_category").performClick()
        composeTestRule.waitForIdle()
        assertEquals(false, movedToQuran)

        // Even for a Quran Surah, general playlists are shown and selectable
        composeTestRule.onNodeWithTag("long_press_add_to_playlist").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sheet_playlist_item_pl1").performClick()
        composeTestRule.waitForIdle()
        assertEquals("pl1", addedToPlaylistId)
    }

    @Test
    fun `simplified player overlay switches tracks on horizontal swipe and opens lyrics and equalizer`() {
        val sampleTrack = CloudTrack(
            id = "dev_ms_101",
            title = "Oud Horizon",
            artist = "Local Artist",
            album = "My Folder",
            quranAudio = false,
            durationMs = 180_000L,
            sourceType = "LOCAL_URI",
            sourceUri = "content://media/external/audio/media/101"
        )
        val playbackState = PlaybackState(
            currentTrack = sampleTrack,
            queue = listOf(sampleTrack, sampleTrack.copy(id = "dev_ms_102", title = "Second Track")),
            isPlaying = true,
            currentPositionMs = 12_000L,
            durationMs = 180_000L
        )

        var skippedNextCount = 0
        var swipedPrevCount = 0

        composeTestRule.setContent {
            NaghamTheme(darkTheme = true, eyeComfortMode = false, languageCode = "ar") {
                FullNowPlayingOverlay(
                    playbackState = playbackState,
                    onCollapse = {},
                    onTogglePlayPause = {},
                    onSkipNext = { skippedNextCount++ },
                    onSkipPrevious = {},
                    onSwipePreviousTrack = { swipedPrevCount++ },
                    onSeekTo = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onEditTrack = {},
                    onSelectPreset = {},
                    onBandChanged = { _, _ -> },
                    onBassChanged = {},
                    onSpeedChanged = {},
                    onSyncEqToCloud = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        // Verify visual effect chips and segmented button bar clutter are gone
        assertEquals(
            0,
            composeTestRule.onAllNodesWithTag("player_inline_mode_switcher").fetchSemanticsNodes().size
        )

        // Swipe left on artwork area -> Next track
        composeTestRule.onNodeWithTag("player_artwork_swipe_area")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertEquals(1, skippedNextCount)

        // Swipe right on artwork area -> Previous track
        composeTestRule.onNodeWithTag("player_artwork_swipe_area")
            .performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertEquals(1, swipedPrevCount)

        // Open Equalizer inline via the top icon button
        composeTestRule.onNodeWithTag("player_open_eq_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("inline_equalizer_panel").assertIsDisplayed()
    }

    @Test
    fun `imported and library playlists preserve Quran Surahs alongside songs and support drag handle reordering`() {
        val songTrack = CloudTrack(
            id = "m1",
            title = "Ziryab Dawn",
            artist = "Sharq Ensemble",
            quranAudio = false,
            durationMs = 240_000L,
            sourceUri = "file:///storage/emulated/0/Music/ziryab_dawn.mp3"
        )
        val surahTrack = CloudTrack(
            id = "q1",
            title = "سورة الكهف",
            artist = "مشاري العفاسي",
            category = TrackCategory.QURAN.id,
            quranAudio = true,
            durationMs = 960_000L,
            sourceUri = "file:///storage/emulated/0/Quran/018_alkahf.mp3"
        )
        val songTrack2 = CloudTrack(
            id = "m2",
            title = "Alhambra Breeze",
            artist = "Nour",
            quranAudio = false,
            durationMs = 150_000L,
            sourceUri = "file:///storage/emulated/0/Music/alhambra.mp3"
        )
        val allTracks = listOf(songTrack, surahTrack, songTrack2)

        // 1. Verify ExternalPlaylistImporter matches BOTH music and Quran Surahs without dropping Surahs
        val m3uContent = """
            #EXTM3U
            #EXTINF:240,Sharq Ensemble - Ziryab Dawn
            /storage/emulated/0/Music/ziryab_dawn.mp3
            #EXTINF:960,مشاري العفاسي - سورة الكهف
            /storage/emulated/0/Quran/018_alkahf.mp3
            #EXTINF:150,Nour - Alhambra Breeze
            /storage/emulated/0/Music/alhambra.mp3
        """.trimIndent()
        val parsed = ExternalPlaylistImporter.parseRawPlaylistText(m3uContent, "Roadtrip Mix", "External M3U")
        assertEquals(3, parsed.entries.size)
        val matchedIds = parsed.entries.mapNotNull { entry ->
            ExternalPlaylistImporter.matchEntryToExistingTrack(entry, allTracks)?.id
        }
        assertEquals(listOf("m1", "q1", "m2"), matchedIds)

        // 2. Verify PlaylistsScreen displays & plays ALL tracks (including Quran Surahs) and supports drag-handle reordering
        var playlistState by mutableStateOf(
            CloudPlaylist(
                id = "pl_mixed",
                name = "Roadtrip Mix",
                sourceApp = "External M3U",
                quranPlaylist = false,
                trackIds = matchedIds
            )
        )
        var playedQueueIds: List<String> = emptyList()

        composeTestRule.setContent {
            NaghamTheme(darkTheme = true, eyeComfortMode = false, languageCode = "ar") {
                PlaylistsScreen(
                    playlists = listOf(playlistState),
                    allTracks = allTracks,
                    onPlayMusicQueue = { queue, _ -> playedQueueIds = queue.map { it.id } },
                    onCreatePlaylist = { _, _, _ -> },
                    onUpdatePlaylistTracks = { updatedPl, newIds ->
                        playlistState = updatedPl.copy(trackIds = newIds)
                    },
                    onDeletePlaylist = {},
                    onOpenImportExternalDialog = {},
                    onEditTrack = {},
                    onToggleFavorite = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("playlist_card_pl_mixed").performClick()
        composeTestRule.waitForIdle()

        // Both song tracks AND the Quran Surah must be present & displayable inside the playlist
        composeTestRule.onNodeWithTag("playlist_detail_tracks_list")
            .performScrollToNode(hasTestTag("track_item_card_m1"))
        composeTestRule.onNodeWithTag("track_item_card_m1").assertIsDisplayed()

        composeTestRule.onNodeWithTag("playlist_detail_tracks_list")
            .performScrollToNode(hasTestTag("track_item_card_q1"))
        composeTestRule.onNodeWithTag("track_item_card_q1").assertIsDisplayed()

        composeTestRule.onNodeWithTag("playlist_detail_tracks_list")
            .performScrollToNode(hasTestTag("track_item_card_m2"))
        composeTestRule.onNodeWithTag("track_item_card_m2").assertIsDisplayed()

        // Playing the playlist plays all 3 items including the Quran Surah
        composeTestRule.onNodeWithTag("playlist_detail_tracks_list")
            .performScrollToNode(hasTestTag("play_playlist_all_button"))
        composeTestRule.onNodeWithTag("play_playlist_all_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf("m1", "q1", "m2"), playedQueueIds)

        // Scroll so both m1 and q1 are visible together, then drag m1's dedicated handle downward past q1
        composeTestRule.onNodeWithTag("playlist_detail_tracks_list")
            .performScrollToNode(hasTestTag("track_item_card_q1"))
        composeTestRule.onNodeWithTag("playlist_detail_tracks_list")
            .performScrollToNode(hasTestTag("drag_handle_m1"))
        composeTestRule.onNodeWithTag("drag_handle_m1").performTouchInput {
            down(center)
            moveBy(Offset(0f, 160f))
            up()
        }
        composeTestRule.waitForIdle()
        assertTrue(
            "Dragging m1 downward via its dedicated drag handle must reorder playlist tracks while keeping q1",
            playlistState.trackIds.contains("q1") && playlistState.trackIds.first() != "m1"
        )
    }

    @Test
    fun `track sorting by name, date, duration, and custom drag handle works in Library and Quran tabs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        TrackSortStore.ensureLoaded(context)

        val tAlpha = CloudTrack(id = "t_alpha", title = "Alpha Oud", durationMs = 120_000L, quranAudio = false)
        val tBeta = CloudTrack(id = "t_beta", title = "Beta Nay", durationMs = 360_000L, quranAudio = false)
        val tGamma = CloudTrack(id = "t_gamma", title = "Gamma Qanun", durationMs = 210_000L, quranAudio = false)
        val tracks = listOf(tGamma, tAlpha, tBeta)
        val fakeDates = mapOf("t_alpha" to 1000L, "t_beta" to 2000L, "t_gamma" to 3000L)

        // Verify deterministic sorting logic
        assertEquals(
            listOf("t_alpha", "t_beta", "t_gamma"),
            TrackSortStore.sortTracks(tracks, TrackSortOption.NAME, emptyList()).map { it.id }
        )
        assertEquals(
            listOf("t_gamma", "t_beta", "t_alpha"),
            TrackSortStore.sortTracks(tracks, TrackSortOption.DATE, emptyList()) { fakeDates[it.id] ?: 0L }.map { it.id }
        )
        assertEquals(
            listOf("t_beta", "t_gamma", "t_alpha"),
            TrackSortStore.sortTracks(tracks, TrackSortOption.DURATION, emptyList()).map { it.id }
        )

        // Verify LibraryScreen single-row toolbar hides sub-options until Sort button is clicked
        FolderFilterStore.resetDefaults(context)
        TrackSortStore.setMusicSortOption(context, TrackSortOption.NAME)
        composeTestRule.setContent {
            NaghamTheme(darkTheme = true, eyeComfortMode = false, languageCode = "ar") {
                LibraryScreen(
                    allTracks = tracks,
                    currentTrackId = null,
                    isPlaying = false,
                    isScanningStorage = false,
                    onPlayMusicTrack = { _, _ -> },
                    onToggleFavorite = {},
                    onEditTrack = {},
                    onJumpToQuranSanctuary = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("library_tracks_list")
            .performScrollToNode(hasTestTag("library_sort_bar"))
        assertEquals(0, composeTestRule.onAllNodesWithTag("library_sort_DURATION").fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag("library_sort_menu_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("library_sort_DURATION").performClick()
        composeTestRule.waitForIdle()
        assertEquals(TrackSortOption.DURATION, TrackSortStore.musicSortOption.value)

        // Scroll so both t_beta (1st) and t_gamma (2nd) are visible, then drag t_beta's handle downward
        composeTestRule.onNodeWithTag("library_tracks_list")
            .performScrollToNode(hasTestTag("track_item_card_t_gamma"))
        composeTestRule.onNodeWithTag("library_tracks_list")
            .performScrollToNode(hasTestTag("drag_handle_t_beta"))
        composeTestRule.onNodeWithTag("drag_handle_t_beta").performTouchInput {
            down(center)
            moveBy(Offset(0f, 160f))
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(TrackSortOption.CUSTOM, TrackSortStore.musicSortOption.value)
        assertTrue(TrackSortStore.musicCustomOrder.value.first() != "t_beta")
    }

    @Test
    fun `quran tab supports sorting and custom drag handle reordering`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        TrackSortStore.ensureLoaded(context)
        TrackSortStore.setQuranSortOption(context, TrackSortOption.DURATION)

        val qShort = CloudTrack(id = "q_short", title = "سورة الإخلاص", durationMs = 45_000L, quranAudio = true)
        val qLong = CloudTrack(id = "q_long", title = "سورة الملك", durationMs = 520_000L, quranAudio = true)

        composeTestRule.setContent {
            NaghamTheme(darkTheme = true, eyeComfortMode = false, languageCode = "ar") {
                QuranSanctuaryScreen(
                    allTracks = listOf(qShort, qLong),
                    currentTrackId = null,
                    isPlaying = false,
                    isScanningStorage = false,
                    onPlayQuranQueue = { _, _ -> },
                    onEditTrack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quran_tracks_list")
            .performScrollToNode(hasTestTag("drag_handle_q_long"))
        composeTestRule.onNodeWithTag("drag_handle_q_long").performTouchInput {
            down(center)
            moveBy(Offset(0f, 220f))
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(TrackSortOption.CUSTOM, TrackSortStore.quranSortOption.value)
        assertEquals(listOf("q_short", "q_long"), TrackSortStore.quranCustomOrder.value)
    }

    @Test
    fun `bottom navigation bar keeps dragged tab continuously floating across multiple positions until released`() {
        val initialTabs = listOf(
            DraggableNavTab("home", "Home", Icons.Default.Home, Icons.Default.Home, "nav_tab_home"),
            DraggableNavTab("library", "Library", Icons.Default.LibraryMusic, Icons.Default.LibraryMusic, "nav_tab_library"),
            DraggableNavTab("playlists", "Playlists", Icons.AutoMirrored.Filled.QueueMusic, Icons.AutoMirrored.Filled.QueueMusic, "nav_tab_playlists"),
            DraggableNavTab("quran", "Quran", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Filled.MenuBook, "nav_tab_quran"),
            DraggableNavTab("settings", "Settings", Icons.Default.Settings, Icons.Default.Settings, "nav_tab_settings")
        )
        var orderedTabs by mutableStateOf(initialTabs)

        composeTestRule.setContent {
            NaghamTheme(darkTheme = true, eyeComfortMode = false, languageCode = "en") {
                Box(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                    ReorderableBottomNavigationBar(
                        tabs = orderedTabs,
                        selectedRoute = "home",
                        onSelectTab = { _, _ -> },
                        onMoveTab = { fromIdx, toIdx ->
                            val m = orderedTabs.toMutableList()
                            val item = m.removeAt(fromIdx)
                            m.add(toIdx, item)
                            orderedTabs = m
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Long-press on first tab (index 0) and drag continuously all the way to the last slot (index 4)
        // WITHOUT releasing finger in between!
        composeTestRule.onNodeWithTag("reorderable_bottom_nav").performTouchInput {
            val startX = width * 0.10f
            val endX = width * 0.90f
            val cy = centerY
            down(Offset(startX, cy))
            advanceEventTime(700L)
            val steps = 8
            for (step in 1..steps) {
                val dx = (endX - startX) / steps
                moveBy(Offset(dx, 0f), delayMillis = 16L)
            }
            up()
        }
        composeTestRule.waitForIdle()

        // Verify 'home' did NOT stop at index 1, and instead floated all the way to index 4!
        assertEquals(
            "Tab must keep floating across all crossed slots until finger release",
            "home",
            orderedTabs.last().route
        )
    }

    @Test
    fun `tab order persistence with home tab and deleted-track dismissal work accurately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertFalse("ai_taste" in ThemePreferences.DEFAULT_TABS)
        assertTrue("home" in ThemePreferences.DEFAULT_TABS)

        val reordered = listOf("quran_sanctuary", "home", "library", "playlists", "settings")
        ThemePreferences.saveTabOrder(context, reordered)
        assertEquals(reordered, ThemePreferences.getTabOrder(context))

        DeviceAudioScanner.markTrackDeletedByUser(context, "dev_ms_999", "content://media/external/audio/media/999")
        assertTrue(DeviceAudioScanner.isDeletedByUser(context, "dev_ms_999", "content://media/external/audio/media/999"))

        val starterDir = File(context.filesDir, "starter_audio").apply { mkdirs() }
        File(starterDir, "legacy.wav").writeText("old")
        assertTrue(starterDir.exists())
        DeviceAudioScanner.deleteLegacyStarterAudioFiles(context)
        assertFalse(starterDir.exists())
    }

    @Test
    fun `audio classifier isolates Quran files in Arabic and English without false positives`() {
        val arabicQuran = AudioClassifier.classify(
            title = "سورة الرحمن",
            artist = "الشيخ مشاري العفاسي"
        )
        assertTrue(arabicQuran.isQuran)
        assertEquals(TrackCategory.QURAN, arabicQuran.category)

        val folderQuran = AudioClassifier.classify(
            title = "018",
            artist = "Local File",
            uriOrPath = "Music/القرآن الكريم/018.mp3"
        )
        assertTrue(folderQuran.isQuran)
        assertEquals(TrackCategory.QURAN, folderQuran.category)

        val musicTrack = AudioClassifier.classify(
            title = "ليالي العود",
            artist = "فرقة الشرق",
            genre = "Arabic Oud"
        )
        assertFalse(musicTrack.isQuran)
        assertEquals(TrackCategory.ARABIC_TARAB, musicTrack.category)
    }

    @Test
    @Config(qualifiers = "ar-rSA-xxxhdpi")
    fun `high density phone startup stays within bitmap budget and survives repeated AppCheck and service commands`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Verify 2D vector launcher icon drawables and nodpi hero bitmaps on 640dpi (xxxhdpi) phones
        val launcherFg = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
        val launcherBg = ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)
        val heroBmp = BitmapFactory.decodeResource(context.resources, R.drawable.img_music_hero)
        val quranBmp = BitmapFactory.decodeResource(context.resources, R.drawable.img_quran_sanctuary)

        assertNotNull(launcherFg)
        assertNotNull(launcherBg)
        assertNotNull(heroBmp)
        assertNotNull(quranBmp)
        assertTrue("Hero bitmap width must stay <= 1024px on xxxhdpi", heroBmp.width <= 1024)
        assertTrue("Quran hero bitmap width must stay <= 1024px on xxxhdpi", quranBmp.width <= 1024)
        assertTrue("Hero bitmap allocation must stay < 4 MB", heroBmp.allocationByteCount < 4_000_000)

        // Simulate Activity creation + configuration recreation calling configureAppCheck repeatedly
        configureAppCheck(context, Intent())
        configureAppCheck(context, Intent())
        DeviceAudioScanner.deleteLegacyStarterAudioFiles(context)
        RecentPlaybackStore.ensureLoaded(context)
        TrackSortStore.ensureLoaded(context)

        // Verify NaghamMediaService fulfills startForeground contract even when currentTrack is null or stopping
        val refreshController = Robolectric.buildService(
            NaghamMediaService::class.java,
            Intent(context, NaghamMediaService::class.java)
        )
        refreshController.create().startCommand(0, 1)

        val stopController = Robolectric.buildService(
            NaghamMediaService::class.java,
            Intent(context, NaghamMediaService::class.java).apply { action = NaghamMediaService.ACTION_STOP }
        )
        stopController.create().startCommand(0, 2)

        // Render full root startup flow and zero-dimension Canvas edge cases on 640dpi phone
        val sampleTrack = CloudTrack(id = "t1", title = "Test", quranAudio = false)
        composeTestRule.setContent {
            Box {
                NaghamRootApp(
                    incomingIntent = null,
                    onIntentConsumed = {}
                )
                Box(modifier = Modifier.size(0.dp)) {
                    LiquidSoundwaveBackdrop(
                        isPlaying = true,
                        accentColor = Color.Cyan,
                        secondaryColor = Color.Magenta
                    )
                    PlayerArtworkWithEffects(
                        track = sampleTrack,
                        playbackState = PlaybackState(currentTrack = sampleTrack, isPlaying = true),
                        visualEffect = PlayerVisualEffect.LIQUID_WAVE
                    )
                    HorizontalSpectrumBarStrip(
                        isPlaying = true,
                        eqBandsMb = listOf(0L, 200L, 400L, 100L, 0L),
                        accentColor = Color.Cyan,
                        modifier = Modifier.size(0.dp)
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }

    @Test
    fun `multi-file selection in Library supports batch add to playlist and batch delete`() {
        val m1 = CloudTrack(id = "m1", title = "Song One", artist = "Artist A", quranAudio = false)
        val m2 = CloudTrack(id = "m2", title = "Song Two", artist = "Artist B", quranAudio = false)
        val m3 = CloudTrack(id = "m3", title = "Song Three", artist = "Artist C", quranAudio = false)
        val existingPlaylist = CloudPlaylist(id = "pl_fav", name = "Favorites Mix", trackIds = emptyList())

        var batchAddedTrackIds: List<String> = emptyList()
        var batchTargetPlaylistId: String? = null
        var batchDeletedIds: List<String> = emptyList()

        val context = ApplicationProvider.getApplicationContext<Context>()
        FolderFilterStore.resetDefaults(context)

        composeTestRule.setContent {
            NaghamTheme(darkTheme = true, eyeComfortMode = false, languageCode = "ar") {
                LibraryScreen(
                    allTracks = listOf(m1, m2, m3),
                    playlists = listOf(existingPlaylist),
                    currentTrackId = null,
                    isPlaying = false,
                    isScanningStorage = false,
                    onPlayMusicTrack = { _, _ -> },
                    onToggleFavorite = {},
                    onEditTrack = {},
                    onBatchDeleteTracks = { selected -> batchDeletedIds = selected.map { it.id } },
                    onBatchAddTracksToPlaylist = { selected, pl ->
                        batchAddedTrackIds = selected.map { it.id }
                        batchTargetPlaylistId = pl.id
                    },
                    onJumpToQuranSanctuary = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Enter multi-selection mode via single-row Select dropdown button (sub-options hidden until clicked)
        composeTestRule.onNodeWithTag("library_tracks_list")
            .performScrollToNode(hasTestTag("library_sort_bar"))
        assertEquals(0, composeTestRule.onAllNodesWithTag("library_select_mode_button").fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag("library_select_menu_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("library_select_mode_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("library_selection_bar").assertIsDisplayed()

        // Select m1 via card click and m2 via checkbox click
        composeTestRule.onNodeWithTag("library_tracks_list")
            .performScrollToNode(hasTestTag("track_item_card_m1"))
        composeTestRule.onNodeWithTag("track_item_card_m1").performClick()

        composeTestRule.onNodeWithTag("library_tracks_list")
            .performScrollToNode(hasTestTag("track_item_card_m2"))
        composeTestRule.onNodeWithTag("select_checkbox_m2").performClick()
        composeTestRule.waitForIdle()

        // Batch add selected tracks to existing playlist
        composeTestRule.onNodeWithTag("library_batch_add_playlist_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("batch_add_to_playlist_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("batch_playlist_item_pl_fav").performClick()
        composeTestRule.waitForIdle()

        assertEquals("pl_fav", batchTargetPlaylistId)
        assertEquals(setOf("m1", "m2"), batchAddedTrackIds.toSet())

        // Re-enter selection mode via Select dropdown, Select All, and Batch Delete
        composeTestRule.onNodeWithTag("library_tracks_list")
            .performScrollToNode(hasTestTag("library_sort_bar"))
        composeTestRule.onNodeWithTag("library_select_menu_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("library_select_mode_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("library_select_all_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("library_batch_delete_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("batch_delete_confirm_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_batch_delete_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(setOf("m1", "m2", "m3"), batchDeletedIds.toSet())
    }

    @Test
    fun `quran sanctuary and long-press sheet support multi-select seed and batch create playlist`() {
        val q1 = CloudTrack(id = "q1", title = "سورة الفاتحة", artist = "القارئ", quranAudio = true)
        val q2 = CloudTrack(id = "q2", title = "سورة البقرة", artist = "القارئ", quranAudio = true)

        var seedTrackId by mutableStateOf<String?>(null)
        var createdPlaylistName: String? = null
        var createdPlaylistTrackIds: List<String> = emptyList()

        composeTestRule.setContent {
            NaghamTheme(darkTheme = true, eyeComfortMode = false, languageCode = "ar") {
                if (seedTrackId == null) {
                    TrackLongPressSheetContent(
                        track = q1,
                        playlists = emptyList(),
                        onDismiss = {},
                        onAddTrackToPlaylist = { _, _ -> },
                        onCreatePlaylistWithTrack = { _, _ -> },
                        onMoveTrackIsolation = { _, _ -> },
                        onEditTrack = {},
                        onDeleteTrack = {},
                        onSelectMultipleFromHere = { trk -> seedTrackId = trk.id }
                    )
                } else {
                    QuranSanctuaryScreen(
                        allTracks = listOf(q1, q2),
                        playlists = emptyList(),
                        currentTrackId = null,
                        isPlaying = false,
                        isScanningStorage = false,
                        initialSelectedTrackId = seedTrackId,
                        onInitialSelectionConsumed = {},
                        onPlayQuranQueue = { _, _ -> },
                        onEditTrack = {},
                        onBatchCreatePlaylistWithTracks = { name, selected ->
                            createdPlaylistName = name
                            createdPlaylistTrackIds = selected.map { it.id }
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("long_press_select_multiple").performClick()
        composeTestRule.waitForIdle()

        // QuranSanctuaryScreen opens directly in selection mode with q1 already selected
        composeTestRule.onNodeWithTag("quran_selection_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_tracks_list")
            .performScrollToNode(hasTestTag("quran_item_card_q2"))
        composeTestRule.onNodeWithTag("quran_item_card_q2").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("quran_batch_add_playlist_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("batch_create_playlist_confirm_button").performClick()
        composeTestRule.waitForIdle()

        assertNotNull(createdPlaylistName)
        assertEquals(setOf("q1", "q2"), createdPlaylistTrackIds.toSet())
    }

    @Test
    fun `settings screen switches between all 5 UI design themes Glass Material Cyber Neon Royal Arabesque and Soft Neumorphic`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var settings by mutableStateOf(
            UserSettings(userId = "u1", darkTheme = true, eyeComfortMode = false, languageCode = "ar")
        )
        var designStyle by mutableStateOf(AppDesignStyle.MATERIAL)
        var activeStyleFromTokens: AppDesignStyle = AppDesignStyle.MATERIAL
        var activeCardAlpha = 1f
        var activeIsFloatingNav = false
        var activeCardShapeIsCutCorner = false
        var activeShadowElevationDp = 0f

        composeTestRule.setContent {
            NaghamTheme(
                darkTheme = settings.darkTheme,
                eyeComfortMode = settings.eyeComfortMode,
                designStyle = designStyle,
                languageCode = settings.languageCode
            ) {
                val tokens = LocalNaghamDesign.current
                activeStyleFromTokens = tokens.style
                activeCardAlpha = tokens.cardAlpha
                activeIsFloatingNav = tokens.isFloatingNavBar
                activeCardShapeIsCutCorner = tokens.cardShape is CutCornerShape
                activeShadowElevationDp = tokens.cardShadowElevation.value

                Box {
                    NaghamThemedBackdrop {
                        SettingsScreen(
                            settings = settings,
                            selectedPalette = AppColorPalette.AMBER_GOLD,
                            selectedDesignStyle = designStyle,
                            isScanningStorage = false,
                            onDarkThemeChange = { settings = settings.copy(darkTheme = it) },
                            onToggleEyeComfort = {},
                            onSelectPalette = {},
                            onSelectDesignStyle = { chosen ->
                                designStyle = chosen
                                ThemePreferences.saveDesignStyle(context, chosen)
                            },
                            onLanguageChange = {},
                            onToggleAutoIsolateQuran = {},
                            onResetTabOrder = {},
                            onSignOut = {}
                        )
                    }
                    // Verify zero-dimension Canvas safety across every design style
                    Box(modifier = Modifier.size(0.dp)) {
                        NaghamThemedBackdrop()
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Switch to Frosted Glass (زجاجي شفاف)
        composeTestRule.onNodeWithTag("settings_screen_list")
            .performScrollToNode(hasTestTag("design_style_option_GLASS"))
        composeTestRule.onNodeWithTag("design_style_option_GLASS")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppDesignStyle.GLASS, activeStyleFromTokens)
        assertEquals(AppDesignStyle.GLASS, ThemePreferences.getDesignStyle(context))
        assertTrue("Glass theme must use translucent frosted card alpha", activeCardAlpha < 0.65f)
        assertTrue("Glass theme must float the bottom navigation bar", activeIsFloatingNav)

        // 2. Switch to Cyber Neon (نيون مستقبلي)
        composeTestRule.onNodeWithTag("settings_screen_list")
            .performScrollToNode(hasTestTag("design_style_option_NEON_CYBER"))
        composeTestRule.onNodeWithTag("design_style_option_NEON_CYBER")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppDesignStyle.NEON_CYBER, activeStyleFromTokens)
        assertTrue("Cyber Neon theme must use chamfered CutCornerShape cards", activeCardShapeIsCutCorner)

        // 3. Switch to Royal Arabesque (فخامة شرقية)
        composeTestRule.onNodeWithTag("settings_screen_list")
            .performScrollToNode(hasTestTag("design_style_option_ROYAL_ARABESQUE"))
        composeTestRule.onNodeWithTag("design_style_option_ROYAL_ARABESQUE")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppDesignStyle.ROYAL_ARABESQUE, activeStyleFromTokens)
        assertFalse(activeCardShapeIsCutCorner)

        // 4. Switch to Soft Neumorphic (نيومورفيك بارز)
        composeTestRule.onNodeWithTag("settings_screen_list")
            .performScrollToNode(hasTestTag("design_style_option_NEUMORPHIC_STUDIO"))
        composeTestRule.onNodeWithTag("design_style_option_NEUMORPHIC_STUDIO")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppDesignStyle.NEUMORPHIC_STUDIO, activeStyleFromTokens)
        assertTrue("Soft Neumorphic theme must apply 3D card shadow elevation", activeShadowElevationDp >= 6f)

        // 5. Switch back to Material 3 (متريال عصري)
        composeTestRule.onNodeWithTag("settings_screen_list")
            .performScrollToNode(hasTestTag("design_style_option_MATERIAL"))
        composeTestRule.onNodeWithTag("design_style_option_MATERIAL")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppDesignStyle.MATERIAL, activeStyleFromTokens)
        assertEquals(AppDesignStyle.MATERIAL, ThemePreferences.getDesignStyle(context))
    }

    @Test
    fun `unwanted folders like WhatsApp and Recordings are hidden automatically and custom folders can be hidden and restored`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FolderFilterStore.resetDefaults(context)

        val whatsappAudio = CloudTrack(
            id = "wa_1",
            title = "AUD-20260501-WA0004",
            artist = "Unknown",
            album = "WhatsApp Audio",
            quranAudio = false,
            sourceUri = "file:///storage/emulated/0/WhatsApp/Media/WhatsApp Audio/AUD-20260501-WA0004.opus"
        )
        val callRec = CloudTrack(
            id = "rec_1",
            title = "Call_0551234",
            artist = "Recorder",
            album = "CallRecordings",
            quranAudio = false,
            sourceUri = "file:///storage/emulated/0/Recordings/CallRecordings/call1.m4a"
        )
        val musicMain = CloudTrack(
            id = "mus_1",
            title = "Odyssey",
            artist = "Roary",
            album = "MyMusic",
            quranAudio = false,
            sourceUri = "file:///storage/emulated/0/MyMusic/odyssey.mp3"
        )
        val downloadsSong = CloudTrack(
            id = "dl_1",
            title = "Starfall",
            artist = "Nour",
            album = "MyDownloads",
            quranAudio = false,
            sourceUri = "file:///storage/emulated/0/MyDownloads/starfall.mp3"
        )

        assertTrue(FolderFilterStore.isBuiltInUnwantedFolder("WhatsApp Audio", whatsappAudio.sourceUri))
        assertTrue(FolderFilterStore.isBuiltInUnwantedFolder("CallRecordings", callRec.sourceUri))
        assertFalse(FolderFilterStore.isBuiltInUnwantedFolder("MyMusic", musicMain.sourceUri))

        composeTestRule.setContent {
            NaghamTheme(
                darkTheme = true,
                eyeComfortMode = false,
                colorPalette = AppColorPalette.AMETHYST_VELVET,
                designStyle = AppDesignStyle.GLASS,
                languageCode = "ar"
            ) {
                LibraryScreen(
                    allTracks = listOf(whatsappAudio, callRec, musicMain, downloadsSong),
                    currentTrackId = null,
                    isPlaying = false,
                    isScanningStorage = false,
                    onPlayMusicTrack = { _, _ -> },
                    onToggleFavorite = {},
                    onEditTrack = {},
                    onJumpToQuranSanctuary = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // WhatsApp Audio and CallRecordings are automatically excluded!
        assertEquals(0, composeTestRule.onAllNodesWithTag("track_item_card_wa_1").fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithTag("track_item_card_rec_1").fetchSemanticsNodes().size)

        // Normal folders (MyMusic & MyDownloads) are visible
        composeTestRule.onNodeWithTag("library_tracks_list")
            .performScrollToNode(hasTestTag("track_item_card_dl_1"))
        composeTestRule.onNodeWithTag("track_item_card_dl_1").assertIsDisplayed()

        // Folder sub-options are hidden until Folders button is clicked on the single-row toolbar
        composeTestRule.onNodeWithTag("library_tracks_list")
            .performScrollToNode(hasTestTag("library_sort_bar"))
        assertEquals(0, composeTestRule.onAllNodesWithTag("library_folder_ALL").fetchSemanticsNodes().size)

        // Open Folders vertical dropdown and hide 'MyDownloads' folder in one tap
        composeTestRule.onNodeWithTag("library_folders_menu_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("library_hide_folder_MyDownloads").performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, composeTestRule.onAllNodesWithTag("track_item_card_dl_1").fetchSemanticsNodes().size)

        // Restore 'MyDownloads' via ManageExcludedFoldersDialog
        composeTestRule.onNodeWithTag("library_folders_menu_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("library_manage_excluded_folders_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("manage_excluded_folders_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("excluded_folders_dialog_list")
            .performScrollToNode(hasTestTag("dialog_toggle_folder_MyDownloads"))
        composeTestRule.onNodeWithTag("dialog_toggle_folder_MyDownloads")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("close_excluded_folders_dialog_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("library_tracks_list")
            .performScrollToNode(hasTestTag("track_item_card_dl_1"))
        composeTestRule.onNodeWithTag("track_item_card_dl_1").assertIsDisplayed()

        FolderFilterStore.resetDefaults(context)
    }

    @Test
    fun `redesigned music and quran player overlays support up next queue selection reciters strip and zero size canvas safety`() {
        val m1 = CloudTrack(id = "m1", title = "Skin", artist = "Flume", durationMs = 203_000L, quranAudio = false)
        val m2 = CloudTrack(id = "m2", title = "Hold On", artist = "Netsky", durationMs = 230_000L, quranAudio = false)
        val q1 = CloudTrack(id = "q1", title = "سورة الرحمن", artist = "مشاري العفاسي", durationMs = 420_000L, quranAudio = true)
        val q2 = CloudTrack(id = "q2", title = "سورة الملك", artist = "سعد الغامدي", durationMs = 510_000L, quranAudio = true)

        var activePlayback by mutableStateOf(
            PlaybackState(
                currentTrack = m1,
                queue = listOf(m1, m2),
                isPlaying = true,
                currentPositionMs = 30_000L,
                durationMs = 203_000L
            )
        )
        var selectedQueueTrackId: String? = null
        var favoritedTrackId: String? = null
        var zeroSizeMode by mutableStateOf(false)

        composeTestRule.setContent {
            NaghamTheme(
                darkTheme = true,
                eyeComfortMode = false,
                colorPalette = AppColorPalette.AMETHYST_VELVET,
                designStyle = AppDesignStyle.GLASS,
                languageCode = "ar"
            ) {
                Box(modifier = if (zeroSizeMode) Modifier.size(0.dp) else Modifier) {
                    FullNowPlayingOverlay(
                        playbackState = activePlayback,
                        onCollapse = {},
                        onTogglePlayPause = {},
                        onSkipNext = {},
                        onSkipPrevious = {},
                        onSeekTo = {},
                        onToggleShuffle = {},
                        onCycleRepeat = {},
                        onEditTrack = {},
                        onToggleFavorite = { favoritedTrackId = it.id },
                        onSelectQueueTrack = { selectedQueueTrackId = it.id },
                        onSelectPreset = {},
                        onBandChanged = { _, _ -> },
                        onBassChanged = {},
                        onSpeedChanged = {},
                        onSyncEqToCloud = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("player_fav_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals("m1", favoritedTrackId)

        // Tap Up-Next queue item m2 inside Music Player
        composeTestRule.onNodeWithTag("player_queue_item_m2")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals("m2", selectedQueueTrackId)

        // Switch to Holy Quran Sanctuary Mode and verify circular medallion badge + Reciters strip
        activePlayback = PlaybackState(
            currentTrack = q1,
            queue = listOf(q1, q2),
            isPlaying = true,
            isQuranMode = true,
            currentPositionMs = 90_000L,
            durationMs = 420_000L
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("quran_medallion_play_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_reciters_row")
            .performScrollTo()
            .performScrollToNode(hasTestTag("quran_reciter_card_q2"))
        composeTestRule.onNodeWithTag("quran_reciter_card_q2").performClick()
        composeTestRule.waitForIdle()
        assertEquals("q2", selectedQueueTrackId)

        // Verify zero-dimension Canvas safety in both Quran and Music modes
        zeroSizeMode = true
        composeTestRule.waitForIdle()
        activePlayback = PlaybackState(
            currentTrack = m1,
            queue = listOf(m1, m2),
            isPlaying = true,
            currentPositionMs = 30_000L,
            durationMs = 203_000L
        )
        composeTestRule.waitForIdle()
    }

    @Test
    fun `eye comfort night filter is removed and new UI themes and color palettes apply cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(8, AppDesignStyle.entries.size)
        assertEquals(10, AppColorPalette.entries.size)

        var designStyle by mutableStateOf(AppDesignStyle.OBSIDIAN_AMOLED)
        var palette by mutableStateOf(AppColorPalette.TURQUOISE_LAGOON)
        var observedBg: Color = Color.Unspecified
        var observedPrimary: Color = Color.Unspecified

        composeTestRule.setContent {
            // Even if legacy eyeComfortMode=true is passed from stored settings, it must NOT tint colors
            NaghamTheme(
                darkTheme = true,
                eyeComfortMode = true,
                colorPalette = palette,
                designStyle = designStyle,
                languageCode = "ar"
            ) {
                observedBg = MaterialTheme.colorScheme.background
                observedPrimary = MaterialTheme.colorScheme.primary
                SettingsScreen(
                    settings = UserSettings(userId = "u1", darkTheme = true, eyeComfortMode = true, languageCode = "ar"),
                    selectedPalette = palette,
                    selectedDesignStyle = designStyle,
                    isScanningStorage = false,
                    onDarkThemeChange = {},
                    onSelectPalette = { palette = it },
                    onSelectDesignStyle = { chosen ->
                        designStyle = chosen
                        ThemePreferences.saveDesignStyle(context, chosen)
                    },
                    onLanguageChange = {},
                    onToggleAutoIsolateQuran = {},
                    onResetTabOrder = {},
                    onSignOut = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        // Confirm night eye-comfort toggle UI is completely removed
        assertEquals(0, composeTestRule.onAllNodesWithTag("eye_comfort_switch").fetchSemanticsNodes().size)
        // Obsidian AMOLED uses pure pitch-black background without warm eye-comfort tint
        assertEquals(Color(0xFF000000), observedBg)
        assertEquals(buildPaletteDarkScheme(AppColorPalette.TURQUOISE_LAGOON).primary, observedPrimary)

        // Select Velvet Sunset & Crystal Minimal themes + new Sunset Coral & Lime Pulse palettes
        composeTestRule.onNodeWithTag("settings_screen_list")
            .performScrollToNode(hasTestTag("design_style_option_VELVET_SUNSET"))
        composeTestRule.onNodeWithTag("design_style_option_VELVET_SUNSET")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppDesignStyle.VELVET_SUNSET, designStyle)

        composeTestRule.onNodeWithTag("settings_screen_list")
            .performScrollToNode(hasTestTag("design_style_option_CRYSTAL_MINIMAL"))
        composeTestRule.onNodeWithTag("design_style_option_CRYSTAL_MINIMAL")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppDesignStyle.CRYSTAL_MINIMAL, designStyle)

        composeTestRule.onNodeWithTag("settings_screen_list")
            .performScrollToNode(hasTestTag("palette_option_LIME_PULSE"))
        composeTestRule.onNodeWithTag("palette_option_LIME_PULSE")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppColorPalette.LIME_PULSE, palette)
    }

    @Test
    fun `player theme miniature preview cards in Settings switch FullNowPlayingOverlay layout automatically`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(6, PlayerThemeStyle.entries.size)

        val sampleTrack = CloudTrack(
            id = "m_theme_1",
            title = "Aurora Horizon",
            artist = "Nagham Studio",
            album = "Sessions",
            durationMs = 200_000L,
            quranAudio = false
        )
        var selectedPlayerTheme by mutableStateOf(PlayerThemeStyle.AURORA_RING)
        var showOverlay by mutableStateOf(false)

        composeTestRule.setContent {
            NaghamTheme(darkTheme = true, languageCode = "ar") {
                if (!showOverlay) {
                    SettingsScreen(
                        settings = UserSettings(userId = "u1", darkTheme = true, languageCode = "ar"),
                        selectedPalette = AppColorPalette.AMETHYST_VELVET,
                        selectedPlayerThemeStyle = selectedPlayerTheme,
                        isScanningStorage = false,
                        onDarkThemeChange = {},
                        onSelectPalette = {},
                        onSelectPlayerThemeStyle = { style ->
                            selectedPlayerTheme = style
                        },
                        onLanguageChange = {},
                        onToggleAutoIsolateQuran = {},
                        onResetTabOrder = {},
                        onSignOut = {}
                    )
                } else {
                    FullNowPlayingOverlay(
                        playbackState = PlaybackState(
                            currentTrack = sampleTrack,
                            queue = listOf(sampleTrack),
                            isPlaying = true,
                            currentPositionMs = 60_000L,
                            durationMs = 200_000L
                        ),
                        playerThemeStyle = selectedPlayerTheme,
                        onCollapse = { showOverlay = false },
                        onTogglePlayPause = {},
                        onSkipNext = {},
                        onSkipPrevious = {},
                        onSeekTo = {},
                        onToggleShuffle = {},
                        onCycleRepeat = {},
                        onEditTrack = {},
                        onSelectPreset = {},
                        onBandChanged = { _, _ -> },
                        onBassChanged = {},
                        onSpeedChanged = {},
                        onSyncEqToCloud = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen_list")
            .performScrollToNode(hasTestTag("settings_player_themes_card"))

        // Select VINYL_TURNTABLE miniature card in Settings
        composeTestRule.onNodeWithTag("player_theme_card_VINYL_TURNTABLE")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(PlayerThemeStyle.VINYL_TURNTABLE, selectedPlayerTheme)
        assertEquals(PlayerThemeStyle.VINYL_TURNTABLE, ThemePreferences.getPlayerThemeStyle(context))

        // Open FullNowPlayingOverlay and verify VINYL_TURNTABLE stage renders automatically
        showOverlay = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("player_theme_stage_VINYL_TURNTABLE").assertIsDisplayed()

        // Cycle through every remaining PlayerThemeStyle (including RADIAL_SPECTRUM) to verify rendering
        PlayerThemeStyle.entries.forEach { style ->
            selectedPlayerTheme = style
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("player_theme_stage_${style.id}").assertIsDisplayed()
        }
    }

    @Test
    fun `player swipe glides track to full screen edge in swiped direction and RTL progress thumb stays pixel-locked with played boundary`() {
        val m1 = CloudTrack(id = "m1", title = "ألف ليلة", artist = "طرب", durationMs = 200_000L, quranAudio = false)
        val m2 = CloudTrack(id = "m2", title = "نسيم الليل", artist = "طرب", durationMs = 200_000L, quranAudio = false)
        val q1 = CloudTrack(id = "q1", title = "سورة الرحمن", artist = "العفاسي", durationMs = 200_000L, quranAudio = true)

        var playbackState by mutableStateOf(
            PlaybackState(
                currentTrack = m1,
                queue = listOf(m1, m2),
                isPlaying = true,
                currentPositionMs = 50_000L, // 25%
                durationMs = 200_000L
            )
        )
        var peakSwipeTx = 0f
        var measuredStageW = 0f
        var observedThumbX = -1f
        var observedBoundaryX = -2f
        var seekedPositionMs = -1L

        composeTestRule.setContent {
            // Arabic RTL layout: verify progress indicator NEVER moves opposite to played boundary!
            NaghamTheme(darkTheme = true, languageCode = "ar") {
                FullNowPlayingOverlay(
                    playbackState = playbackState,
                    onPeakSwipeTranslationObserved = { tx, w ->
                        peakSwipeTx = tx
                        measuredStageW = w
                    },
                    onProgressSyncObserved = { thumbX, boundaryX, _ ->
                        observedThumbX = thumbX
                        observedBoundaryX = boundaryX
                    },
                    onCollapse = {},
                    onTogglePlayPause = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSwipePreviousTrack = {},
                    onSeekTo = { target ->
                        seekedPositionMs = target
                        playbackState = playbackState.copy(currentPositionMs = target)
                    },
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onEditTrack = {},
                    onSelectPreset = {},
                    onBandChanged = { _, _ -> },
                    onBassChanged = {},
                    onSpeedChanged = {},
                    onSyncEqToCloud = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        // 1. Verify 0.0px lock between thumb and played boundary at 25% in Arabic RTL
        assertTrue("Thumb X must be positive", observedThumbX > 0f)
        assertEquals("Progress thumb and played boundary must share exact X", observedBoundaryX, observedThumbX, 0.01f)
        val xAt25Percent = observedThumbX

        // Advance playback to 75% -> thumb & boundary must BOTH move right (monotonically increasing X)
        playbackState = playbackState.copy(currentPositionMs = 150_000L)
        composeTestRule.waitForIdle()
        val xAt75Percent = observedThumbX
        assertEquals(observedBoundaryX, observedThumbX, 0.01f)
        assertTrue(
            "Progress thumb and played boundary must advance Left-to-Right together in Arabic RTL",
            xAt75Percent > xAt25Percent
        )

        // Scrub seekbar from left toward right and verify seek target increases toward end of track
        composeTestRule.onNodeWithTag("player_seek_slider")
            .performTouchInput {
                down(Offset(width * 0.20f, centerY))
                moveTo(Offset(width * 0.85f, centerY))
                up()
            }
        composeTestRule.waitForIdle()
        assertTrue("Dragging seekbar rightward in RTL must seek toward >75% of track", seekedPositionMs > 150_000L)
        assertEquals(observedBoundaryX, observedThumbX, 0.01f)

        // 2. Verify full-screen-edge swipe exit in BOTH directions
        composeTestRule.onNodeWithTag("player_artwork_swipe_area")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertTrue(
            "Swiping left must glide track all the way past the left screen edge (tx=$peakSwipeTx, w=$measuredStageW)",
            peakSwipeTx <= -measuredStageW && measuredStageW > 0f
        )

        composeTestRule.onNodeWithTag("player_artwork_swipe_area")
            .performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertTrue(
            "Swiping right must glide track all the way past the right screen edge (tx=$peakSwipeTx, w=$measuredStageW)",
            peakSwipeTx >= measuredStageW && measuredStageW > 0f
        )

        // 3. Switch to Quran Sanctuary player in Arabic RTL and verify waveform thumb & boundary also match 0.0px
        playbackState = PlaybackState(
            currentTrack = q1,
            queue = listOf(q1),
            isQuranMode = true,
            isPlaying = true,
            currentPositionMs = 40_000L,
            durationMs = 200_000L
        )
        composeTestRule.waitForIdle()
        val quranX20 = observedThumbX
        assertEquals(observedBoundaryX, observedThumbX, 0.01f)

        playbackState = playbackState.copy(currentPositionMs = 160_000L)
        composeTestRule.waitForIdle()
        val quranX80 = observedThumbX
        assertEquals(observedBoundaryX, observedThumbX, 0.01f)
        assertTrue("Quran waveform thumb & played bars must advance together Left-to-Right", quranX80 > quranX20)
    }

    @Test
    fun `headphones and Bluetooth disconnect setting toggles between pausing and continuing playback`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = AudioPlayerController.getInstance(context)
        val sampleTrack = CloudTrack(id = "bt_1", title = "Night Drive", artist = "Nour", quranAudio = false)

        composeTestRule.setContent {
            NaghamTheme(darkTheme = true, languageCode = "ar") {
                SettingsScreen(
                    settings = UserSettings(userId = "u1", darkTheme = true, languageCode = "ar"),
                    selectedPalette = AppColorPalette.AMBER_GOLD,
                    isScanningStorage = false,
                    onDarkThemeChange = {},
                    onSelectPalette = {},
                    onLanguageChange = {},
                    onToggleAutoIsolateQuran = {},
                    onResetTabOrder = {},
                    onSignOut = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen_list")
            .performScrollToNode(hasTestTag("headset_disconnect_continue_option"))

        // 1. Select "Continue Playing" when headphones/Bluetooth disconnect
        composeTestRule.onNodeWithTag("headset_disconnect_continue_option")
            .performClick()
        composeTestRule.waitForIdle()
        assertFalse(ThemePreferences.getPauseOnHeadsetDisconnect(context))

        controller.setSimulatedPlayingStateForTest(sampleTrack, isPlaying = true)
        val pausedWhenContinueSelected = controller.handleAudioOutputDisconnected()
        assertFalse("Playback must NOT pause on disconnect when Continue option is selected", pausedWhenContinueSelected)
        assertTrue("Track must remain playing when Continue option is selected", controller.state.value.isPlaying)

        // 2. Select "Pause Playback" when headphones/Bluetooth disconnect
        composeTestRule.onNodeWithTag("settings_screen_list")
            .performScrollToNode(hasTestTag("headset_disconnect_pause_option"))
        composeTestRule.onNodeWithTag("headset_disconnect_pause_option")
            .performClick()
        composeTestRule.waitForIdle()
        assertTrue(ThemePreferences.getPauseOnHeadsetDisconnect(context))

        controller.setSimulatedPlayingStateForTest(sampleTrack, isPlaying = true)
        val pausedWhenPauseSelected = controller.handleAudioOutputDisconnected()
        assertTrue("Playback MUST pause on disconnect when Pause option is selected", pausedWhenPauseSelected)
        assertFalse("Track must transition to paused state on disconnect", controller.state.value.isPlaying)

        controller.stopAndClear()
    }

    @Test
    fun `equalizer screen controls multi-band frequency sliders presets vertical rack and master switch`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ThemePreferences.saveEqEnabled(context, true)
        ThemePreferences.saveEqVerticalMode(context, false)

        val controller = AudioPlayerController.getInstance(context)
        controller.setEqualizerEnabled(true)
        controller.updateEqualizer(EqualizerPreset.FLAT.id, EqualizerPreset.FLAT.bandsMb, 0L)
        controller.setPlaybackSpeed(1.0f)

        var closedCount = 0
        var syncedCount = 0

        composeTestRule.setContent {
            val state by controller.state.collectAsState()
            NaghamTheme(darkTheme = true, languageCode = "ar") {
                Box {
                    // Zero-size Canvas guard verification alongside live EqualizerScreen
                    EqualizerResponseCurveCanvas(
                        bandsMb = state.eqBandsMb,
                        enabled = state.eqEnabled,
                        accentColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(0.dp)
                    )
                    EqualizerScreen(
                        playbackState = state,
                        onClose = { closedCount++ },
                        onToggleEnabled = { controller.setEqualizerEnabled(it) },
                        onResetFlat = {
                            controller.updateEqualizer(
                                EqualizerPreset.FLAT.id,
                                EqualizerPreset.FLAT.bandsMb,
                                0L
                            )
                        },
                        onSelectPreset = { preset ->
                            val bands = if (preset == EqualizerPreset.CUSTOM) state.eqBandsMb else preset.bandsMb
                            controller.updateEqualizer(preset.id, bands, state.bassBoostStrength)
                        },
                        onBandChanged = { index, gainMb ->
                            val updated = state.eqBandsMb.toMutableList()
                            while (updated.size < 5) updated.add(0L)
                            updated[index] = gainMb.coerceIn(-1500L, 1500L)
                            controller.updateEqualizer(EqualizerPreset.CUSTOM.id, updated, state.bassBoostStrength)
                        },
                        onBassChanged = { strength ->
                            controller.updateEqualizer(
                                state.eqPresetId,
                                state.eqBandsMb,
                                strength.coerceIn(0L, 1000L)
                            )
                        },
                        onSpeedChanged = { controller.setPlaybackSpeed(it) },
                        onSyncToCloud = { syncedCount++ }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("equalizer_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("eq_response_curve_canvas").assertIsDisplayed()

        // 1. Verify all 5 frequency sliders (60Hz .. 14kHz) exist and adjust gains independently
        composeTestRule.onNodeWithTag("equalizer_scroll_list")
            .performScrollToNode(hasTestTag("eq_frequency_bands_card"))
        composeTestRule.waitForIdle()

        for (i in 0 until 5) {
            assertTrue(
                "Frequency band slider $i must be present",
                composeTestRule.onAllNodesWithTag("eq_band_slider_$i").fetchSemanticsNodes().isNotEmpty()
            )
        }

        composeTestRule.onNodeWithTag("eq_band_slider_0")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(650f) }
        composeTestRule.waitForIdle()
        assertEquals(650L, controller.state.value.eqBandsMb[0])
        assertEquals(EqualizerPreset.CUSTOM.id, controller.state.value.eqPresetId)

        composeTestRule.onNodeWithTag("eq_band_slider_3")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(-450f) }
        composeTestRule.waitForIdle()
        assertEquals(-450L, controller.state.value.eqBandsMb[3])

        // Tap-to-zero dB badge resets band 0 while preserving band 3
        composeTestRule.onNodeWithTag("eq_band_reset_0").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0L, controller.state.value.eqBandsMb[0])
        assertEquals(-450L, controller.state.value.eqBandsMb[3])

        // 2. Switch to Vertical Studio Faders rack and adjust high-frequency band 4 (14kHz)
        composeTestRule.onNodeWithTag("eq_mode_vertical_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(ThemePreferences.getEqVerticalMode(context))
        composeTestRule.onNodeWithTag("eq_vertical_faders_rack").assertIsDisplayed()

        composeTestRule.onNodeWithTag("eq_band_slider_4")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(900f) }
        composeTestRule.waitForIdle()
        assertEquals(900L, controller.state.value.eqBandsMb[4])

        // 3. Select a preset (BASS_BOOST) and verify preset curve applies
        composeTestRule.onNodeWithTag("equalizer_scroll_list")
            .performScrollToNode(hasTestTag("eq_preset_BASS_BOOST"))
        composeTestRule.onNodeWithTag("eq_preset_BASS_BOOST").performClick()
        composeTestRule.waitForIdle()
        assertEquals(EqualizerPreset.BASS_BOOST.id, controller.state.value.eqPresetId)
        assertEquals(EqualizerPreset.BASS_BOOST.bandsMb, controller.state.value.eqBandsMb)

        // 4. Adjust Bass Boost and Playback Speed sliders
        composeTestRule.onNodeWithTag("equalizer_scroll_list")
            .performScrollToNode(hasTestTag("eq_bass_slider"))
        composeTestRule.onNodeWithTag("eq_bass_slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(720f) }
        composeTestRule.waitForIdle()
        assertEquals(720L, controller.state.value.bassBoostStrength)

        composeTestRule.onNodeWithTag("equalizer_scroll_list")
            .performScrollToNode(hasTestTag("eq_speed_slider"))
        composeTestRule.onNodeWithTag("eq_speed_slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1.25f) }
        composeTestRule.waitForIdle()
        assertEquals(1.25f, controller.state.value.playbackSpeed, 0.02f)

        // 5. Reset to flat (0 dB across all bands) and toggle master bypass switch
        composeTestRule.onNodeWithTag("eq_reset_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(EqualizerPreset.FLAT.id, controller.state.value.eqPresetId)
        assertEquals(listOf(0L, 0L, 0L, 0L, 0L), controller.state.value.eqBandsMb)
        assertEquals(0L, controller.state.value.bassBoostStrength)

        composeTestRule.onNodeWithTag("eq_master_switch").performClick()
        composeTestRule.waitForIdle()
        assertFalse(controller.state.value.eqEnabled)
        assertFalse(ThemePreferences.getEqEnabled(context))

        composeTestRule.onNodeWithTag("eq_master_switch").performClick()
        composeTestRule.waitForIdle()
        assertTrue(controller.state.value.eqEnabled)

        composeTestRule.onNodeWithTag("eq_close_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, closedCount)

        ThemePreferences.saveEqVerticalMode(context, false)
        controller.stopAndClear()
    }

    @Test
    fun `home and settings equalizer studio buttons trigger equalizer launch callback`() {
        var homeOpenedCount = 0
        var settingsOpenedCount = 0
        var showSettings by mutableStateOf(false)

        composeTestRule.setContent {
            NaghamTheme(darkTheme = true, languageCode = "ar") {
                if (!showSettings) {
                    HomeScreen(
                        allTracks = listOf(CloudTrack(id = "m1", title = "Oud Echo", artist = "Ziad")),
                        recentTimestamps = emptyMap(),
                        currentTrackId = null,
                        isPlaying = false,
                        isScanningStorage = false,
                        onPlayMusicTrack = { _, _ -> },
                        onLongPressTrack = {},
                        onJumpToQuranSanctuary = {},
                        onOpenEqualizer = { homeOpenedCount++ }
                    )
                } else {
                    SettingsScreen(
                        settings = UserSettings(userId = "u1", darkTheme = true, languageCode = "ar"),
                        selectedPalette = AppColorPalette.AMBER_GOLD,
                        isScanningStorage = false,
                        onDarkThemeChange = {},
                        onSelectPalette = {},
                        onLanguageChange = {},
                        onToggleAutoIsolateQuran = {},
                        onResetTabOrder = {},
                        onSignOut = {},
                        onOpenEqualizer = { settingsOpenedCount++ }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_hero_equalizer_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, homeOpenedCount)

        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("home_open_equalizer_button"))
        composeTestRule.onNodeWithTag("home_open_equalizer_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(2, homeOpenedCount)

        showSettings = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen_list")
            .performScrollToNode(hasTestTag("settings_open_equalizer_button"))
        composeTestRule.onNodeWithTag("settings_open_equalizer_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, settingsOpenedCount)
    }

    @Test
    fun `smooth tab swipe deck glides across tabs in RTL and LTR with live sliding pill and zero snap back`() {
        var langCode by mutableStateOf("ar")
        val observedActiveRoutes = mutableListOf<String>()
        var exposedDeckState: com.example.ui.components.SmoothTabSwipeDeckState? = null

        val sampleMusic = listOf(
            CloudTrack(id = "sw_m1", title = "Samai Bayati", artist = "Nour", quranAudio = false)
        )
        val sampleQuran = listOf(
            CloudTrack(id = "sw_q1", title = "سورة الملك", artist = "مشاري العفاسي", quranAudio = true)
        )

        val tabs = listOf(
            DraggableNavTab(
                route = ROUTE_HOME,
                label = "الرئيسية",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                testTag = "nav_tab_home"
            ),
            DraggableNavTab(
                route = ROUTE_LIBRARY,
                label = "المكتبة",
                selectedIcon = Icons.Filled.LibraryMusic,
                unselectedIcon = Icons.Outlined.LibraryMusic,
                testTag = "nav_tab_library"
            ),
            DraggableNavTab(
                route = ROUTE_PLAYLISTS,
                label = "القوائم",
                selectedIcon = Icons.AutoMirrored.Filled.QueueMusic,
                unselectedIcon = Icons.AutoMirrored.Outlined.QueueMusic,
                testTag = "nav_tab_playlists"
            ),
            DraggableNavTab(
                route = ROUTE_QURAN,
                label = "القرآن",
                selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
                unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
                testTag = "nav_tab_quran"
            ),
            DraggableNavTab(
                route = ROUTE_SETTINGS,
                label = "الإعدادات",
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "nav_tab_settings"
            )
        )
        val tabRoutes = tabs.map { it.route }

        composeTestRule.setContent {
            val scope = rememberCoroutineScope()
            NaghamTheme(darkTheme = true, languageCode = langCode) {
                val deckState = rememberSmoothTabSwipeDeckState(
                    tabs = tabRoutes,
                    initialRoute = ROUTE_HOME
                )
                exposedDeckState = deckState
                LaunchedEffect(deckState.activeRoute) {
                    if (observedActiveRoutes.lastOrNull() != deckState.activeRoute) {
                        observedActiveRoutes.add(deckState.activeRoute)
                    }
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    SmoothTabSwipeDeck(
                        state = deckState,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("main_tab_swipe_deck")
                    ) { route, isTabActive ->
                        when (route) {
                            ROUTE_HOME -> HomeScreen(
                                allTracks = sampleMusic + sampleQuran,
                                recentTimestamps = emptyMap(),
                                currentTrackId = null,
                                isPlaying = false,
                                isScanningStorage = false,
                                onPlayMusicTrack = { _, _ -> },
                                onLongPressTrack = {},
                                onJumpToQuranSanctuary = {
                                    deckState.navigateToRoute(ROUTE_QURAN, scope)
                                },
                                isActiveTab = isTabActive
                            )
                            ROUTE_LIBRARY -> LibraryScreen(
                                allTracks = sampleMusic + sampleQuran,
                                currentTrackId = null,
                                isPlaying = false,
                                isScanningStorage = false,
                                onPlayMusicTrack = { _, _ -> },
                                onToggleFavorite = {},
                                onEditTrack = {},
                                onJumpToQuranSanctuary = {
                                    deckState.navigateToRoute(ROUTE_QURAN, scope)
                                },
                                isActiveTab = isTabActive
                            )
                            ROUTE_PLAYLISTS -> PlaylistsScreen(
                                playlists = emptyList(),
                                allTracks = sampleMusic,
                                onPlayMusicQueue = { _, _ -> },
                                onCreatePlaylist = { _, _, _ -> },
                                onUpdatePlaylistTracks = { _, _ -> },
                                onDeletePlaylist = {},
                                onOpenImportExternalDialog = {},
                                onEditTrack = {},
                                onToggleFavorite = {},
                                onBack = { deckState.navigateToRoute(ROUTE_HOME, scope) },
                                isActiveTab = isTabActive
                            )
                            ROUTE_QURAN -> QuranSanctuaryScreen(
                                allTracks = sampleQuran,
                                currentTrackId = null,
                                isPlaying = false,
                                isScanningStorage = false,
                                onPlayQuranQueue = { _, _ -> },
                                onEditTrack = {},
                                onBack = { deckState.navigateToRoute(ROUTE_HOME, scope) },
                                isActiveTab = isTabActive
                            )
                            ROUTE_SETTINGS -> SettingsScreen(
                                settings = UserSettings(userId = "u1", darkTheme = true, languageCode = langCode),
                                selectedPalette = AppColorPalette.AMBER_GOLD,
                                isScanningStorage = false,
                                onDarkThemeChange = {},
                                onSelectPalette = {},
                                onLanguageChange = {},
                                onToggleAutoIsolateQuran = {},
                                onResetTabOrder = {},
                                onSignOut = {},
                                onBack = { deckState.navigateToRoute(ROUTE_HOME, scope) },
                                isActiveTab = isTabActive
                            )
                        }
                    }

                    ReorderableBottomNavigationBar(
                        tabs = tabs,
                        selectedRoute = deckState.activeRoute,
                        onSelectTab = { _, tab ->
                            deckState.navigateToRoute(tab.route, scope)
                        },
                        onMoveTab = { _, _ -> },
                        swipePositionProvider = { deckState.visualPosition }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        val deck = requireNotNull(exposedDeckState)
        assertEquals(ROUTE_HOME, deck.activeRoute)
        composeTestRule.onNodeWithTag("home_screen_list").assertIsDisplayed()

        // Verify live sliding active pill aligns with Home tab (on the right in Arabic RTL)
        val homeTabBoundsRtl = composeTestRule.onNodeWithTag("nav_tab_home").fetchSemanticsNode().boundsInRoot
        val pillBoundsInitial = composeTestRule.onNodeWithTag("nav_sliding_active_pill").fetchSemanticsNode().boundsInRoot
        assertEquals(homeTabBoundsRtl.center.x, pillBoundsInitial.center.x, 3f)

        // 1. In Arabic RTL, swipe finger Right to move smoothly from Home (0) -> Library (1)
        composeTestRule.onNodeWithTag("main_tab_swipe_deck").performTouchInput {
            swipeRight()
        }
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_LIBRARY, deck.activeRoute)
        assertEquals(1f, deck.visualPosition, 0.01f)
        val libTabBoundsRtl = composeTestRule.onNodeWithTag("nav_tab_library").fetchSemanticsNode().boundsInRoot
        val pillBoundsAtLib = composeTestRule.onNodeWithTag("nav_sliding_active_pill").fetchSemanticsNode().boundsInRoot
        assertEquals(libTabBoundsRtl.center.x, pillBoundsAtLib.center.x, 3f)

        // 2. Rapid consecutive RTL swipes: Library (1) -> Playlists (2) -> Quran (3) with zero snap-back
        composeTestRule.onNodeWithTag("main_tab_swipe_deck").performTouchInput { swipeRight() }
        composeTestRule.onNodeWithTag("main_tab_swipe_deck").performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_QURAN, deck.activeRoute)
        assertEquals(3f, deck.visualPosition, 0.01f)

        // 3. Reverse RTL swipe Left: Quran (3) -> Playlists (2)
        composeTestRule.onNodeWithTag("main_tab_swipe_deck").performTouchInput {
            swipeLeft()
        }
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_PLAYLISTS, deck.activeRoute)

        // 4. Direct distant jump via bottom bar tap: Home (0) -> Settings (4) without activating middle tabs
        composeTestRule.onNodeWithTag("nav_tab_home").performClick()
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_HOME, deck.activeRoute)
        observedActiveRoutes.clear()
        observedActiveRoutes.add(ROUTE_HOME)

        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_SETTINGS, deck.activeRoute)
        assertEquals(listOf(ROUTE_HOME, ROUTE_SETTINGS), observedActiveRoutes)
        composeTestRule.onNodeWithTag("settings_screen_list").assertIsDisplayed()
        val settingsBoundsRtl = composeTestRule.onNodeWithTag("nav_tab_settings").fetchSemanticsNode().boundsInRoot
        val pillBoundsAtSettings = composeTestRule.onNodeWithTag("nav_sliding_active_pill").fetchSemanticsNode().boundsInRoot
        assertEquals(settingsBoundsRtl.center.x, pillBoundsAtSettings.center.x, 3f)

        // 5. Switch to English LTR: Return Home (0) and verify swipeLeft moves Home (0) -> Library (1)
        langCode = "en"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_home").performClick()
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_HOME, deck.activeRoute)

        composeTestRule.onNodeWithTag("main_tab_swipe_deck").performTouchInput {
            swipeLeft()
        }
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_LIBRARY, deck.activeRoute)
        val libTabBoundsLtr = composeTestRule.onNodeWithTag("nav_tab_library").fetchSemanticsNode().boundsInRoot
        val pillBoundsLtr = composeTestRule.onNodeWithTag("nav_sliding_active_pill").fetchSemanticsNode().boundsInRoot
        assertEquals(libTabBoundsLtr.center.x, pillBoundsLtr.center.x, 3f)
    }

    @Test
    fun `navigation compose NavHost routes cleanly across Library Playlists and Player view in idle and active states`() {
        val sampleTrack = CloudTrack(
            id = "nav_song_1",
            title = "Odyssey",
            artist = "Roary",
            album = "Horizon",
            durationMs = 176_000L,
            quranAudio = false
        )
        val samplePlaylist = CloudPlaylist(
            id = "nav_pl_1",
            name = "Late Night Mix",
            description = "Ambient synth tracks",
            trackIds = listOf("nav_song_1")
        )

        var playbackState by mutableStateOf(PlaybackState())
        var exposedController: NavHostController? = null

        composeTestRule.setContent {
            NaghamTheme(darkTheme = true, eyeComfortMode = false, languageCode = "en") {
                val navController = rememberNavController()
                exposedController = navController
                BasicMusicNavHost(
                    allTracks = listOf(sampleTrack),
                    playlists = listOf(samplePlaylist),
                    playbackState = playbackState,
                    navController = navController,
                    onPlayMusicTrack = { queue, startId ->
                        val chosen = queue.firstOrNull { it.id == startId } ?: queue.first()
                        playbackState = playbackState.copy(
                            currentTrack = chosen,
                            queue = queue,
                            isPlaying = true,
                            durationMs = chosen.durationMs
                        )
                    }
                )
            }
        }

        composeTestRule.waitForIdle()
        val navController = requireNotNull(exposedController)

        // 1. Starts on Library route
        assertEquals(ROUTE_LIBRARY, navController.currentBackStackEntry?.destination?.route)
        composeTestRule.onNodeWithTag("library_tracks_list").assertIsDisplayed()

        // 2. Navigate to Playlists route via NavigationBar
        composeTestRule.onNodeWithTag("nav_item_playlists").performClick()
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_PLAYLISTS, navController.currentBackStackEntry?.destination?.route)
        composeTestRule.onNodeWithTag("playlists_list").assertIsDisplayed()

        // 3. Navigate to Player route while idle (no track loaded yet) -> renders IdlePlayerView cleanly
        composeTestRule.onNodeWithTag("nav_item_player").performClick()
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_PLAYER, navController.currentBackStackEntry?.destination?.route)
        composeTestRule.onNodeWithTag("player_view_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_idle_view").assertIsDisplayed()

        // 4. Tap Play inside IdlePlayerView -> starts first Library track and upgrades PlayerScreen to FullNowPlayingOverlay
        composeTestRule.onNodeWithTag("player_idle_play_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("nav_song_1", playbackState.currentTrack?.id)
        assertEquals(0, composeTestRule.onAllNodesWithTag("player_idle_view").fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag("player_view_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("full_player_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_seek_slider").assertIsDisplayed()

        // 5. Return to Library via NavigationBar item and re-open Player via library_open_player_button
        composeTestRule.onNodeWithTag("nav_item_library").performClick()
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_LIBRARY, navController.currentBackStackEntry?.destination?.route)
        composeTestRule.onNodeWithTag("library_open_player_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_PLAYER, navController.currentBackStackEntry?.destination?.route)
        composeTestRule.onNodeWithTag("player_view_screen").assertIsDisplayed()
    }

    @Test
    fun `NaghamAppNavHost manages backstack between tabs workspace Player view and Equalizer`() {
        val sampleTrack = CloudTrack(
            id = "app_nav_1",
            title = "Towards Home",
            artist = "Shawn Williams",
            durationMs = 210_000L,
            quranAudio = false
        )
        var playbackState by mutableStateOf(PlaybackState())
        var appNavController: NavHostController? = null

        composeTestRule.setContent {
            NaghamTheme(darkTheme = true, eyeComfortMode = false, languageCode = "en") {
                val navController = rememberNavController()
                appNavController = navController
                NaghamAppNavHost(
                    navController = navController,
                    startDestination = ROUTE_TABS_HOST,
                    tabsWorkspaceContent = {
                        LibraryScreen(
                            allTracks = listOf(sampleTrack),
                            playlists = emptyList(),
                            currentTrackId = playbackState.currentTrack?.id,
                            isPlaying = playbackState.isPlaying,
                            isScanningStorage = false,
                            onPlayMusicTrack = { list, id ->
                                val chosen = list.first { it.id == id }
                                playbackState = playbackState.copy(currentTrack = chosen, queue = list, isPlaying = true)
                            },
                            onToggleFavorite = {},
                            onEditTrack = {},
                            onJumpToQuranSanctuary = {},
                            onOpenPlayer = { navController.navigateSingleTopTo(ROUTE_PLAYER) }
                        )
                    },
                    libraryContent = {
                        LibraryScreen(
                            allTracks = listOf(sampleTrack),
                            playlists = emptyList(),
                            currentTrackId = playbackState.currentTrack?.id,
                            isPlaying = playbackState.isPlaying,
                            isScanningStorage = false,
                            onPlayMusicTrack = { _, _ -> },
                            onToggleFavorite = {},
                            onEditTrack = {},
                            onJumpToQuranSanctuary = {},
                            onOpenPlayer = { navController.navigateSingleTopTo(ROUTE_PLAYER) }
                        )
                    },
                    playlistsContent = {
                        PlaylistsScreen(
                            playlists = emptyList(),
                            allTracks = listOf(sampleTrack),
                            onPlayMusicQueue = { _, _ -> },
                            onCreatePlaylist = { _, _, _ -> },
                            onUpdatePlaylistTracks = { _, _ -> },
                            onDeletePlaylist = {},
                            onOpenImportExternalDialog = {},
                            onEditTrack = {},
                            onToggleFavorite = {}
                        )
                    },
                    playerContent = {
                        PlayerScreen(
                            playbackState = playbackState,
                            availableMusicTracks = listOf(sampleTrack),
                            onNavigateToLibrary = { navController.popOrNavigateToStart(ROUTE_TABS_HOST) },
                            onNavigateToPlaylists = { navController.navigateSingleTopTo(ROUTE_PLAYLISTS) },
                            onCollapse = { navController.popOrNavigateToStart(ROUTE_TABS_HOST) },
                            onTogglePlayPause = {},
                            onSkipNext = {},
                            onSkipPrevious = {},
                            onSeekTo = {},
                            onToggleShuffle = {},
                            onCycleRepeat = {},
                            onEditTrack = {},
                            onSelectPreset = {},
                            onBandChanged = { _, _ -> },
                            onBassChanged = {},
                            onSpeedChanged = {},
                            onSyncEqToCloud = {},
                            onOpenFullEqualizer = { navController.navigateSingleTopTo(ROUTE_EQUALIZER) }
                        )
                    },
                    equalizerContent = {
                        EqualizerScreen(
                            playbackState = playbackState,
                            onClose = { navController.popOrNavigateToStart(ROUTE_TABS_HOST) },
                            onToggleEnabled = {},
                            onResetFlat = {},
                            onSelectPreset = {},
                            onBandChanged = { _, _ -> },
                            onBassChanged = {},
                            onSpeedChanged = {},
                            onSyncToCloud = {}
                        )
                    }
                )
            }
        }

        composeTestRule.waitForIdle()
        val nav = requireNotNull(appNavController)
        assertEquals(ROUTE_TABS_HOST, nav.currentBackStackEntry?.destination?.route)

        // Open Player view from Library hero button -> pushes ROUTE_PLAYER
        composeTestRule.onNodeWithTag("library_open_player_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_PLAYER, nav.currentBackStackEntry?.destination?.route)
        composeTestRule.onNodeWithTag("player_idle_view").assertIsDisplayed()

        // Open Equalizer from Player header -> pushes ROUTE_EQUALIZER on top of ROUTE_PLAYER
        composeTestRule.onNodeWithTag("player_idle_equalizer_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_EQUALIZER, nav.currentBackStackEntry?.destination?.route)

        // Close Equalizer -> pops back to ROUTE_PLAYER
        composeTestRule.onNodeWithTag("eq_close_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_PLAYER, nav.currentBackStackEntry?.destination?.route)

        // Collapse Player -> pops back to ROUTE_TABS_HOST
        composeTestRule.onNodeWithTag("player_idle_back_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_TABS_HOST, nav.currentBackStackEntry?.destination?.route)
        composeTestRule.onNodeWithTag("library_tracks_list").assertIsDisplayed()
    }
}


