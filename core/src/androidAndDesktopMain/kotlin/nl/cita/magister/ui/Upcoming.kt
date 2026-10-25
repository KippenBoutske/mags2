package nl.cita.magister.ui

import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Grade
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.subject.displaySubject
import nl.cita.magister.ui.homework.isToets
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val nl = Locale("nl", "NL")
private val weekDay = DateTimeFormatter.ofPattern("EEE H:mm", nl)
private val weekDayOnly = DateTimeFormatter.ofPattern("EEE", nl)

fun nextLessonHint(
    appointments: List<Appointment>,
    afterDate: LocalDate,
    afterTime: LocalTime? = null,
): String? {
    val next = appointments
        .asSequence()
        .filter { !it.isCancelled }
        .mapNotNull { lesson ->
            val date = localDateOf(lesson.startIso) ?: return@mapNotNull null
            val time = parseInstant(lesson.startIso)?.atZone(AppClock.zone)?.toLocalTime()
                ?: return@mapNotNull null
            Triple(lesson, date, time)
        }
        .filter { (_, date, time) ->
            date.isAfter(afterDate) ||
                (date == afterDate && (afterTime == null || time.isAfter(afterTime)))
        }
        .minByOrNull { (_, date, time) -> date.atTime(time) }
        ?: return null
    val whenText = next.second.atTime(next.third).format(weekDay).replaceFirstChar { it.lowercase(nl) }
    return "Volgende les: $whenText ${next.first.displaySubject()}"
}

fun upcomingTaskHint(
    homework: List<HomeworkItem>,
    afterDate: LocalDate,
    testsOnly: Boolean = false,
): String? {
    val next = homework
        .asSequence()
        .filter { !it.isDone }
        .filter { !testsOnly || isToets(it.infoType) }
        .mapNotNull { item ->
            val date = localDateOf(item.dueIso) ?: return@mapNotNull null
            if (date.isBefore(afterDate)) return@mapNotNull null
            item to date
        }
        .sortedBy { it.second }
        .firstOrNull()
        ?: return null
    val kind = if (isToets(next.first.infoType)) "toets" else "huiswerk"
    val day = when (next.second) {
        afterDate -> "vandaag"
        afterDate.plusDays(1) -> "morgen"
        else -> next.second.format(weekDayOnly)
    }
    return "Eerstvolgende $kind: $day ${next.first.displaySubject()}"
}

fun latestGradeHint(grade: Grade?): String? {
    if (grade == null) return null
    val value = formatGrade(grade.numericValue, grade.value)
    return "Laatste cijfer: $value ${grade.displaySubject()}"
}
