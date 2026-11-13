package com.example

import android.Manifest
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.audio.AudioPlayerController
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.firebase.signOutUser
import com.example.data.model.CloudTrack
import com.example.data.model.UserSettings
import com.example.data.repository.MusicRepository
import com.example.data.scanner.DeviceAudioScanner
import com.example.data.scanner.FolderFilterStore
import com.example.data.scanner.RecentPlaybackStore
import com.example.data.scanner.TrackSortStore
import com.example.ui.components.DraggableNavTab
import com.example.ui.components.ReorderableBottomNavigationBar
import com.example.ui.components.ReorderableSideNavigationRail
import com.example.ui.components.SmoothTabSwipeDeck
import com.example.ui.components.TrackLongPressSheet
import com.example.ui.components.rememberSmoothTabSwipeDeckState
import com.example.ui.localization.LocalAppStrings
import com.example.ui.navigation.NaghamAppNavHost
import com.example.ui.navigation.PlayerScreen
import com.example.ui.navigation.ROUTE_EQUALIZER
import com.example.ui.navigation.ROUTE_PLAYER
import com.example.ui.navigation.ROUTE_TABS_HOST
import com.example.ui.navigation.navigateSingleTopTo
import com.example.ui.navigation.popOrNavigateToStart
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.ExternalPlaylistImportDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MiniPlayerBar
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.QuranSanctuaryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TrackEditorDialog
import com.example.ui.theme.AppColorPalette
import com.example.ui.theme.AppDesignStyle
import com.example.ui.theme.NaghamTheme
import com.example.ui.theme.NaghamThemedBackdrop
import com.example.ui.theme.PlayerThemeStyle
import com.example.ui.theme.PlayerVisualEffect
import com.example.ui.theme.ThemePreferences
import com.example.ui.viewmodel.NaghamViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val ROUTE_HOME = "home"
const val ROUTE_LIBRARY = "library"
const val ROUTE_PLAYLISTS = "playlists"
const val ROUTE_QURAN = "quran_sanctuary"
const val ROUTE_SETTINGS = "settings"

private const val MAIN_TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private var latestExternalIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Firebase App Check wiring BEFORE any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)
        DeviceAudioScanner.deleteLegacyStarterAudioFiles(this)
        FolderFilterStore.ensureLoaded(this)
        RecentPlaybackStore.ensureLoaded(this)
        TrackSortStore.ensureLoaded(this)

        enableEdgeToEdge()
        latestExternalIntent = intent

        setContent {
            NaghamRootApp(
                incomingIntent = latestExternalIntent,
                onIntentConsumed = { latestExternalIntent = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        latestExternalIntent = intent
    }
}

@Composable
fun NaghamRootApp(
    incomingIntent: Intent?,
    onIntentConsumed: () -> Unit
) {
    val context = LocalContext.current
    val auth = remember(context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            Firebase.auth
        } catch (t: Throwable) {
            Log.e(MAIN_TAG, "FirebaseAuth unavailable: ${t.message}", t)
            null
        }
    }
    val authFlow = remember(auth) { auth?.authStateFlow() ?: flowOf(null) }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = auth?.currentUser)

    val savedLanguage = remember { ThemePreferences.getLanguageCode(context, fallback = "ar") }
    val savedDark = remember { ThemePreferences.getDarkTheme(context, fallback = true) }
    val savedPalette = remember {
        ThemePreferences.getColorPalette(context, fallback = AppColorPalette.AMETHYST_VELVET)
    }
    val savedDesignStyle = remember {
        ThemePreferences.getDesignStyle(context, fallback = AppDesignStyle.GLASS)
    }

    val user = currentUser
    if (user == null) {
        NaghamTheme(
            darkTheme = savedDark,
            eyeComfortMode = false,
            colorPalette = savedPalette,
            designStyle = savedDesignStyle,
            languageCode = savedLanguage
        ) {
            AuthScreen(
                onAuthSuccess = {}
            )
        }
    } else {
        val safeName = user.displayName?.trim().takeUnless { it.isNullOrEmpty() } ?: "مستمع نغم"
        AuthenticatedNaghamShell(
            currentUserId = user.uid,
            displayName = safeName,
            incomingIntent = incomingIntent,
            onIntentConsumed = onIntentConsumed
        )
    }
}

