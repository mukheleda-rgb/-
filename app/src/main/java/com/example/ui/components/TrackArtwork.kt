package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CloudTrack
import com.example.data.scanner.DeviceAudioScanner
import com.example.ui.theme.LocalNaghamColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_ARTWORK_PX = 480
private const val LIST_THUMB_ARTWORK_PX = 168

private object ArtworkBitmapCache {
    // Byte-bounded cache (max 10 MB) so high-res embedded album covers never cause OutOfMemoryError
    private val androidCache = object : LruCache<String, Bitmap>(10 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.allocationByteCount / 1024).coerceAtLeast(1)
        }
    }
    private val composeCache = object : LruCache<String, ImageBitmap>(96) {}
    private val negativeKeys = object : LinkedHashMap<String, Boolean>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
            return size > 512
        }
    }

    fun getCompose(key: String): ImageBitmap? = synchronized(this) { composeCache.get(key) }
    fun getAndroid(key: String): Bitmap? = synchronized(this) { androidCache.get(key) }
    fun isKnownMissing(key: String): Boolean = synchronized(this) { negativeKeys.containsKey(key) }

    fun put(key: String, bitmap: Bitmap?) = synchronized(this) {
        if (bitmap != null && !bitmap.isRecycled) {
            androidCache.put(key, bitmap)
            composeCache.put(key, bitmap.asImageBitmap())
        } else {
            negativeKeys[key] = true
        }
    }
}

private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
    var inSampleSize = 1
    if (height > maxDim || width > maxDim) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= maxDim && (halfWidth / inSampleSize) >= maxDim) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}

private fun decodeSampledBytes(bytes: ByteArray, maxDim: Int = MAX_ARTWORK_PX): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val opts = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
}

private fun decodeSampledUri(context: Context, uri: Uri, maxDim: Int = MAX_ARTWORK_PX): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val opts = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, opts)
    }
}

private suspend fun decodeRawTrackBitmap(
    context: Context,
    sourceUri: String,
    albumArtUri: String,
    maxDim: Int = MAX_ARTWORK_PX
): Bitmap? = withContext(Dispatchers.IO) {
    val cacheKey = "$sourceUri|$albumArtUri|$maxDim"
    ArtworkBitmapCache.getAndroid(cacheKey)?.let { cached ->
        if (!cached.isRecycled) return@withContext cached
    }
    if (ArtworkBitmapCache.isKnownMissing(cacheKey)) return@withContext null

    var decodedBitmap: Bitmap? = null

    // 1. Try embedded ID3 / MP4 cover picture directly from audio file (bounds-checked)
    if (sourceUri.startsWith("content://") || sourceUri.startsWith("file://")) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(sourceUri))
            val embeddedBytes = retriever.embeddedPicture
            if (embeddedBytes != null && embeddedBytes.isNotEmpty()) {
                decodedBitmap = decodeSampledBytes(embeddedBytes, maxDim)
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
        }
    }

    // 2. Try MediaStore albumArtUri or Folder cover image URI (bounds-checked)
    if (decodedBitmap == null && albumArtUri.isNotBlank()) {
        try {
            val artUri = Uri.parse(albumArtUri)
            decodedBitmap = decodeSampledUri(context, artUri, maxDim)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
        }
    }

    // 3. Try Android 10+ ContentResolver thumbnail loader
    if (decodedBitmap == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && sourceUri.startsWith("content://")) {
        try {
            decodedBitmap = context.contentResolver.loadThumbnail(
                Uri.parse(sourceUri),
                Size(maxDim, maxDim),
                null
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
        }
    }

    ArtworkBitmapCache.put(cacheKey, decodedBitmap)
    decodedBitmap
}

/**
 * Resolves a rich Bitmap for Android MediaStyle Notification Center & Lock Screen widget.
 * Uses the real embedded/album/folder cover if present, or renders a stylized folder cover Bitmap.
 */
suspend fun resolveNotificationCoverBitmap(
    context: Context,
    track: CloudTrack
): Bitmap = withContext(Dispatchers.IO) {
    val localMeta = DeviceAudioScanner.resolveMetaForTrack(context, track)
    val raw = decodeRawTrackBitmap(context, track.sourceUri, localMeta.albumArtUri, MAX_ARTWORK_PX)
    if (raw != null && !raw.isRecycled) return@withContext raw

    // Render stylized Folder/Album Cover Bitmap for Notification & Lock Screen
    val sizePx = 320
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val primaryInt = try {
        if (track.quranAudio) android.graphics.Color.parseColor("#38B28B")
        else android.graphics.Color.parseColor(track.accentHex.ifBlank { "#E8A855" })
    } catch (_: Throwable) {
        android.graphics.Color.parseColor("#E8A855")
    }
    val darkBg = android.graphics.Color.parseColor("#141820")

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
            intArrayOf(primaryInt, darkBg),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(65, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx * 0.28f, ringPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 32f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val label = localMeta.folderName.ifBlank { track.album.ifBlank { "Nagham" } }.take(18)
    canvas.drawText(label, sizePx / 2f, sizePx * 0.54f, textPaint)
    bmp
}

@Composable
fun TrackArtwork(
    track: CloudTrack,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    showFolderBadge: Boolean = false
) {
    val context = LocalContext.current
    val extColors = LocalNaghamColors.current
    val localMeta = remember(track.sourceUri, track.album) {
        DeviceAudioScanner.resolveMetaForTrack(context, track)
    }

    val targetMaxDim = if (showFolderBadge) MAX_ARTWORK_PX else LIST_THUMB_ARTWORK_PX
    val cacheKey = "${track.sourceUri}|${localMeta.albumArtUri}|$targetMaxDim"
    val coverBitmap by produceState<ImageBitmap?>(
        initialValue = ArtworkBitmapCache.getCompose(cacheKey),
        key1 = cacheKey
    ) {
        if (value != null || ArtworkBitmapCache.isKnownMissing(cacheKey)) {
            return@produceState
        }
        val raw = decodeRawTrackBitmap(context, track.sourceUri, localMeta.albumArtUri, targetMaxDim)
        value = ArtworkBitmapCache.getCompose(cacheKey)
            ?: if (raw != null && !raw.isRecycled) raw.asImageBitmap() else null
    }

    val baseAccent = if (track.quranAudio) {
        extColors.quranAccent
    } else {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        baseAccent.copy(alpha = 0.32f),
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        baseAccent.copy(alpha = 0.14f)
                    )
                )
            )
            .border(1.dp, baseAccent.copy(alpha = 0.28f), shape),
        contentAlignment = Alignment.Center
    ) {
        val bmp = coverBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (showFolderBadge && localMeta.folderName.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = baseAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = track.album.ifBlank { localMeta.folderName },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            // Stylized Folder Cover Representation (صورة الحافظة) when audio file has no embedded cover
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (showFolderBadge) 68.dp else 32.dp)
                        .clip(CircleShape)
                        .background(baseAccent.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (track.quranAudio) {
                            Icons.AutoMirrored.Filled.MenuBook
                        } else {
                            Icons.Default.FolderSpecial
                        },
                        contentDescription = localMeta.folderName,
                        tint = baseAccent,
                        modifier = Modifier.size(if (showFolderBadge) 36.dp else 18.dp)
                    )
                }

                if (showFolderBadge) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = baseAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = localMeta.folderName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
