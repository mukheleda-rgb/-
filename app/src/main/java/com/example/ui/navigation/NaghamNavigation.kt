package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ROUTE_LIBRARY
import com.example.ROUTE_PLAYLISTS
import com.example.audio.PlaybackState
import com.example.data.model.CloudPlaylist
import com.example.data.model.CloudTrack
import com.example.data.model.EqualizerPreset
import com.example.ui.localization.LocalAppStrings
import com.example.ui.screens.FullNowPlayingOverlay
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.theme.LocalNaghamColors
import com.example.ui.theme.LocalNaghamDesign
import com.example.ui.theme.PlayerThemeStyle
import com.example.ui.theme.PlayerVisualEffect
import com.example.ui.theme.naghamDesignBorder

const val ROUTE_TABS_HOST = "tabs_host"
const val ROUTE_PLAYER = "player"
const val ROUTE_EQUALIZER = "equalizer"

fun NavHostController.navigateSingleTopTo(route: String) {
    if (currentBackStackEntry?.destination?.route == route) return
    navigate(route) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateTopLevelDestination(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavHostController.popOrNavigateToStart(fallbackRoute: String = ROUTE_TABS_HOST) {
    val currentRoute = currentBackStackEntry?.destination?.route
    if (currentRoute != null && currentRoute != fallbackRoute) {
        if (!popBackStack()) {
            navigateSingleTopTo(fallbackRoute)
        }
    }
}

/**
 * Basic Jetpack Navigation Compose [NavHost] wiring the Library, Playlists, and Player view screens.
 */
@Composable
fun BasicMusicNavHost(
    allTracks: List<CloudTrack>,
    playlists: List<CloudPlaylist>,
    playbackState: PlaybackState,
    isScanningStorage: Boolean = false,
    playerThemeStyle: PlayerThemeStyle = PlayerThemeStyle.AURORA_RING,
    visualEffect: PlayerVisualEffect = PlayerVisualEffect.VINYL_AURA,
    showBottomBar: Boolean = true,
    startDestination: String = ROUTE_LIBRARY,
    navController: NavHostController = rememberNavController(),
    onPlayMusicTrack: (List<CloudTrack>, String) -> Unit = { _, _ -> },
    onPlayPlaylistQueue: (List<CloudTrack>, String?) -> Unit = { _, _ -> },
    onTogglePlayPause: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    onToggleFavorite: (CloudTrack) -> Unit = {},
    onEditTrack: (CloudTrack) -> Unit = {},
    onCreatePlaylist: (String, String, List<String>) -> Unit = { _, _, _ -> },
    onUpdatePlaylistTracks: (CloudPlaylist, List<String>) -> Unit = { _, _ -> },
    onDeletePlaylist: (String) -> Unit = {},
    onOpenImportExternalDialog: () -> Unit = {},
    onJumpToQuranSanctuary: () -> Unit = {},
    onSelectPreset: (EqualizerPreset) -> Unit = {},
    onBandChanged: (Int, Long) -> Unit = { _, _ -> },
    onBassChanged: (Long) -> Unit = {},
    onSpeedChanged: (Float) -> Unit = {},
    onSyncEqToCloud: () -> Unit = {},
    onToggleEqEnabled: (Boolean) -> Unit = {},
    onResetEq: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: startDestination
    val availableMusicTracks = remember(allTracks) { allTracks.filter { !it.quranAudio } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("basic_music_bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_LIBRARY,
                        onClick = { navController.navigateTopLevelDestination(ROUTE_LIBRARY) },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == ROUTE_LIBRARY) {
                                    Icons.Filled.LibraryMusic
                                } else {
                                    Icons.Outlined.LibraryMusic
                                },
                                contentDescription = strings.navLibrary
                            )
                        },
                        label = { Text(strings.navLibrary) },
                        modifier = Modifier.testTag("nav_item_library")
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_PLAYLISTS,
                        onClick = { navController.navigateTopLevelDestination(ROUTE_PLAYLISTS) },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == ROUTE_PLAYLISTS) {
                                    Icons.AutoMirrored.Filled.QueueMusic
                                } else {
                                    Icons.AutoMirrored.Outlined.QueueMusic
                                },
                                contentDescription = strings.navPlaylists
                            )
                        },
                        label = { Text(strings.navPlaylists) },
                        modifier = Modifier.testTag("nav_item_playlists")
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_PLAYER,
                        onClick = { navController.navigateTopLevelDestination(ROUTE_PLAYER) },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == ROUTE_PLAYER) {
                                    Icons.Filled.PlayCircle
                                } else {
                                    Icons.Outlined.PlayCircle
                                },
                                contentDescription = strings.navPlayer
                            )
                        },
                        label = { Text(strings.navPlayer) },
                        modifier = Modifier.testTag("nav_item_player")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { fadeIn(tween(180)) },
            exitTransition = { fadeOut(tween(150)) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("basic_music_nav_host")
        ) {
            composable(ROUTE_LIBRARY) {
                LibraryScreen(
                    allTracks = allTracks,
                    playlists = playlists,
                    currentTrackId = playbackState.currentTrack?.id,
                    isPlaying = playbackState.isPlaying,
                    isScanningStorage = isScanningStorage,
                    onPlayMusicTrack = onPlayMusicTrack,
                    onToggleFavorite = onToggleFavorite,
                    onEditTrack = onEditTrack,
                    onJumpToQuranSanctuary = onJumpToQuranSanctuary,
                    onOpenPlayer = { navController.navigateTopLevelDestination(ROUTE_PLAYER) }
                )
            }

            composable(ROUTE_PLAYLISTS) {
                PlaylistsScreen(
                    playlists = playlists,
                    allTracks = allTracks,
                    currentTrackId = playbackState.currentTrack?.id,
                    isPlaying = playbackState.isPlaying,
                    onPlayMusicQueue = onPlayPlaylistQueue,
                    onCreatePlaylist = onCreatePlaylist,
                    onUpdatePlaylistTracks = onUpdatePlaylistTracks,
                    onDeletePlaylist = onDeletePlaylist,
                    onOpenImportExternalDialog = onOpenImportExternalDialog,
                    onEditTrack = onEditTrack,
                    onToggleFavorite = onToggleFavorite,
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigateTopLevelDestination(ROUTE_LIBRARY)
                        }
                    }
                )
            }

            composable(ROUTE_PLAYER) {
                PlayerScreen(
                    playbackState = playbackState,
                    availableMusicTracks = availableMusicTracks,
                    playerThemeStyle = playerThemeStyle,
                    visualEffect = visualEffect,
                    onPlayAvailableTracks = onPlayMusicTrack,
                    onNavigateToLibrary = { navController.navigateTopLevelDestination(ROUTE_LIBRARY) },
                    onNavigateToPlaylists = { navController.navigateTopLevelDestination(ROUTE_PLAYLISTS) },
                    onCollapse = {
                        if (!navController.popBackStack()) {
                            navController.navigateTopLevelDestination(ROUTE_LIBRARY)
                        }
                    },
                    onTogglePlayPause = onTogglePlayPause,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onSeekTo = onSeekTo,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    onEditTrack = onEditTrack,
                    onToggleFavorite = onToggleFavorite,
                    onSelectPreset = onSelectPreset,
                    onBandChanged = onBandChanged,
                    onBassChanged = onBassChanged,
                    onSpeedChanged = onSpeedChanged,
                    onSyncEqToCloud = onSyncEqToCloud,
                    onToggleEqEnabled = onToggleEqEnabled,
                    onResetEq = onResetEq
                )
            }
        }
    }
}

