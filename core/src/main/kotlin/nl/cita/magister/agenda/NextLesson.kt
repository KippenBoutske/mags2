package nl.cita.magister.agenda

import nl.cita.magister.model.Appointment
import java.time.Instant
import java.time.OffsetDateTime

object NextLesson {
    fun pick(appointments: List<Appointment>, now: Instant = Instant.now()): Appointment? {
        return appointments
            .asSequence()
            .filter { !it.isCancelled }
            .mapNotNull { lesson -> parseStart(lesson.startIso)?.let { lesson to it } }
            .filter { (_, start) -> !start.isBefore(now) }
            .minByOrNull { it.second }
            ?.first
    }

    fun parseStart(iso: String?): Instant? {
        if (iso.isNullOrBlank()) return null
        return runCatching { Instant.parse(iso) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()
    }
}
