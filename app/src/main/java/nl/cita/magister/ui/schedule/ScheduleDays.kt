package nl.cita.magister.ui.schedule

import nl.cita.magister.model.Appointment
import nl.cita.magister.ui.localDateOf
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

private val zone = ZoneId.of("Europe/Amsterdam")
private const val MinBreakMinutes = 5L

internal enum class GapKind { FreePeriod, Break }

internal sealed class DaySlot {
    data class Lesson(val item: Appointment) : DaySlot()
    data class Gap(
        val fromIso: String,
        val toIso: String,
        val kind: GapKind,
        val missingFrom: Int? = null,
        val missingTo: Int? = null,
    ) : DaySlot()
}

internal fun Appointment.periodStart(): Int? = periodFrom

internal fun Appointment.periodEnd(): Int? = periodTo ?: periodFrom

internal fun isWeekend(date: LocalDate): Boolean =
    date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

internal fun weekendDatesWithLessons(appointments: List<Appointment>): Set<LocalDate> =
    appointments.mapNotNull { localDateOf(it.startIso) }
        .filter(::isWeekend)
        .toSet()

/** Zaterdag/zondag zonder les → eerstvolgende maandag. */
internal fun nextSchoolDate(date: LocalDate, weekendLessons: Set<LocalDate>): LocalDate {
    if (!isWeekend(date) || date in weekendLessons) return date
    return date.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
}

internal fun weekVisibleDays(monday: LocalDate, weekendLessons: Set<LocalDate>): List<LocalDate> {
    val days = (0..4).map { monday.plusDays(it.toLong()) }.toMutableList()
    val saturday = monday.plusDays(5)
    val sunday = monday.plusDays(6)
    if (saturday in weekendLessons) days += saturday
    if (sunday in weekendLessons) days += sunday
    return days
}

internal fun schoolDays(
    startMonday: LocalDate,
    weekCount: Int,
    weekendLessons: Set<LocalDate>,
): List<LocalDate> {
    val days = ArrayList<LocalDate>(weekCount * 5)
    repeat(weekCount) { week ->
        days += weekVisibleDays(startMonday.plusWeeks(week.toLong()), weekendLessons)
    }
    return days
}

/**
 * Tussenuur alleen bij een ontbrekend lesuurnummer (2→4 betekent uur 3 vrij).
 * Een tijdgat tussen opeenvolgende lesuren is een pauze, geen tussenuur.
 */
internal fun buildDaySlots(
    lessons: List<Appointment>,
    minBreakMinutes: Long = MinBreakMinutes,
): List<DaySlot> {
    val sorted = lessons.sortedBy { it.startIso }
    if (sorted.isEmpty()) return emptyList()
    val slots = mutableListOf<DaySlot>()
    sorted.forEachIndexed { index, item ->
        if (index > 0) {
            gapBetween(sorted[index - 1], item, minBreakMinutes)?.let { slots += it }
        }
        slots += DaySlot.Lesson(item)
    }
    return slots
}

internal fun gapBetween(
    previous: Appointment,
    next: Appointment,
    minBreakMinutes: Long = MinBreakMinutes,
): DaySlot.Gap? {
    val prevEnd = previous.periodEnd()
    val nextStart = next.periodStart()
    if (prevEnd != null && nextStart != null && nextStart > prevEnd + 1) {
        return DaySlot.Gap(
            fromIso = previous.endIso,
            toIso = next.startIso,
            kind = GapKind.FreePeriod,
            missingFrom = prevEnd + 1,
            missingTo = nextStart - 1,
        )
    }
    val minutes = minutesBetween(previous.endIso, next.startIso) ?: return null
    if (minutes >= minBreakMinutes) {
        return DaySlot.Gap(
            fromIso = previous.endIso,
            toIso = next.startIso,
            kind = GapKind.Break,
        )
    }
    return null
}

internal fun minutesBetween(fromIso: String, toIso: String): Long? {
    val from = lessonDateTime(fromIso) ?: return null
    val to = lessonDateTime(toIso) ?: return null
    return Duration.between(from, to).toMinutes()
}

internal fun lessonDateTime(iso: String?): java.time.ZonedDateTime? {
    if (iso.isNullOrBlank()) return null
    val instant = runCatching { java.time.Instant.parse(iso) }.getOrNull()
        ?: runCatching { java.time.OffsetDateTime.parse(iso).toInstant() }.getOrNull()
        ?: return null
    return instant.atZone(zone)
}
