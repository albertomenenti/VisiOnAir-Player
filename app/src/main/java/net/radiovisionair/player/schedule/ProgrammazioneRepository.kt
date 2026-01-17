package net.radiovisionair.player.schedule

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.text.Normalizer
import java.time.*
import java.time.format.DateTimeFormatter

class ProgrammazioneRepository {

    private data class Slot(
        val day: DayOfWeek,
        val start: LocalTime,
        val title: String,
        val description: String
    )

    private data class Cached(
        val fetchedAt: Instant,
        val schedule: Map<DayOfWeek, List<Slot>>
    )

    private var cache: Cached? = null

    private val zone: ZoneId = ZoneId.of("Europe/Rome")
    private val programmazioneUrl = "https://radiovisionair.net/programmazione/"

    suspend fun nowPlaying(): ShowInfo = withContext(Dispatchers.IO) {
        val now = ZonedDateTime.now(zone)
        val schedule = getScheduleSafe()

        val daySlots = schedule[now.dayOfWeek].orEmpty().sortedBy { it.start }
        if (daySlots.isEmpty()) {
            return@withContext ShowInfo(
                title = "In diretta",
                description = "",
                source = "Programmazione",
                validUntil = null
            )
        }

        val nowTime = now.toLocalTime()
        val idx = daySlots.indexOfLast { it.start <= nowTime }
        val current = if (idx >= 0) daySlots[idx] else daySlots.first()

        val nextStart = daySlots.getOrNull((idx.coerceAtLeast(0)) + 1)?.start
        val validUntil = if (nextStart != null) {
            ZonedDateTime.of(now.toLocalDate(), nextStart, zone).toInstant()
        } else {
            // End of day: refresh at next midnight.
            now.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()
        }

        ShowInfo(
            title = current.title.ifBlank { "In diretta" },
            description = current.description.ifBlank { "" },
            source = "Programmazione (${dayName(now.dayOfWeek)})",
            validUntil = validUntil
        )
    }

    private suspend fun getScheduleSafe(): Map<DayOfWeek, List<Slot>> {
        val now = Instant.now()
        val cached = cache

        // Cache 6h (riduce richieste e mantiene app leggera).
        if (cached != null && Duration.between(cached.fetchedAt, now) < Duration.ofHours(6)) {
            return cached.schedule
        }

        return try {
            val schedule = fetchAndParseProgrammazione()
            cache = Cached(now, schedule)
            schedule
        } catch (e: Exception) {
            // fallback: usa cache se c'e'
            cached?.schedule ?: emptyMap()
        }
    }

    private fun fetchAndParseProgrammazione(): Map<DayOfWeek, List<Slot>> {
        val doc = Jsoup.connect(programmazioneUrl)
            .userAgent("Mozilla/5.0 (Android) VisionairRadioPlayer/1.0")
            .timeout(15_000)
            .get()

        val content = doc.selectFirst("main, article, .entry-content") ?: doc.body()

        val elements = content.select("h2, h3, p")
        var currentDay: DayOfWeek? = null
        var currentTitle: String? = null
        val desc = StringBuilder()

        val all = mutableListOf<Slot>()

        for (el in elements) {
            val t = el.text().trim()
            if (t.isBlank()) continue

            when (el.tagName()) {
                "h2" -> {
                    if (isTime(t)) {
                        val day = currentDay
                        val title = currentTitle
                        if (day != null && title != null) {
                            val start = parseTime(t)
                            val description = desc.toString().trim()
                            all.add(Slot(day, start, title, description))
                        }
                    } else {
                        // day header
                        parseDay(t)?.let { day ->
                            currentDay = day
                            currentTitle = null
                            desc.setLength(0)
                        }
                    }
                }
                "h3" -> {
                    if (currentDay != null) {
                        currentTitle = t
                        desc.setLength(0)
                    }
                }
                "p" -> {
                    if (currentTitle != null) {
                        val cleaned = t
                            .replace("\u00A0", " ")
                            .trim()
                        if (cleaned.isNotBlank() && cleaned != "[...]" && cleaned.lowercase() != "discover more") {
                            if (desc.isNotEmpty()) desc.append('\n')
                            desc.append(cleaned)
                        }
                    }
                }
            }
        }

        return all
            .groupBy { it.day }
            .mapValues { (_, v) -> v.sortedBy { it.start } }
    }

    private fun isTime(text: String): Boolean {
        val t = text.trim()
        return Regex("^\\d{2}:\\d{2}$").matches(t)
    }

    private fun parseTime(text: String): LocalTime {
        val t = text.trim()
        if (t == "24:00") return LocalTime.MIDNIGHT
        return LocalTime.parse(t, DateTimeFormatter.ofPattern("HH:mm"))
    }

    private fun parseDay(text: String): DayOfWeek? {
        val n = normalize(text)
        return when (n) {
            "lunedi" -> DayOfWeek.MONDAY
            "martedi" -> DayOfWeek.TUESDAY
            "mercoledi" -> DayOfWeek.WEDNESDAY
            "giovedi" -> DayOfWeek.THURSDAY
            "venerdi" -> DayOfWeek.FRIDAY
            "sabato" -> DayOfWeek.SATURDAY
            "domenica" -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    private fun dayName(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "Lunedì"
        DayOfWeek.TUESDAY -> "Martedì"
        DayOfWeek.WEDNESDAY -> "Mercoledì"
        DayOfWeek.THURSDAY -> "Giovedì"
        DayOfWeek.FRIDAY -> "Venerdì"
        DayOfWeek.SATURDAY -> "Sabato"
        DayOfWeek.SUNDAY -> "Domenica"
    }

    private fun normalize(s: String): String {
        val lower = s.trim().lowercase()
        val noAccent = Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return noAccent.replace("[^a-z]".toRegex(), "")
    }
}
