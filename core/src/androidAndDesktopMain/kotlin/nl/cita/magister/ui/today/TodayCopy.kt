package nl.cita.magister.ui.today

import nl.cita.magister.model.Appointment
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.InfoType
import nl.cita.magister.subject.displaySubject
import nl.cita.magister.ui.components.minutesUntil
import nl.cita.magister.ui.components.overLabel
import nl.cita.magister.ui.formatTime
import nl.cita.magister.ui.localDateOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val nl = Locale("nl", "NL")
private val shortDay = DateTimeFormatter.ofPattern("d MMM", nl)

fun subjectChipLabel(name: String, code: String): String {
    val abbr = code.trim().takeIf { it.isNotBlank() && it != "?" }?.uppercase(nl)
    if (abbr != null) return abbr
    return name.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
}

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

fun dueChipLabel(dueIso: String, today: LocalDate): String {
    val date = localDateOf(dueIso)
    val time = formatTime(dueIso)
    val day = when {
        date == null -> ""
        date == today -> "vandaag"
        date == today.plusDays(1) -> "morgen"
        date == today.minusDays(1) -> "gisteren"
        else -> date.format(shortDay)
    }
    return listOf(day, time).filter { it.isNotBlank() }.joinToString(" · ")
}

fun lessonChipLabel(lesson: Appointment): String {
    val time = formatTime(lesson.startIso)
    val subject = lesson.displaySubject()
    return listOf(time, subject).filter { it.isNotBlank() }.joinToString(" ")
}

/** Timing under NU / VOLGENDE LES. Never repeats "nu" while a lesson is on. */
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
    return overLabel(minutesUntil(startIso, nowMillis))
}
