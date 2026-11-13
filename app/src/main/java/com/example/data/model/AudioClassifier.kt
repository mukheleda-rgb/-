package com.example.data.model

object AudioClassifier {

    // Unambiguous multi-character / phrase Quran signals
    private val quranPhraseSignals = listOf(
        "قرآن", "قران", "سورة", "سوره", "تلاوة", "تلاوه", "مصحف", "تجويد", "ترتيل",
        "الرقية", "الرقيه", "ختمة", "آية الكرسي", "اية الكرسي", "الشيخ", "القارئ",
        "الفاتحة", "البقرة", "آل عمران", "المائدة", "الأنعام", "الأعراف", "الأنفال",
        "الكهف", "الأنبياء", "المؤمنون", "الفرقان", "العنكبوت", "الأحزاب", "الصافات",
        "فصلت", "الجاثية", "الأحقاف", "الحجرات", "الذاريات", "الرحمن", "الواقعة",
        "المجادلة", "الممتحنة", "المنافقون", "التغابن", "الحاقة", "المعارج",
        "المزمل", "المدثر", "القيامة", "المرسلات", "النازعات", "التكوير", "الانفطار",
        "المطففين", "الانشقاق", "الغاشية", "الزلزلة", "العاديات", "القارعة", "التكاثر",
        "الكوثر", "الكافرون", "الإخلاص", "الفلق",
        "العفاسي", "السديس", "الشريم", "المعيقلي", "المنشاوي", "الحصري", "عبدالباسط",
        "عبد الباسط", "ياسر الدوسري", "ناصر القطامي", "إسلام صبحي", "اسلام صبحي",
        "هزاع البلوشي", "فارس عباد", "أبو بكر الشاطري", "أحمد العجمي", "سعد الغامدي",
        "محمود علي البنا", "الطبلاوي",
        "quran", "quraan", "koran", "surah", "surat", "tilawah", "tilawa", "recitation",
        "mushaf", "tajweed", "ruqyah", "al-fatihah", "al-baqarah", "al-kahf",
        "ar-rahman", "yaseen", "al-mulk", "al-ikhlas", "alafasy", "sudais",
        "shuraim", "muaiqly", "minshawi", "husary", "abdulbasit", "mp3quran", "everyayah"
    )

    private val quranExactTokens = setOf(
        "ayah", "afasy", "dosari", "ghamdi", "ajmi"
    )

    private val arabicTarabSignals = listOf(
        "طرب", "عود", "مقام", "حجاز", "بياتي", "نهاوند", "راست", "شرقي", "موشح",
        "فيروز", "أم كلثوم", "ام كلثوم", "عبد الحليم", "وردة", "كاظم", "ماجدة", "عبادي",
        "محمد عبده", "oud", "maqam", "tarab", "oriental", "arabic", "hijaz", "fairouz"
    )

    private val chillSignals = listOf(
        "هادئ", "هدوء", "ليل", "سهر", "مطر", "استرخاء", "نوم", "تأمل",
        "chill", "lofi", "lo-fi", "ambient", "night", "relax", "calm", "sleep", "acoustic"
    )

    private val classicalSignals = listOf(
        "كلاسيك", "بيانو", "كمان", "أوركسترا", "سمفونية", "معزوفة",
        "classical", "piano", "violin", "orchestra", "symphony", "sonata", "cello", "instrumental"
    )

    private val energeticSignals = listOf(
        "حماس", "رياضة", "إيقاع", "رقص", "بوب", "روك",
        "workout", "gym", "energetic", "dance", "edm", "electronic", "synth", "pop", "rock", "beat"
    )

    private val podcastSignals = listOf(
        "بودكاست", "حلقة", "حوار", "محاضرة", "كتاب صوتي",
        "podcast", "episode", "talk", "audiobook", "interview"
    )

    private val tokenSplitter = Regex("[^\\p{L}\\p{N}]+")

    data class ClassificationResult(
        val category: TrackCategory,
        val isQuran: Boolean,
        val suggestedAccentHex: String
    )

    fun classify(
        title: String,
        artist: String = "",
        album: String = "",
        genre: String = "",
        uriOrPath: String = "",
        autoIsolateQuran: Boolean = true
    ): ClassificationResult {
        val combined = "$title $artist $album $genre $uriOrPath".lowercase()
        val tokens = combined.split(tokenSplitter).filter { it.isNotEmpty() }.toSet()

        val matchesQuran = quranPhraseSignals.any { signal -> combined.contains(signal.lowercase()) } ||
            tokens.any { it in quranExactTokens }

        if (autoIsolateQuran && matchesQuran) {
            return ClassificationResult(
                category = TrackCategory.QURAN,
                isQuran = true,
                suggestedAccentHex = "#38B28B"
            )
        }

        return when {
            podcastSignals.any { combined.contains(it) } -> ClassificationResult(
                category = TrackCategory.PODCAST,
                isQuran = false,
                suggestedAccentHex = "#6C9BD2"
            )
            arabicTarabSignals.any { combined.contains(it) } -> ClassificationResult(
                category = TrackCategory.ARABIC_TARAB,
                isQuran = false,
                suggestedAccentHex = "#E8A855"
            )
            classicalSignals.any { combined.contains(it) } -> ClassificationResult(
                category = TrackCategory.CLASSICAL,
                isQuran = false,
                suggestedAccentHex = "#C9966B"
            )
            chillSignals.any { combined.contains(it) } -> ClassificationResult(
                category = TrackCategory.CHILL_NIGHT,
                isQuran = false,
                suggestedAccentHex = "#7E8CE0"
            )
            energeticSignals.any { combined.contains(it) } -> ClassificationResult(
                category = TrackCategory.ENERGETIC,
                isQuran = false,
                suggestedAccentHex = "#E06D53"
            )
            else -> ClassificationResult(
                category = TrackCategory.OTHER_MUSIC,
                isQuran = false,
                suggestedAccentHex = "#D4A359"
            )
        }
    }
}