@Composable
private fun AuthenticatedNaghamShell(
    currentUserId: String,
    displayName: String,
    incomingIntent: Intent?,
    onIntentConsumed: () -> Unit
) {
    val viewModel: NaghamViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                NaghamViewModel(
                    repository = MusicRepository(db),
                    playerController = AudioPlayerController.getInstance(app.applicationContext),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName
                )
            }
        }
    )

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) {
        runCatching { CredentialManager.create(context) }.getOrNull()
    }

    val settingsUi by viewModel.settingsState.collectAsStateWithLifecycle()
    val tracksUi by viewModel.tracksState.collectAsStateWithLifecycle()
    val playlistsUi by viewModel.playlistsState.collectAsStateWithLifecycle()
    // Isolate 250ms playback position ticks from the root tab pager so swiping tabs stays 60/120fps smooth
    val activeTrackId by remember(viewModel) {
        viewModel.playbackState.map { it.currentTrack?.id }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = viewModel.playbackState.value.currentTrack?.id)
    val isPlayingNow by remember(viewModel) {
        viewModel.playbackState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = viewModel.playbackState.value.isPlaying)
    val hasActiveTrack = activeTrackId != null

    val recentTimestamps by viewModel.recentTimestamps.collectAsStateWithLifecycle()
    val deviceLibraryPlaylists by viewModel.deviceLibraryPlaylists.collectAsStateWithLifecycle()
    val lyricsDownloading by viewModel.lyricsDownloading.collectAsStateWithLifecycle()
    val isScanningStorage by viewModel.isScanningStorage.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val settings = (settingsUi as? UiState.Success)?.data
        ?: UserSettings(
            userId = currentUserId,
            displayName = displayName,
            darkTheme = ThemePreferences.getDarkTheme(context, fallback = true),
            languageCode = ThemePreferences.getLanguageCode(context, fallback = "ar")
        )
    val allTracks = (tracksUi as? UiState.Success)?.data ?: emptyList()
    val allPlaylists = (playlistsUi as? UiState.Success)?.data ?: emptyList()

    // Local UI Preferences (Color Palette, Design Style, Player Theme Style, Reorderable Tab Order)
    var selectedPalette by remember {
        mutableStateOf(ThemePreferences.getColorPalette(context, fallback = AppColorPalette.AMETHYST_VELVET))
    }
    var selectedDesignStyle by remember {
        mutableStateOf(ThemePreferences.getDesignStyle(context, fallback = AppDesignStyle.GLASS))
    }
    var selectedPlayerThemeStyle by remember {
        mutableStateOf(ThemePreferences.getPlayerThemeStyle(context, fallback = PlayerThemeStyle.AURORA_RING))
    }
    var selectedPlayerEffect by remember { mutableStateOf(ThemePreferences.getPlayerEffect(context)) }
    var tabOrder by remember { mutableStateOf(ThemePreferences.getTabOrder(context)) }

    var hasAudioPermission by remember {
        mutableStateOf(DeviceAudioScanner.hasAudioScanPermission(context))
    }
    var didAutoRequestPermission by rememberSaveable { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted || DeviceAudioScanner.hasAudioScanPermission(context)
        if (hasAudioPermission) {
            viewModel.autoSyncDeviceAudio(context, silent = true)
        }
    }

    val requestAudioPermissionIfNeeded: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                audioPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            } catch (t: Throwable) {
                Log.w(MAIN_TAG, "Unable to launch permission prompt: ${t.message}")
            }
        } else {
            hasAudioPermission = true
            viewModel.autoSyncDeviceAudio(context, silent = false)
        }
    }

    // Automatic Initial Sync & One-Time Permission Prompt
    LaunchedEffect(currentUserId) {
        FolderFilterStore.ensureLoaded(context)
        RecentPlaybackStore.ensureLoaded(context)
        TrackSortStore.ensureLoaded(context)
        if (ThemePreferences.hasExplicitTheme(context)) {
            viewModel.setDarkTheme(ThemePreferences.getDarkTheme(context, fallback = true))
        }
        hasAudioPermission = DeviceAudioScanner.hasAudioScanPermission(context)
        if (hasAudioPermission) {
            viewModel.autoSyncDeviceAudio(context, silent = true)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !didAutoRequestPermission) {
            didAutoRequestPermission = true
            try {
                audioPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            } catch (t: Throwable) {
                Log.w(MAIN_TAG, "Auto permission prompt skipped: ${t.message}")
            }
        }
    }

    // Automatic Live MediaStore Watcher + Lifecycle ON_RESUME Auto-Discovery for New Files
    DisposableEffect(currentUserId, lifecycleOwner) {
        var debounceJob: Job? = null
        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                debounceJob?.cancel()
                debounceJob = scope.launch {
                    delay(750L)
                    if (DeviceAudioScanner.hasAudioScanPermission(context)) {
                        viewModel.autoSyncDeviceAudio(context, silent = true)
                    }
                }
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                contentObserver
            )
        } catch (_: Throwable) {
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAudioPermission = DeviceAudioScanner.hasAudioScanPermission(context)
                if (hasAudioPermission) {
                    viewModel.autoSyncDeviceAudio(context, silent = true)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            debounceJob?.cancel()
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            try {
                context.contentResolver.unregisterContentObserver(contentObserver)
            } catch (_: Throwable) {
            }
        }
    }

    LaunchedEffect(incomingIntent) {
        if (incomingIntent != null &&
            (incomingIntent.action == Intent.ACTION_VIEW ||
                incomingIntent.action == Intent.ACTION_SEND ||
                incomingIntent.action == Intent.ACTION_SEND_MULTIPLE)
        ) {
            viewModel.handleExternalIntent(context, incomingIntent)
            onIntentConsumed()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    var savedActiveRoute by rememberSaveable { mutableStateOf(ROUTE_HOME) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentNavRoute = navBackStackEntry?.destination?.route ?: ROUTE_TABS_HOST
    val isInTabsWorkspace = currentNavRoute == ROUTE_TABS_HOST
    val availableMusicTracks = remember(allTracks) { allTracks.filter { !it.quranAudio } }
    var longPressedTrack by remember { mutableStateOf<CloudTrack?>(null) }
    var editingTrack by remember { mutableStateOf<CloudTrack?>(null) }
    var librarySeedSelectTrackId by remember { mutableStateOf<String?>(null) }
    var quranSeedSelectTrackId by remember { mutableStateOf<String?>(null) }
    var showImportPlaylistModal by remember { mutableStateOf(false) }
    var pendingPlaylistImportName by remember { mutableStateOf("") }
    var pendingLrcCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val pickExternalPlaylistLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importExternalPlaylistFromUri(context, uri, pendingPlaylistImportName)
            showImportPlaylistModal = false
        }
    }

    val pickLrcFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val content = try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)
                            ?.bufferedReader(Charsets.UTF_8)
                            ?.use { it.readText() }
                            .orEmpty()
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Throwable) {
                    ""
                }
                if (content.isNotBlank()) {
                    pendingLrcCallback?.invoke(content.take(14500))
                }
            }
        }
    }

    NaghamTheme(
        darkTheme = settings.darkTheme,
        eyeComfortMode = false,
        colorPalette = selectedPalette,
        designStyle = selectedDesignStyle,
        languageCode = settings.languageCode
    ) {
        val strings = LocalAppStrings.current

        val catalogByRoute = remember(strings) {
            linkedMapOf(
                ROUTE_HOME to DraggableNavTab(
                    route = ROUTE_HOME,
                    label = strings.navHome,
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    testTag = "nav_tab_home"
                ),
                ROUTE_LIBRARY to DraggableNavTab(
                    route = ROUTE_LIBRARY,
                    label = strings.navLibrary,
                    selectedIcon = Icons.Filled.LibraryMusic,
                    unselectedIcon = Icons.Outlined.LibraryMusic,
                    testTag = "nav_tab_library"
                ),
                ROUTE_PLAYLISTS to DraggableNavTab(
                    route = ROUTE_PLAYLISTS,
                    label = strings.navPlaylists,
                    selectedIcon = Icons.AutoMirrored.Filled.QueueMusic,
                    unselectedIcon = Icons.AutoMirrored.Outlined.QueueMusic,
                    testTag = "nav_tab_playlists"
                ),
                ROUTE_QURAN to DraggableNavTab(
                    route = ROUTE_QURAN,
                    label = strings.navQuran,
                    selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
                    unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
                    testTag = "nav_tab_quran"
                ),
                ROUTE_SETTINGS to DraggableNavTab(
                    route = ROUTE_SETTINGS,
                    label = strings.navSettings,
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    testTag = "nav_tab_settings"
                )
            )
        }

        val orderedTabs = remember(tabOrder, catalogByRoute) {
            tabOrder
                .mapNotNull { catalogByRoute[it] }
                .distinctBy { it.route }
                .ifEmpty { catalogByRoute.values.toList() }
        }

        val orderedRoutes = remember(orderedTabs) {
            orderedTabs.map { it.route }
        }

        val tabDeckState = rememberSmoothTabSwipeDeckState(
            tabs = orderedRoutes,
            initialRoute = savedActiveRoute
        )
        val activeRoute = tabDeckState.activeRoute

        // One-way persistence sync so configuration changes restore active tab without feedback loops
        LaunchedEffect(activeRoute) {
            savedActiveRoute = activeRoute
        }

        val selectRoute: (String) -> Unit = remember(tabDeckState, scope) {
            { route ->
                tabDeckState.navigateToRoute(
                    route = route,
                    scope = scope,
                    animate = true
                )
            }
        }

        val handleMoveTab: (Int, Int) -> Unit = { fromIndex, toIndex ->
            val mutable = tabOrder.toMutableList()
            if (fromIndex in mutable.indices && toIndex in mutable.indices) {
                val moved = mutable.removeAt(fromIndex)
                mutable.add(toIndex, moved)
                tabOrder = mutable
                ThemePreferences.saveTabOrder(context, mutable)
                tabDeckState.syncTabsOrder(mutable)
            }
        }

        val showPermissionPromptBanner = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasAudioPermission

        // Outer BackHandler returns non-Home tabs smoothly to Home when inside the main tabs workspace
        BackHandler(enabled = isInTabsWorkspace && activeRoute != ROUTE_HOME) {
            selectRoute(ROUTE_HOME)
        }

        val openPlayerRoute: () -> Unit = {
            navController.navigateSingleTopTo(ROUTE_PLAYER)
        }
        val openEqualizerRoute: () -> Unit = {
            navController.navigateSingleTopTo(ROUTE_EQUALIZER)
        }

        NaghamThemedBackdrop(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isExpandedScreen = maxWidth >= 720.dp

                NaghamAppNavHost(
                    navController = navController,
                    startDestination = ROUTE_TABS_HOST,
                    tabsWorkspaceContent = {
                        Scaffold(
                            contentWindowInsets = WindowInsets.safeDrawing,
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            containerColor = Color.Transparent,
                            bottomBar = {
                                if (!isExpandedScreen) {
                                    Column {
                                        AnimatedVisibility(
                                            visible = hasActiveTrack && currentNavRoute != ROUTE_PLAYER,
                                            enter = slideInVertically(
                                                animationSpec = tween(200, easing = FastOutSlowInEasing)
                                            ) { it / 2 } + fadeIn(tween(170)),
                                            exit = slideOutVertically(
                                                animationSpec = tween(170)
                                            ) { it / 2 } + fadeOut(tween(140))
                                        ) {
                                            LiveMiniPlayerSlot(
                                                viewModel = viewModel,
                                                onExpand = openPlayerRoute,
                                                onOpenEqualizer = openEqualizerRoute
                                            )
                                        }
                                        ReorderableBottomNavigationBar(
                                            tabs = orderedTabs,
                                            selectedRoute = activeRoute,
                                            onSelectTab = { _, tab ->
                                                selectRoute(tab.route)
                                            },
                                            onMoveTab = handleMoveTab,
                                            swipePositionProvider = { tabDeckState.visualPosition }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                if (isExpandedScreen) {
                                    ReorderableSideNavigationRail(
                                        tabs = orderedTabs,
                                        selectedRoute = activeRoute,
                                        onSelectTab = { _, tab ->
                                            selectRoute(tab.route)
                                        },
                                        onMoveTab = handleMoveTab
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    SmoothTabSwipeDeck(
                                        state = tabDeckState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("main_tab_swipe_deck")
                                    ) { currentTabRoute, isTabActive ->
                                        when (currentTabRoute) {
                                            ROUTE_HOME -> HomeScreen(
                                                allTracks = allTracks,
                                                recentTimestamps = recentTimestamps,
                                                currentTrackId = activeTrackId,
                                                isPlaying = isPlayingNow,
                                                isScanningStorage = isScanningStorage,
                                                showGrantAudioPermissionButton = showPermissionPromptBanner,
                                                onRequestAudioPermission = requestAudioPermissionIfNeeded,
                                                onPlayMusicTrack = { list, id -> viewModel.playMusic(list, id) },
                                                onLongPressTrack = { longPressedTrack = it },
                                                onJumpToQuranSanctuary = { selectRoute(ROUTE_QURAN) },
                                                onOpenEqualizer = openEqualizerRoute,
                                                isActiveTab = isTabActive
                                            )

                                            ROUTE_LIBRARY -> LibraryScreen(
                                                allTracks = allTracks,
                                                playlists = allPlaylists,
                                                currentTrackId = activeTrackId,
                                                isPlaying = isPlayingNow,
                                                isScanningStorage = isScanningStorage,
                                                initialSelectedTrackId = librarySeedSelectTrackId,
                                                onInitialSelectionConsumed = { librarySeedSelectTrackId = null },
                                                onPlayMusicTrack = { list, id -> viewModel.playMusic(list, id) },
                                                onToggleFavorite = { viewModel.toggleTrackFavorite(it) },
                                                onEditTrack = { editingTrack = it },
                                                onLongPressTrack = { longPressedTrack = it },
                                                onBatchDeleteTracks = { selected ->
                                                    viewModel.deleteTracksBatch(context, selected)
                                                },
                                                onBatchAddTracksToPlaylist = { selected, pl ->
                                                    viewModel.addTracksToPlaylistBatch(selected, pl)
                                                },
                                                onBatchCreatePlaylistWithTracks = { name, selected ->
                                                    viewModel.createPlaylistWithTracksBatch(name, selected)
                                                },
                                                onJumpToQuranSanctuary = { selectRoute(ROUTE_QURAN) },
                                                onOpenPlayer = openPlayerRoute,
                                                isActiveTab = isTabActive
                                            )

                                            ROUTE_PLAYLISTS -> PlaylistsScreen(
                                                playlists = allPlaylists,
                                                allTracks = allTracks,
                                                currentTrackId = activeTrackId,
                                                isPlaying = isPlayingNow,
                                                onPlayMusicQueue = { list, startId -> viewModel.playPlaylist(list, startId) },
                                                onCreatePlaylist = { name, desc, ids ->
                                                    viewModel.createCustomPlaylist(name, desc, ids)
                                                },
                                                onUpdatePlaylistTracks = { pl, ids ->
                                                    viewModel.updatePlaylistTracks(pl, ids)
                                                },
                                                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                                onOpenImportExternalDialog = {
                                                    viewModel.refreshDeviceLibraryPlaylists(context)
                                                    showImportPlaylistModal = true
                                                },
                                                onEditTrack = { editingTrack = it },
                                                onToggleFavorite = { viewModel.toggleTrackFavorite(it) },
                                                onLongPressTrack = { longPressedTrack = it },
                                                onBatchDeleteTracks = { selected ->
                                                    viewModel.deleteTracksBatch(context, selected)
                                                },
                                                onBatchAddTracksToPlaylist = { selected, pl ->
                                                    viewModel.addTracksToPlaylistBatch(selected, pl)
                                                },
                                                onBatchCreatePlaylistWithTracks = { name, selected ->
                                                    viewModel.createPlaylistWithTracksBatch(name, selected)
                                                },
                                                onBack = { selectRoute(ROUTE_HOME) },
                                                isActiveTab = isTabActive
                                            )

                                            ROUTE_QURAN -> QuranSanctuaryScreen(
                                                allTracks = allTracks,
                                                playlists = allPlaylists,
                                                currentTrackId = activeTrackId,
                                                isPlaying = isPlayingNow,
                                                isScanningStorage = isScanningStorage,
                                                initialSelectedTrackId = quranSeedSelectTrackId,
                                                onInitialSelectionConsumed = { quranSeedSelectTrackId = null },
                                                onPlayQuranQueue = { list, id -> viewModel.playQuran(list, id) },
                                                onEditTrack = { editingTrack = it },
                                                onLongPressTrack = { longPressedTrack = it },
                                                onBatchDeleteTracks = { selected ->
                                                    viewModel.deleteTracksBatch(context, selected)
                                                },
                                                onBatchAddTracksToPlaylist = { selected, pl ->
                                                    viewModel.addTracksToPlaylistBatch(selected, pl)
                                                },
                                                onBatchCreatePlaylistWithTracks = { name, selected ->
                                                    viewModel.createPlaylistWithTracksBatch(name, selected)
                                                },
                                                onBack = { selectRoute(ROUTE_HOME) },
                                                isActiveTab = isTabActive
                                            )

                                            ROUTE_SETTINGS -> SettingsScreen(
                                                settings = settings,
                                                selectedPalette = selectedPalette,
                                                selectedDesignStyle = selectedDesignStyle,
                                                selectedPlayerThemeStyle = selectedPlayerThemeStyle,
                                                selectedVisualEffect = selectedPlayerEffect,
                                                isScanningStorage = isScanningStorage,
                                                showGrantAudioPermissionButton = showPermissionPromptBanner,
                                                onRequestAudioPermission = requestAudioPermissionIfNeeded,
                                                onDarkThemeChange = { isDark ->
                                                    ThemePreferences.saveDarkTheme(context, isDark)
                                                    viewModel.setDarkTheme(isDark)
                                                },
                                                onSelectPalette = { palette ->
                                                    selectedPalette = palette
                                                    ThemePreferences.saveColorPalette(context, palette)
                                                },
                                                onSelectDesignStyle = { style ->
                                                    selectedDesignStyle = style
                                                    ThemePreferences.saveDesignStyle(context, style)
                                                },
                                                onSelectPlayerThemeStyle = { pTheme ->
                                                    selectedPlayerThemeStyle = pTheme
                                                    ThemePreferences.savePlayerThemeStyle(context, pTheme)
                                                },
                                                onSelectVisualEffect = { effect ->
                                                    selectedPlayerEffect = effect
                                                    ThemePreferences.savePlayerEffect(context, effect)
                                                },
                                                onLanguageChange = { lang ->
                                                    ThemePreferences.saveLanguageCode(context, lang)
                                                    viewModel.setLanguage(lang)
                                                },
                                                onToggleAutoIsolateQuran = { viewModel.toggleAutoIsolateQuran() },
                                                onResetTabOrder = {
                                                    tabOrder = ThemePreferences.DEFAULT_TABS
                                                    ThemePreferences.saveTabOrder(context, ThemePreferences.DEFAULT_TABS)
                                                    tabDeckState.syncTabsOrder(ThemePreferences.DEFAULT_TABS)
                                                },
                                                onSignOut = {
                                                    AudioPlayerController.getInstance(context).stopAndClear()
                                                    signOutUser(credentialManager, onSignOutComplete = {}, scope = scope)
                                                },
                                                onOpenEqualizer = openEqualizerRoute,
                                                onBack = { selectRoute(ROUTE_HOME) },
                                                isActiveTab = isTabActive
                                            )
                                        }
                                    }

                                    if (isExpandedScreen) {
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = hasActiveTrack && currentNavRoute != ROUTE_PLAYER,
                                            enter = slideInVertically(tween(200, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(170)),
                                            exit = slideOutVertically(tween(170)) { it / 2 } + fadeOut(tween(140)),
                                            modifier = Modifier.align(Alignment.BottomCenter)
                                        ) {
                                            LiveMiniPlayerSlot(
                                                viewModel = viewModel,
                                                onExpand = openPlayerRoute,
                                                onOpenEqualizer = openEqualizerRoute
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    libraryContent = {
                        LibraryScreen(
                            allTracks = allTracks,
                            playlists = allPlaylists,
                            currentTrackId = activeTrackId,
                            isPlaying = isPlayingNow,
                            isScanningStorage = isScanningStorage,
                            initialSelectedTrackId = librarySeedSelectTrackId,
                            onInitialSelectionConsumed = { librarySeedSelectTrackId = null },
                            onPlayMusicTrack = { list, id -> viewModel.playMusic(list, id) },
                            onToggleFavorite = { viewModel.toggleTrackFavorite(it) },
                            onEditTrack = { editingTrack = it },
                            onLongPressTrack = { longPressedTrack = it },
                            onBatchDeleteTracks = { selected ->
                                viewModel.deleteTracksBatch(context, selected)
                            },
                            onBatchAddTracksToPlaylist = { selected, pl ->
                                viewModel.addTracksToPlaylistBatch(selected, pl)
                            },
                            onBatchCreatePlaylistWithTracks = { name, selected ->
                                viewModel.createPlaylistWithTracksBatch(name, selected)
                            },
                            onJumpToQuranSanctuary = {
                                selectRoute(ROUTE_QURAN)
                                navController.popOrNavigateToStart(ROUTE_TABS_HOST)
                            },
                            onOpenPlayer = openPlayerRoute
                        )
                    },
                    playlistsContent = {
                        PlaylistsScreen(
                            playlists = allPlaylists,
                            allTracks = allTracks,
                            currentTrackId = activeTrackId,
                            isPlaying = isPlayingNow,
                            onPlayMusicQueue = { list, startId -> viewModel.playPlaylist(list, startId) },
                            onCreatePlaylist = { name, desc, ids ->
                                viewModel.createCustomPlaylist(name, desc, ids)
                            },
                            onUpdatePlaylistTracks = { pl, ids ->
                                viewModel.updatePlaylistTracks(pl, ids)
                            },
                            onDeletePlaylist = { viewModel.deletePlaylist(it) },
                            onOpenImportExternalDialog = {
                                viewModel.refreshDeviceLibraryPlaylists(context)
                                showImportPlaylistModal = true
                            },
                            onEditTrack = { editingTrack = it },
                            onToggleFavorite = { viewModel.toggleTrackFavorite(it) },
                            onLongPressTrack = { longPressedTrack = it },
                            onBatchDeleteTracks = { selected ->
                                viewModel.deleteTracksBatch(context, selected)
                            },
                            onBatchAddTracksToPlaylist = { selected, pl ->
                                viewModel.addTracksToPlaylistBatch(selected, pl)
                            },
                            onBatchCreatePlaylistWithTracks = { name, selected ->
                                viewModel.createPlaylistWithTracksBatch(name, selected)
                            },
                            onBack = { navController.popOrNavigateToStart(ROUTE_TABS_HOST) }
                        )
                    },
                    playerContent = {
                        LiveFullPlayerOverlaySlot(
                            viewModel = viewModel,
                            availableMusicTracks = availableMusicTracks,
                            playerThemeStyle = selectedPlayerThemeStyle,
                            visualEffect = selectedPlayerEffect,
                            onSelectVisualEffect = { effect ->
                                selectedPlayerEffect = effect
                                ThemePreferences.savePlayerEffect(context, effect)
                            },
                            onNavigateToLibrary = {
                                selectRoute(ROUTE_LIBRARY)
                                navController.popOrNavigateToStart(ROUTE_TABS_HOST)
                            },
                            onNavigateToPlaylists = {
                                selectRoute(ROUTE_PLAYLISTS)
                                navController.popOrNavigateToStart(ROUTE_TABS_HOST)
                            },
                            onCollapse = { navController.popOrNavigateToStart(ROUTE_TABS_HOST) },
                            onEditTrack = { editingTrack = it },
                            onAddToPlaylistRequest = { longPressedTrack = it },
                            onOpenFullEqualizer = openEqualizerRoute
                        )
                    },
                    equalizerContent = {
                        LiveEqualizerOverlaySlot(
                            viewModel = viewModel,
                            onClose = { navController.popOrNavigateToStart(ROUTE_TABS_HOST) }
                        )
                    }
                )

                longPressedTrack?.let { selected ->
                    TrackLongPressSheet(
                        track = selected,
                        playlists = allPlaylists,
                        onDismiss = { longPressedTrack = null },
                        onAddTrackToPlaylist = { trk, pl ->
                            viewModel.addTrackToPlaylist(trk, pl)
                        },
                        onCreatePlaylistWithTrack = { name, trk ->
                            viewModel.createPlaylistWithTrack(name, trk)
                        },
                        onMoveTrackIsolation = { trk, moveToQuran ->
                            viewModel.moveTrackBetweenQuranAndMusic(trk, moveToQuran)
                        },
                        onEditTrack = { trk ->
                            editingTrack = trk
                        },
                        onDeleteTrack = { trk ->
                            viewModel.deleteTrack(context, trk)
                        },
                        onSelectMultipleFromHere = { trk ->
                            if (trk.quranAudio) {
                                quranSeedSelectTrackId = trk.id
                                selectRoute(ROUTE_QURAN)
                            } else {
                                librarySeedSelectTrackId = trk.id
                                selectRoute(ROUTE_LIBRARY)
                            }
                            navController.popOrNavigateToStart(ROUTE_TABS_HOST)
                        }
                    )
                }

                editingTrack?.let { target ->
                    TrackEditorDialog(
                        track = target,
                        isDownloadingLyrics = lyricsDownloading,
                        onDismiss = { editingTrack = null },
                        onSave = { updated -> viewModel.saveEditedTrack(updated) },
                        onDelete = { id -> viewModel.deleteTrackById(context, id) },
                        onDownloadLyrics = { t, a, cb -> viewModel.downloadLyricsForEditor(t, a, cb) },
                        onPickLrcFileRequest = { cb ->
                            pendingLrcCallback = cb
                            try {
                                pickLrcFileLauncher.launch(arrayOf("text/*", "*/*"))
                            } catch (t: Throwable) {
                                Log.w(MAIN_TAG, "Document picker unavailable: ${t.message}")
                            }
                        }
                    )
                }

                if (showImportPlaylistModal) {
                    ExternalPlaylistImportDialog(
                        devicePlaylists = deviceLibraryPlaylists,
                        onImportDevicePlaylist = { devPl, customName ->
                            viewModel.importDeviceLibraryPlaylist(context, devPl, customName)
                        },
                        onDismiss = { showImportPlaylistModal = false },
                        onPickFile = { name ->
                            pendingPlaylistImportName = name
                            try {
                                pickExternalPlaylistLauncher.launch(
                                    arrayOf(
                                        "audio/x-mpegurl",
                                        "audio/mpegurl",
                                        "audio/x-scpls",
                                        "application/vnd.apple.mpegurl",
                                        "text/plain",
                                        "*/*"
                                    )
                                )
                            } catch (t: Throwable) {
                                Log.w(MAIN_TAG, "Playlist picker unavailable: ${t.message}")
                            }
                        },
                        onImportRawText = { raw, name ->
                            viewModel.importExternalPlaylistFromText(raw, name)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveMiniPlayerSlot(
    viewModel: NaghamViewModel,
    onExpand: () -> Unit,
    onOpenEqualizer: () -> Unit = {}
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    var lastNonNullPlayback by remember { mutableStateOf(playbackState) }
    if (playbackState.currentTrack != null) {
        lastNonNullPlayback = playbackState
    }
    MiniPlayerBar(
        playbackState = if (playbackState.currentTrack != null) playbackState else lastNonNullPlayback,
        onExpand = onExpand,
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onSkipNext = { viewModel.skipNext() },
        onSwipePreviousTrack = { viewModel.skipToPreviousTrack() },
        onOpenEqualizer = onOpenEqualizer
    )
}

@Composable
private fun LiveFullPlayerOverlaySlot(
    viewModel: NaghamViewModel,
    availableMusicTracks: List<CloudTrack>,
    playerThemeStyle: PlayerThemeStyle,
    visualEffect: PlayerVisualEffect,
    onSelectVisualEffect: (PlayerVisualEffect) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onCollapse: () -> Unit,
    onEditTrack: (CloudTrack) -> Unit,
    onAddToPlaylistRequest: (CloudTrack) -> Unit,
    onOpenFullEqualizer: () -> Unit = {}
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    PlayerScreen(
        playbackState = playbackState,
        availableMusicTracks = availableMusicTracks,
        playerThemeStyle = playerThemeStyle,
        visualEffect = visualEffect,
        onSelectVisualEffect = onSelectVisualEffect,
        onPlayAvailableTracks = { list, id -> viewModel.playMusic(list, id) },
        onNavigateToLibrary = onNavigateToLibrary,
        onNavigateToPlaylists = onNavigateToPlaylists,
        onCollapse = onCollapse,
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onSkipNext = { viewModel.skipNext() },
        onSkipPrevious = { viewModel.skipPrevious() },
        onSwipePreviousTrack = { viewModel.skipToPreviousTrack() },
        onSeekTo = { viewModel.seekTo(it) },
        onToggleShuffle = { viewModel.toggleShuffle() },
        onCycleRepeat = { viewModel.cycleRepeatMode() },
        onEditTrack = onEditTrack,
        onToggleFavorite = { viewModel.toggleTrackFavorite(it) },
        onAddToPlaylistRequest = onAddToPlaylistRequest,
        onSelectQueueTrack = { chosen ->
            val q = playbackState.queue.ifEmpty { listOf(chosen) }
            when {
                playbackState.isMixedPlaylistQueue -> viewModel.playPlaylist(q, chosen.id)
                playbackState.isQuranMode || chosen.quranAudio -> viewModel.playQuran(q, chosen.id)
                else -> viewModel.playMusic(q, chosen.id)
            }
        },
        onSelectPreset = { viewModel.selectEqualizerPreset(it) },
        onBandChanged = { idx, mb -> viewModel.updateEqualizerBand(idx, mb) },
        onBassChanged = { viewModel.updateBassBoost(it) },
        onSpeedChanged = { viewModel.setPlaybackSpeed(it) },
        onSyncEqToCloud = { viewModel.syncCurrentEqualizerToCloud() },
        onToggleEqEnabled = { viewModel.setEqualizerEnabled(it) },
        onResetEq = { viewModel.resetEqualizerToFlat() },
        onOpenFullEqualizer = onOpenFullEqualizer
    )
}

@Composable
private fun LiveEqualizerOverlaySlot(
    viewModel: NaghamViewModel,
    onClose: () -> Unit
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    EqualizerScreen(
        playbackState = playbackState,
        onClose = onClose,
        onToggleEnabled = { viewModel.setEqualizerEnabled(it) },
        onResetFlat = { viewModel.resetEqualizerToFlat() },
        onSelectPreset = { viewModel.selectEqualizerPreset(it) },
        onBandChanged = { idx, mb -> viewModel.updateEqualizerBand(idx, mb) },
        onBassChanged = { viewModel.updateBassBoost(it) },
        onSpeedChanged = { viewModel.setPlaybackSpeed(it) },
        onSyncToCloud = { viewModel.syncCurrentEqualizerToCloud() }
    )
}

