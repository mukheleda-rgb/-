package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TrackCategory(
    val id: String,
    val labelAr: String,
    val labelEn: String
) {
    ARABIC_TARAB("ARABIC_TARAB", "طرب وموسيقى عربية", "Arabic & Tarab"),
    CHILL_NIGHT("CHILL_NIGHT", "استرخاء وليلي", "Chill & Night"),
    CLASSICAL("CLASSICAL", "كلاسيك ومعزوفات", "Classical & Instrumental"),
    ENERGETIC("ENERGETIC", "إيقاع وحماس", "Energetic & Rhythm"),
    PODCAST("PODCAST", "بودكاست وصوتيات", "Podcast & Spoken"),
    OTHER_MUSIC("OTHER_MUSIC", "متنوعات موسيقية", "General Music"),
    QURAN("QURAN", "القرآن الكريم (معزول)", "Holy Quran (Isolated)");

    companion object {
        fun fromId(id: String): TrackCategory =
            entries.firstOrNull { it.id == id } ?: OTHER_MUSIC
    }
}

enum class EqualizerPreset(
    val id: String,
    val labelAr: String,
    val labelEn: String,
    val bandsMb: List<Long>,
    val defaultBass: Long
) {
    FLAT("FLAT", "متوازن (قياسي)", "Flat Studio", listOf(0L, 0L, 0L, 0L, 0L), 0L),
    WARM_OUD("WARM_OUD", "عود وطرب دافئ", "Warm Oud & Tarab", listOf(250L, 350L, 150L, 200L, 100L), 280L),
    BASS_BOOST("BASS_BOOST", "تعزيز الإيقاع (Bass)", "Deep Bass", listOf(750L, 500L, 0L, 150L, 250L), 650L),
    VOCAL_CLEAR("VOCAL_CLEAR", "نقاء الصوت والتلاوة", "Pure Vocal & Clarity", listOf(-150L, 100L, 450L, 500L, 200L), 0L),
    NIGHT_COMFORT("NIGHT_COMFORT", "استماع ليلي مريح", "Night Comfort", listOf(200L, 150L, 50L, -200L, -350L), 200L),
    CLASSICAL("CLASSICAL", "أوركسترا وكلاسيك", "Orchestral", listOf(350L, 150L, -100L, 250L, 400L), 180L),
    CUSTOM("CUSTOM", "تخصيص يدوي", "Custom Tuning", listOf(0L, 0L, 0L, 0L, 0L), 0L);

    companion object {
        fun fromId(id: String): EqualizerPreset =
            entries.firstOrNull { it.id == id } ?: WARM_OUD
    }
}

@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "مستمع نغم",
    val languageCode: String = "ar",
    val darkTheme: Boolean = true,
    val eyeComfortMode: Boolean = true,
    val autoIsolateQuran: Boolean = true,
    val eqPreset: String = EqualizerPreset.WARM_OUD.id,
    val eqBands: List<Long> = EqualizerPreset.WARM_OUD.bandsMb,
    val bassBoostStrength: Long = EqualizerPreset.WARM_OUD.defaultBass,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudTrack(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val genre: String = "",
    val category: String = TrackCategory.OTHER_MUSIC.id,
    val quranAudio: Boolean = false,
    val durationMs: Long = 0L,
    val sourceType: String = "LOCAL_URI",
    val sourceUri: String = "",
    val lyrics: String = "",
    val favorite: Boolean = false,
    val playCount: Long = 0L,
    val accentHex: String = "#E8A855",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudPlaylist(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val trackIds: List<String> = emptyList(),
    val sourceApp: String = "Nagham",
    val quranPlaylist: Boolean = false,
    val coverHex: String = "#E8A855",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class LyricLine(
    val timestampMs: Long?,
    val text: String
)