/**
 * Full-application [NavHost] hosting the GPU tab swipe deck alongside dedicated
 * Library, Playlists, Player, and Equalizer destinations with real backstack transitions.
 */
@Composable
fun NaghamAppNavHost(
    navController: NavHostController,
    startDestination: String = ROUTE_TABS_HOST,
    tabsWorkspaceContent: @Composable () -> Unit,
    libraryContent: @Composable () -> Unit,
    playlistsContent: @Composable () -> Unit,
    playerContent: @Composable () -> Unit,
    equalizerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(180)) },
        exitTransition = { fadeOut(tween(150)) },
        popEnterTransition = { fadeIn(tween(160)) },
        popExitTransition = { fadeOut(tween(150)) },
        modifier = modifier
            .fillMaxSize()
            .testTag("nagham_nav_host")
    ) {
        composable(ROUTE_TABS_HOST) {
            tabsWorkspaceContent()
        }

        composable(ROUTE_LIBRARY) {
            libraryContent()
        }

        composable(ROUTE_PLAYLISTS) {
            playlistsContent()
        }

        composable(
            route = ROUTE_PLAYER,
            enterTransition = {
                slideInVertically(
                    animationSpec = tween(230, easing = FastOutSlowInEasing)
                ) { it } + fadeIn(tween(180))
            },
            exitTransition = {
                fadeOut(tween(160))
            },
            popEnterTransition = {
                fadeIn(tween(160))
            },
            popExitTransition = {
                slideOutVertically(
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ) { it } + fadeOut(tween(150))
            }
        ) {
            playerContent()
        }

        composable(
            route = ROUTE_EQUALIZER,
            enterTransition = {
                slideInVertically(
                    animationSpec = tween(230, easing = FastOutSlowInEasing)
                ) { it } + fadeIn(tween(180))
            },
            exitTransition = {
                slideOutVertically(
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ) { it } + fadeOut(tween(150))
            },
            popEnterTransition = {
                fadeIn(tween(160))
            },
            popExitTransition = {
                slideOutVertically(
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ) { it } + fadeOut(tween(150))
            }
        ) {
            equalizerContent()
        }
    }
}

/**
 * Dedicated Player View destination that renders [FullNowPlayingOverlay] when a track is loaded,
 * or a polished Material 3 [IdlePlayerView] when navigated to before playback starts.
 */
