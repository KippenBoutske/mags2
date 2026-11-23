package nl.cita.magister.wear

import kotlinx.serialization.Serializable
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Grade
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.StudentProfile
import nl.cita.magister.model.SubjectAverage
import nl.cita.magister.parse.MagisterJson
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object WearSyncContract {
    const val PATH_SNAPSHOT = "/mags/snapshot"
    const val PATH_REQUEST_SYNC = "/mags/request-sync"
    const val KEY_PAYLOAD = "payload"
    const val KEY_UPDATED_AT = "updated_at"
    const val NEXT_LESSON_LEAD_MINUTES = 5L
}

@Serializable
data class WearSyncPayload(
    val updatedAtMillis: Long,
    val studentName: String? = null,
    val schoolName: String? = null,
    val appointments: List<Appointment> = emptyList(),
    val latestGrades: List<Grade> = emptyList(),
    val averages: List<SubjectAverage> = emptyList(),
    val openHomework: List<HomeworkItem> = emptyList(),
) {
    fun toJson(): String = MagisterJson.instance.encodeToString(serializer(), this)

    companion object {
        fun fromJson(json: String): WearSyncPayload? =
            runCatching { MagisterJson.instance.decodeFromString(serializer(), json) }.getOrNull()

        fun build(
            profile: StudentProfile?,
            appointments: List<Appointment>,
            grades: List<Grade>,
            latestGrades: List<Grade>,
            homework: List<HomeworkItem>,
            now: Instant = Instant.now(),
            zone: ZoneId = ZoneId.of("Europe/Amsterdam"),
        ): WearSyncPayload {
            val today = now.atZone(zone).toLocalDate()
            val windowEnd = today.plusDays(2)
            val nearbyAppointments = appointments.filter { appt ->
                val date = WearScheduleHelper.localDateOf(appt.startIso, zone)
                date != null && !date.isBefore(today) && !date.isAfter(windowEnd)
            }.sortedBy { it.startIso }

            val currentYearGrades = GradeCalculator.currentSchoolYearGrades(grades, today)
            val averages = GradeCalculator.averagesBySubject(currentYearGrades)
            val latest = GradeCalculator.latestEarnedGrades(
                GradeCalculator.currentSchoolYearGrades(latestGrades + grades, today),
                limit = 20,
            )
            val openHw = homework
                .filter { !it.isDone }
                .sortedBy { it.dueIso }
                .take(15)

            return WearSyncPayload(
                updatedAtMillis = now.toEpochMilli(),
                studentName = profile?.firstName?.takeIf { it.isNotBlank() },
                schoolName = profile?.schoolName?.takeIf { it.isNotBlank() },
                appointments = nearbyAppointments,
                latestGrades = latest,
                averages = averages,
                openHomework = openHw,
            )
        }
    }
}

data class WearLessonFocus(
    val lesson: Appointment,
    val isCurrent: Boolean,
    val remainingMinutes: Int,
    val startLabel: String,
    val endLabel: String,
)

object WearScheduleHelper {
    val defaultZone: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun parseInstant(iso: String?, zone: ZoneId = defaultZone): Instant? {
        if (iso.isNullOrBlank()) return null
        return runCatching { Instant.parse(iso) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(iso).atZone(zone).toInstant() }.getOrNull()
    }

    fun localDateOf(iso: String?, zone: ZoneId = defaultZone): LocalDate? =
        parseInstant(iso, zone)?.atZone(zone)?.toLocalDate()

    fun formatTime(iso: String?, zone: ZoneId = defaultZone): String =
        parseInstant(iso, zone)?.atZone(zone)?.format(timeFmt).orEmpty()

    fun lessonsForDate(
        appointments: List<Appointment>,
        date: LocalDate,
        zone: ZoneId = defaultZone,
    ): List<Appointment> = appointments
        .filter { localDateOf(it.startIso, zone) == date }
        .sortedBy { parseInstant(it.startIso, zone) ?: Instant.MAX }

    fun currentOrNextLesson(
        appointments: List<Appointment>,
        now: Instant = Instant.now(),
        zone: ZoneId = defaultZone,
    ): WearLessonFocus? {
        val today = now.atZone(zone).toLocalDate()
        val day = lessonsForDate(appointments, today, zone).filter { !it.isCancelled }
        if (day.isEmpty()) return null

        val current = day.firstOrNull { appt ->
            val start = parseInstant(appt.startIso, zone) ?: return@firstOrNull false
            val end = parseInstant(appt.endIso, zone) ?: start.plus(Duration.ofMinutes(45))
            !now.isBefore(start) && now.isBefore(end)
        }
        val upcoming = day.firstOrNull { appt ->
            val start = parseInstant(appt.startIso, zone) ?: return@firstOrNull false
            start.isAfter(now)
        }
        val upcomingStartsSoon = upcoming?.let { appt ->
            val start = parseInstant(appt.startIso, zone) ?: return@let false
            !now.isBefore(start.minus(Duration.ofMinutes(WearSyncContract.NEXT_LESSON_LEAD_MINUTES)))
        } == true

        val showingUpcoming = upcoming != null && (upcomingStartsSoon || current == null)
        val focus = if (showingUpcoming) upcoming else current ?: return null
        val isCurrent = !showingUpcoming && current != null
        val target = if (isCurrent) {
            parseInstant(focus.endIso, zone) ?: now
        } else {
            parseInstant(focus.startIso, zone) ?: now
        }
        val remainingSec = Duration.between(now, target).seconds.coerceAtLeast(0L)
        val remainingMin = ((remainingSec + 59L) / 60L).toInt()
        return WearLessonFocus(
            lesson = focus,
            isCurrent = isCurrent,
            remainingMinutes = remainingMin,
            startLabel = formatTime(focus.startIso, zone),
            endLabel = formatTime(focus.endIso, zone),
        )
    }

    fun countdownLabel(focus: WearLessonFocus): String = when {
        focus.isCurrent && focus.remainingMinutes > 0 -> "nog ${focus.remainingMinutes} min"
        focus.isCurrent -> "tot ${focus.endLabel}"
        focus.remainingMinutes >= 60 -> {
            val h = focus.remainingMinutes / 60
            val m = focus.remainingMinutes % 60
            if (m == 0) "over ${h}u" else "over ${h}u ${m}m"
        }
        focus.remainingMinutes > 0 -> "over ${focus.remainingMinutes} min"
        else -> focus.startLabel
    }
}
