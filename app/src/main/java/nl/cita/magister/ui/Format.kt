package nl.cita.magister.ui

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import nl.cita.magister.grades.GradeCalculator

private val zone = ZoneId.of("Europe/Amsterdam")
private val nl = Locale("nl", "NL")
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", nl)
private val dayFmt = DateTimeFormatter.ofPattern("EEEE d MMMM", nl)
private val shortDay = DateTimeFormatter.ofPattern("EEE d MMM", nl)

fun formatGrade(value: Double?, label: String? = null): String =
    GradeCalculator.formatGradeValue(value, label)

/** Join class, study, school — skip blanks so the line never ends in `·`. */
fun profileLine(vararg parts: String?): String =
    parts.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.joinToString(" · ")

fun formatWhen(iso: String?): String {
    val instant = parseInstant(iso) ?: return iso.orEmpty()
    return DateTimeFormatter.ofPattern("d MMM, HH:mm", nl).withZone(zone).format(instant)
}

fun formatTime(iso: String?): String {
    val instant = parseInstant(iso) ?: return ""
    return timeFmt.withZone(zone).format(instant)
}

fun formatDay(iso: String?): String {
    val instant = parseInstant(iso) ?: return iso.orEmpty()
    return dayFmt.withZone(zone).format(instant)
}

fun formatDayShort(date: LocalDate): String = date.format(shortDay)

fun localDateOf(iso: String?): LocalDate? {
    val instant = parseInstant(iso) ?: return iso?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    return instant.atZone(zone).toLocalDate()
}

fun parseInstant(iso: String?): Instant? {
    if (iso.isNullOrBlank()) return null
    return runCatching { Instant.parse(iso) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()
        ?: runCatching { LocalDate.parse(iso.take(10)).atStartOfDay(zone).toInstant() }.getOrNull()
}
