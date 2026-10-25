package nl.cita.magister.notify

import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Grade

data class GradePing(
    val subject: String,
    val value: String?,
    val column: String?,
)

data class SchedulePing(
    val title: String,
    val kind: Kind,
) {
    enum class Kind { New, Changed, Cancelled }
}

object ChangeDetector {
    fun newGrades(seenIds: Set<Int>, current: List<Grade>): List<GradePing> {
        return current
            .filter { it.id !in seenIds }
            .filter { !it.value.isNullOrBlank() }
            .map { grade ->
                GradePing(
                    subject = grade.subjectName,
                    value = grade.value,
                    column = grade.columnHeading ?: grade.columnDescription,
                )
            }
    }

    fun appointmentKey(item: Appointment): String =
        listOf(
            item.id,
            item.startIso,
            item.endIso,
            item.title,
            item.classroom ?: item.location.orEmpty(),
            item.isCancelled,
            item.isChanged,
        ).joinToString("|")

    fun scheduleDiff(seenKeys: Set<String>, current: List<Appointment>): List<SchedulePing> {
        if (seenKeys.isEmpty()) return emptyList()
        val currentById = current.associateBy { it.id }
        val seenIds = seenKeys.mapNotNull { it.substringBefore('|').toIntOrNull() }.toSet()
        val pings = mutableListOf<SchedulePing>()
        current.forEach { lesson ->
            val key = appointmentKey(lesson)
            if (key in seenKeys) return@forEach
            val kind = when {
                lesson.id !in seenIds -> SchedulePing.Kind.New
                lesson.isCancelled -> SchedulePing.Kind.Cancelled
                else -> SchedulePing.Kind.Changed
            }
            pings += SchedulePing(lesson.title, kind)
        }
        seenIds.forEach { id ->
            if (id !in currentById) {
                val title = seenKeys.firstOrNull { it.startsWith("$id|") }?.split('|')?.getOrNull(3)
                pings += SchedulePing(title ?: "Les", SchedulePing.Kind.Cancelled)
            }
        }
        return pings.distinctBy { it.title to it.kind }
    }
}
