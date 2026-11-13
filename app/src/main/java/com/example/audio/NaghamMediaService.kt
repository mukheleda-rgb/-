package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState as PlatformPlaybackState
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.CloudTrack
import com.example.ui.components.resolveNotificationCoverBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground MediaSession Service providing the native Android Media Player Widget
 * in the Notification Center / Quick Settings shade AND on the Lock Screen.
 */
class NaghamMediaService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaSession: MediaSession? = null
    private var stateObserverJob: Job? = null
    private var cachedTrackId: String? = null
    private var cachedCoverBitmap: Bitmap? = null
    private var isInForeground = false

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        val controller = AudioPlayerController.getInstance(applicationContext)

        try {
            val openAppIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val sessionActivityPi = PendingIntent.getActivity(
                this,
                100,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val session = MediaSession(this, "NaghamMediaSession").apply {
                setSessionActivity(sessionActivityPi)
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() {
                        controller.play()
                    }

                    override fun onPause() {
                        controller.pause()
                    }

                    override fun onSkipToNext() {
                        controller.skipNext()
                    }

                    override fun onSkipToPrevious() {
                        controller.skipToPreviousTrack()
                    }

                    override fun onSeekTo(pos: Long) {
                        controller.seekTo(pos)
                    }

                    override fun onStop() {
                        controller.stopAndClear()
                        stopForegroundAndSelf()
                    }
                })
                isActive = true
            }
            mediaSession = session
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to initialize MediaSession: ${t.message}")
        }

        stateObserverJob = serviceScope.launch {
            controller.state.collectLatest { st ->
                val track = st.currentTrack
                if (track == null) {
                    if (isInForeground) {
                        stopForegroundAndSelf()
                    }
                    return@collectLatest
                }
                val art = resolveArtwork(track)
                updateSessionAndNotification(track, st, art)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        val controller = AudioPlayerController.getInstance(applicationContext)

        when (intent?.action) {
            ACTION_PREV -> controller.skipToPreviousTrack()
            ACTION_TOGGLE -> controller.togglePlayPause()
            ACTION_NEXT -> controller.skipNext()
            ACTION_STOP -> {
                // Satisfy Android 12-16 5s startForeground contract even if stop arrives first
                if (!isInForeground) {
                    promoteToForeground(buildIdleFallbackNotification())
                }
                controller.stopAndClear()
                stopForegroundAndSelf()
                return START_NOT_STICKY
            }
        }

        // Always satisfy Android 12-16 startForeground contract on every onStartCommand
        val st = controller.state.value
        val track = st.currentTrack
        if (track != null) {
            if (!isInForeground) {
                val notification = buildMediaNotification(track, st, cachedCoverBitmap)
                promoteToForeground(notification)
            }
        } else {
            if (!isInForeground) {
                promoteToForeground(buildIdleFallbackNotification())
            }
            stopForegroundAndSelf()
        }
        return START_NOT_STICKY
    }

    private suspend fun resolveArtwork(track: CloudTrack): Bitmap? {
        if (cachedTrackId == track.id && cachedCoverBitmap != null) {
            return cachedCoverBitmap
        }
        return try {
            val bmp = resolveNotificationCoverBitmap(applicationContext, track)
            cachedTrackId = track.id
            cachedCoverBitmap = bmp
            bmp
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            null
        }
    }

    private fun updateSessionAndNotification(
        track: CloudTrack,
        st: PlaybackState,
        artBitmap: Bitmap?
    ) {
        try {
            mediaSession?.let { session ->
                val metadataBuilder = MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, track.id)
                    .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album.ifBlank { "Nagham Player" })
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, st.durationMs.coerceAtLeast(1000L))

                if (artBitmap != null && !artBitmap.isRecycled) {
                    metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artBitmap)
                    metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, artBitmap)
                    metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, artBitmap)
                }
                session.setMetadata(metadataBuilder.build())

                val platformState = when {
                    st.isBuffering -> PlatformPlaybackState.STATE_BUFFERING
                    st.isPlaying -> PlatformPlaybackState.STATE_PLAYING
                    else -> PlatformPlaybackState.STATE_PAUSED
                }

                val actions = PlatformPlaybackState.ACTION_PLAY or
                    PlatformPlaybackState.ACTION_PAUSE or
                    PlatformPlaybackState.ACTION_PLAY_PAUSE or
                    PlatformPlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlatformPlaybackState.ACTION_SKIP_TO_NEXT or
                    PlatformPlaybackState.ACTION_SEEK_TO or
                    PlatformPlaybackState.ACTION_STOP

                session.setPlaybackState(
                    PlatformPlaybackState.Builder()
                        .setActions(actions)
                        .setState(
                            platformState,
                            st.currentPositionMs.coerceAtLeast(0L),
                            if (st.isPlaying) st.playbackSpeed else 0f
                        )
                        .build()
                )
            }

            val notification = buildMediaNotification(track, st, artBitmap)
            if (st.isPlaying || st.isBuffering || !isInForeground) {
                promoteToForeground(notification)
            } else {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(NOTIFICATION_ID, notification)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed updating session/notification: ${t.message}")
        }
    }

    private fun promoteToForeground(notification: Notification) {
        try {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
            isInForeground = true
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to promote media service to foreground: ${t.message}")
            try {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(NOTIFICATION_ID, notification)
            } catch (_: Throwable) {
            }
        }
    }

    private fun buildIdleFallbackNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(R.drawable.ic_stat_music_note)
            .setContentTitle(getString(R.string.app_name))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .build()
    }

    private fun buildMediaNotification(
        track: CloudTrack,
        st: PlaybackState,
        artBitmap: Bitmap?
    ): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            this,
            101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevAction = Notification.Action.Builder(
            android.R.drawable.ic_media_previous,
            "Previous",
            serviceActionPendingIntent(ACTION_PREV, 201)
        ).build()

        val playPauseAction = Notification.Action.Builder(
            if (st.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (st.isPlaying) "Pause" else "Play",
            serviceActionPendingIntent(ACTION_TOGGLE, 202)
        ).build()

        val nextAction = Notification.Action.Builder(
            android.R.drawable.ic_media_next,
            "Next",
            serviceActionPendingIntent(ACTION_NEXT, 203)
        ).build()

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val style = Notification.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
        mediaSession?.sessionToken?.let { style.setMediaSession(it) }

        builder
            .setSmallIcon(R.drawable.ic_stat_music_note)
            .setContentTitle(track.title.ifBlank { getString(R.string.app_name) })
            .setContentText(track.artist)
            .setSubText(if (track.quranAudio) "القرآن الكريم" else track.album)
            .setContentIntent(contentPi)
            .setDeleteIntent(serviceActionPendingIntent(ACTION_STOP, 204))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(st.isPlaying)
            .setShowWhen(false)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(style)

        if (artBitmap != null && !artBitmap.isRecycled) {
            builder.setLargeIcon(artBitmap)
        }

        return builder.build()
    }

    private fun serviceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, NaghamMediaService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "Nagham Playback Controls",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Media playback widget in notification shade and lock screen"
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        setShowBadge(false)
                    }
                    nm.createNotificationChannel(channel)
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun stopForegroundAndSelf() {
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (_: Throwable) {
        }
        isInForeground = false
        try {
            stopSelf()
        } catch (_: Throwable) {
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stateObserverJob?.cancel()
        serviceScope.cancel()
        try {
            mediaSession?.isActive = false
            mediaSession?.release()
        } catch (_: Throwable) {
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "NaghamMediaService"
        private const val CHANNEL_ID = "nagham_playback_channel"
        private const val NOTIFICATION_ID = 4102

        const val ACTION_PREV = "com.example.audio.ACTION_PREV"
        const val ACTION_TOGGLE = "com.example.audio.ACTION_TOGGLE"
        const val ACTION_NEXT = "com.example.audio.ACTION_NEXT"
        const val ACTION_STOP = "com.example.audio.ACTION_STOP"

        fun ensureStartedForPlayback(context: Context) {
            try {
                val appCtx = context.applicationContext ?: context
                val intent = Intent(appCtx, NaghamMediaService::class.java)
                ContextCompat.startForegroundService(appCtx, intent)
            } catch (t: Throwable) {
                Log.w(TAG, "Skipped starting NaghamMediaService in current context: ${t.message}")
            }
        }

        fun stopServiceSafely(context: Context) {
            try {
                val appCtx = context.applicationContext ?: context
                appCtx.stopService(Intent(appCtx, NaghamMediaService::class.java))
            } catch (_: Throwable) {
            }
        }
    }
}
