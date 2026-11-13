package com.example.data.remote

import com.example.data.model.LyricLine
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class LrcLibDto(
    @param:Json(name = "id") val id: Long? = null,
    @param:Json(name = "trackName") val trackName: String? = null,
    @param:Json(name = "artistName") val artistName: String? = null,
    @param:Json(name = "albumName") val albumName: String? = null,
    @param:Json(name = "plainLyrics") val plainLyrics: String? = null,
    @param:Json(name = "syncedLyrics") val syncedLyrics: String? = null
)

interface LrcLibApi {
    @GET("api/search")
    suspend fun searchByTrackAndArtist(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String
    ): List<LrcLibDto>

    @GET("api/search")
    suspend fun searchByQuery(
        @Query("q") query: String
    ): List<LrcLibDto>
}

object LyricsService {

    private val api: LrcLibApi by lazy {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "NaghamPlayer/1.0 (Android)")
                    .build()
                chain.proceed(req)
            }
            .build()

        Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LrcLibApi::class.java)
    }

    suspend fun downloadLyrics(title: String, artist: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = title.trim()
            val cleanArtist = artist.trim()
            val directMatches = if (cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                api.searchByTrackAndArtist(cleanTitle, cleanArtist)
            } else {
                emptyList()
            }

            val candidates = directMatches.ifEmpty {
                api.searchByQuery("$cleanTitle $cleanArtist".trim())
            }

            val best = candidates.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
                ?: candidates.firstOrNull { !it.plainLyrics.isNullOrBlank() }

            val text = best?.syncedLyrics?.takeIf { it.isNotBlank() }
                ?: best?.plainLyrics?.takeIf { it.isNotBlank() }

            if (!text.isNullOrBlank()) {
                Result.success(text.take(14500))
            } else {
                Result.failure(NoSuchElementException("No matching lyrics found on LRCLIB for '$cleanTitle'"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private val lrcPattern = Regex("""^\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?]\s*(.*)$""")

    fun parseLyrics(rawLyrics: String): List<LyricLine> {
        if (rawLyrics.isBlank()) return emptyList()
        return rawLyrics.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                val match = lrcPattern.find(line)
                if (match != null) {
                    val min = match.groupValues[1].toLongOrNull() ?: 0L
                    val sec = match.groupValues[2].toLongOrNull() ?: 0L
                    val fracStr = match.groupValues[3]
                    val ms = when (fracStr.length) {
                        1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                        2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                        3 -> fracStr.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                    val totalMs = min * 60_000L + sec * 1_000L + ms
                    val lyricText = match.groupValues[4].ifBlank { "♪" }
                    LyricLine(timestampMs = totalMs, text = lyricText)
                } else {
                    LyricLine(timestampMs = null, text = line)
                }
            }
    }
}
