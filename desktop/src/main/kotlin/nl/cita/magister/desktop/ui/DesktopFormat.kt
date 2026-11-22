package nl.cita.magister.desktop.ui

import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Grade
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.InfoType
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object DesktopFormat {
    val zone: ZoneId = ZoneId.of("Europe/Amsterdam")
    val nl: Locale = Locale.forLanguageTag("nl-NL")

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", nl)
    private val dayFmt = DateTimeFormatter.ofPattern("EEEE d MMMM", nl)
    private val shortDayFmt = DateTimeFormatter.ofPattern("EEE d MMM", nl)
    private val dayMonthFmt = DateTimeFormatter.ofPattern("d MMM", nl)
    private val whenFmt = DateTimeFormatter.ofPattern("d MMM, HH:mm", nl)
    private val dayHeaderFmt = DateTimeFormatter.ofPattern("EEE", nl)

    fun formatGrade(value: Double?, label: String? = null): String =
        GradeCalculator.formatGradeValue(value, label)

    fun profileLine(vararg parts: String?): String =
        parts.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.joinToString(" · ")

    fun parseInstant(iso: String?): Instant? {
        if (iso.isNullOrBlank()) return null
        return runCatching { Instant.parse(iso) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()
            ?: runCatching { LocalDate.parse(iso.take(10)).atStartOfDay(zone).toInstant() }.getOrNull()
    }

    fun localDateOf(iso: String?): LocalDate? {
        val instant = parseInstant(iso)
            ?: return iso?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return instant.atZone(zone).toLocalDate()
    }

    fun zonedDateTimeOf(iso: String?): ZonedDateTime? =
        parseInstant(iso)?.atZone(zone)

    fun formatTime(iso: String?): String =
        parseInstant(iso)?.let { timeFmt.withZone(zone).format(it) }.orEmpty()

    fun formatWhen(iso: String?): String =
        parseInstant(iso)?.let { whenFmt.withZone(zone).format(it) } ?: iso.orEmpty()

    fun formatDay(date: LocalDate): String =
        date.format(dayFmt).replaceFirstChar { it.titlecase(nl) }

    fun formatDayShort(date: LocalDate): String = date.format(shortDayFmt)

    fun formatDayOfWeekShort(date: LocalDate): String =
        date.format(dayHeaderFmt).replace(".", "").replaceFirstChar { it.titlecase(nl) }

    fun formatDayMonth(date: LocalDate): String = date.format(dayMonthFmt)

    fun minutesUntil(iso: String?, nowMillis: Long): Long? {
        val target = parseInstant(iso) ?: return null
        return Duration.between(Instant.ofEpochMilli(nowMillis), target).toMinutes()
    }

    fun heroTimingLabel(
        current: Boolean,
        startIso: String?,
        endIso: String?,
        nowMillis: Long,
    ): String {
        if (current) {
            val end = formatTime(endIso)
            val left = minutesUntil(endIso, nowMillis)
            return when {
                left == null && end.isBlank() -> ""
                left == null || left <= 0 -> if (end.isBlank()) "" else "tot $end"
                end.isBlank() -> "nog $left min"
                else -> "tot $end · nog $left min"
            }
        }
        val mins = minutesUntil(startIso, nowMillis) ?: return ""
        return when {
            mins <= 0 -> "nu"
            mins < 60 -> "over $mins min"
            else -> {
                val h = mins / 60
                val m = mins % 60
                if (m == 0L) "over $h uur" else "over ${h}u ${m}m"
            }
        }
    }

    fun latestGradeCaption(grade: Grade): String =
        listOfNotNull(
            grade.columnHeading?.takeIf { it.isNotBlank() }
                ?: grade.columnDescription?.takeIf { it.isNotBlank() },
            GradeCalculator.formatWeight(grade.weight),
        ).joinToString(" · ")

    fun homeworkBody(item: HomeworkItem): String {
        val description = item.description?.trim().orEmpty()
        if (description.isNotBlank() && !description.equals(item.subject, ignoreCase = true)) {
            return description
        }
        val title = item.title.trim()
        if (title.isNotBlank() && !title.equals(item.subject, ignoreCase = true)) {
            return title
        }
        return when (item.infoType) {
            InfoType.Test -> "Proefwerk"
            InfoType.Written -> "SO / PO"
            InfoType.Oral -> "Mondeling"
            InfoType.Homework -> "Huiswerk"
            else -> "Taak"
        }
    }

    fun isToets(type: InfoType): Boolean = when (type) {
        InfoType.Test, InfoType.Written, InfoType.Oral -> true
        else -> false
    }

    fun homeworkDayLabel(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Vandaag"
        today.plusDays(1) -> "Morgen"
        today.minusDays(1) -> "Gisteren"
        else -> date.format(shortDayFmt).replaceFirstChar { it.titlecase(nl) }
    }

    fun dueChipLabel(dueIso: String, today: LocalDate): String {
        val date = localDateOf(dueIso)
        val time = formatTime(dueIso)
        val day = when {
            date == null -> ""
            date == today -> "vandaag"
            date == today.plusDays(1) -> "morgen"
            date == today.minusDays(1) -> "gisteren"
            else -> date.format(dayMonthFmt)
        }
        return listOf(day, time).filter { it.isNotBlank() }.joinToString(" · ")
    }

    fun stripHtml(html: String?): String {
        if (html.isNullOrBlank()) return ""
        return html
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n\n")
            .replace(Regex("(?i)</div>"), "\n")
            .replace(Regex("(?i)<li[^>]*>"), "• ")
            .replace(Regex("(?i)</li>"), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    fun mondayOf(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekVisibleDays(monday: LocalDate, appointments: List<Appointment>): List<LocalDate> {
        val weekendWithLessons = appointments
            .mapNotNull { localDateOf(it.startIso) }
            .filter { it.dayOfWeek == DayOfWeek.SATURDAY || it.dayOfWeek == DayOfWeek.SUNDAY }
            .toSet()
        val days = (0L..4L).map { monday.plusDays(it) }.toMutableList()
        val sat = monday.plusDays(5)
        val sun = monday.plusDays(6)
        if (sat in weekendWithLessons) days += sat
        if (sun in weekendWithLessons) days += sun
        return days
    }

    fun buildDaySlots(lessons: List<Appointment>, minBreakMinutes: Long = 5L): List<DesktopDaySlot> {
        val sorted = lessons.sortedBy { it.startIso }
        if (sorted.isEmpty()) return emptyList()
        val slots = mutableListOf<DesktopDaySlot>()
        sorted.forEachIndexed { index, item ->
            if (index > 0) {
                gapBetween(sorted[index - 1], item, minBreakMinutes)?.let { slots += it }
            }
            slots += DesktopDaySlot.Lesson(item)
        }
        return slots
    }

    private fun gapBetween(
        previous: Appointment,
        next: Appointment,
        minBreakMinutes: Long,
    ): DesktopDaySlot.Gap? {
        val prevEnd = previous.periodTo ?: previous.periodFrom
        val nextStart = next.periodFrom
        if (prevEnd != null && nextStart != null && nextStart > prevEnd + 1) {
            return DesktopDaySlot.Gap(
                fromIso = previous.endIso,
                toIso = next.startIso,
                isFreePeriod = true,
                missingFrom = prevEnd + 1,
                missingTo = nextStart - 1,
            )
        }
        val from = zonedDateTimeOf(previous.endIso) ?: return null
        val to = zonedDateTimeOf(next.startIso) ?: return null
        val mins = Duration.between(from, to).toMinutes()
        if (mins >= minBreakMinutes) {
            return DesktopDaySlot.Gap(
                fromIso = previous.endIso,
                toIso = next.startIso,
                isFreePeriod = false,
            )
        }
        return null
    }
}

sealed class DesktopDaySlot {
    data class Lesson(val item: Appointment) : DesktopDaySlot()
    data class Gap(
        val fromIso: String,
        val toIso: String,
        val isFreePeriod: Boolean,
        val missingFrom: Int? = null,
        val missingTo: Int? = null,
    ) : DesktopDaySlot()
}

enum class HwFilter(val label: String) {
    Open("Open"),
    Done("Af"),
    All("Alles"),
}

sealed class HomeworkBucket {
    data class Overdue(val items: List<HomeworkItem>) : HomeworkBucket()
    data class Day(val date: LocalDate, val label: String, val items: List<HomeworkItem>) : HomeworkBucket()
}

fun groupHomework(
    items: List<HomeworkItem>,
    today: LocalDate,
    filter: HwFilter,
): List<HomeworkBucket> {
    val filtered = when (filter) {
        HwFilter.Open -> items.filter { !it.isDone }
        HwFilter.Done -> items.filter { it.isDone }
        HwFilter.All -> items
    }
    val overdue = if (filter == HwFilter.Done) {
        emptyList()
    } else {
        filtered.filter { item ->
            !item.isDone && (DesktopFormat.localDateOf(item.dueIso)?.isBefore(today) == true)
        }.sortedBy { it.dueIso }
    }
    val rest = filtered.filterNot { item -> overdue.any { it.id == item.id } }
        .groupBy { DesktopFormat.localDateOf(it.dueIso) ?: today }
        .toSortedMap()
        .map { (date, dayItems) ->
            HomeworkBucket.Day(date, DesktopFormat.homeworkDayLabel(date, today), dayItems.sortedBy { it.dueIso })
        }
    return buildList {
        if (overdue.isNotEmpty()) add(HomeworkBucket.Overdue(overdue))
        addAll(rest)
    }
}