@Composable
fun PlayerScreen(
    playbackState: PlaybackState,
    availableMusicTracks: List<CloudTrack> = emptyList(),
    playerThemeStyle: PlayerThemeStyle = PlayerThemeStyle.AURORA_RING,
    visualEffect: PlayerVisualEffect = PlayerVisualEffect.VINYL_AURA,
    onSelectVisualEffect: (PlayerVisualEffect) -> Unit = {},
    onPlayAvailableTracks: (List<CloudTrack>, String) -> Unit = { _, _ -> },
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSwipePreviousTrack: () -> Unit = onSkipPrevious,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onEditTrack: (CloudTrack) -> Unit,
    onToggleFavorite: (CloudTrack) -> Unit = {},
    onAddToPlaylistRequest: (CloudTrack) -> Unit = {},
    onSelectQueueTrack: (CloudTrack) -> Unit = {},
    onSelectPreset: (EqualizerPreset) -> Unit,
    onBandChanged: (Int, Long) -> Unit,
    onBassChanged: (Long) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSyncEqToCloud: () -> Unit,
    onToggleEqEnabled: (Boolean) -> Unit = {},
    onResetEq: () -> Unit = {},
    onOpenFullEqualizer: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("player_view_screen")
    ) {
        if (playbackState.currentTrack != null) {
            FullNowPlayingOverlay(
                playbackState = playbackState,
                playerThemeStyle = playerThemeStyle,
                visualEffect = visualEffect,
                onSelectVisualEffect = onSelectVisualEffect,
                onCollapse = onCollapse,
                onTogglePlayPause = onTogglePlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onSwipePreviousTrack = onSwipePreviousTrack,
                onSeekTo = onSeekTo,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onEditTrack = onEditTrack,
                onToggleFavorite = onToggleFavorite,
                onAddToPlaylistRequest = onAddToPlaylistRequest,
                onSelectQueueTrack = onSelectQueueTrack,
                onSelectPreset = onSelectPreset,
                onBandChanged = onBandChanged,
                onBassChanged = onBassChanged,
                onSpeedChanged = onSpeedChanged,
                onSyncEqToCloud = onSyncEqToCloud,
                onToggleEqEnabled = onToggleEqEnabled,
                onResetEq = onResetEq,
                onOpenFullEqualizer = onOpenFullEqualizer
            )
        } else {
            IdlePlayerView(
                availableMusicTracks = availableMusicTracks,
                onPlayAvailableTracks = onPlayAvailableTracks,
                onNavigateToLibrary = onNavigateToLibrary,
                onNavigateToPlaylists = onNavigateToPlaylists,
                onCollapse = onCollapse,
                onOpenFullEqualizer = onOpenFullEqualizer
            )
        }
    }
}

@Composable
private fun IdlePlayerView(
    availableMusicTracks: List<CloudTrack>,
    onPlayAvailableTracks: (List<CloudTrack>, String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onCollapse: () -> Unit,
    onOpenFullEqualizer: (() -> Unit)?
) {
    val strings = LocalAppStrings.current
    val extColors = LocalNaghamColors.current
    val designTokens = LocalNaghamDesign.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    BackHandler(onBack = onCollapse)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("player_idle_view")
    ) {
        val isCompactHeight = maxHeight < 580.dp
        val outerMedallionSize = if (isCompactHeight) 96.dp else 208.dp
        val innerMedallionSize = if (isCompactHeight) 66.dp else 144.dp
        val albumIconSize = if (isCompactHeight) 32.dp else 64.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .widthIn(max = 560.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = if (isCompactHeight) 6.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("player_idle_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = strings.cancelAction,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = strings.nowPlayingTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (onOpenFullEqualizer != null) {
                    IconButton(
                        onClick = onOpenFullEqualizer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_idle_equalizer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = strings.navEqualizer,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // Medallion Vinyl Artwork Stage + Friendly Empty State Copy
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 8.dp else 16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(outerMedallionSize)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.28f),
                                    secondaryColor.copy(alpha = 0.14f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            )
                        )
                        .border(2.dp, extColors.cardBorder, CircleShape)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(innerMedallionSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                            .border(1.dp, primaryColor.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(albumIconSize)
                        )
                    }
                }

                Text(
                    text = strings.playerIdleTitle,
                    style = (if (isCompactHeight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall)
                        .copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = strings.playerIdleSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            // Transport Controls & Quick Route Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = designTokens.cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .naghamDesignBorder(shapeOverride = designTokens.cardShape)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Slider(
                            value = 0f,
                            onValueChange = {},
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("player_idle_progress_slider")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "0:00",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "0:00",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("player_idle_previous_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            FilledIconButton(
                                onClick = {
                                    val firstTrack = availableMusicTracks.firstOrNull()
                                    if (firstTrack != null) {
                                        onPlayAvailableTracks(availableMusicTracks, firstTrack.id)
                                    } else {
                                        onNavigateToLibrary()
                                    }
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = primaryColor,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .size(64.dp)
                                    .testTag("player_idle_play_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = strings.playerPlayFirstTrackBtn,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            IconButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("player_idle_next_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateToLibrary,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("player_go_to_library_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.playerBrowseLibraryBtn,
                            maxLines = 1
                        )
                    }

                    FilledTonalButton(
                        onClick = onNavigateToPlaylists,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("player_go_to_playlists_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.playerBrowsePlaylistsBtn,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
