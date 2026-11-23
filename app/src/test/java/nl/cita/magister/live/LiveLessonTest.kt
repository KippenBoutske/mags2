package nl.cita.magister.live

import nl.cita.magister.model.Appointment
import nl.cita.magister.model.InfoType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class LiveLessonTest {
    private val physics: Appointment = Appointment(
        id = 1,
        startIso = "2026-10-21T09:00:00Z",
        endIso = "2026-10-21T10:10:00Z",
        periodFrom = 2,
        periodTo = 2,
        title = "Natuurkunde",
        location = "Griftdijk",
        classroom = "B1.08",
        teachers = emptyList(),
        subjects = listOf("Natuurkunde"),
        content = null,
        infoType = InfoType.None,
        isCancelled = false,
        isChanged = false,
        isCompleted = false,
        hasAttachments = false,
    )
    private val biology: Appointment = physics.copy(
        id = 2,
        startIso = "2026-10-21T10:10:00Z",
        endIso = "2026-10-21T10:55:00Z",
        title = "Biologie",
        subjects = listOf("Biologie"),
        classroom = "C2.01",
    )
    private val appointments: List<Appointment> = listOf(physics, biology)

    @Test
    fun schedulesUpdateFiveMinutesBeforeNextLesson() {
        val now = Instant.parse("2026-10-21T10:04:00Z")

        val snapshot = LiveLesson.snapshot(appointments, now)

        assertEquals("Nu: Natuurkunde", snapshot?.title)
        assertEquals(
            Instant.parse("2026-10-21T10:05:00Z").toEpochMilli(),
            snapshot?.nextAlarmMillis,
        )

        val upcomingSnapshot = LiveLesson.snapshot(appointments, Instant.parse("2026-10-21T10:05:00Z"))
        assertEquals("Volgende: Biologie", upcomingSnapshot?.title)
        assertTrue(upcomingSnapshot?.text.orEmpty().contains("C2.01"))
    }

    @Test
    fun showsNextLessonAndRoomFiveMinutesBeforeItStarts() {
        val now = Instant.parse("2026-10-21T10:05:00Z")

        val snapshot = LiveLesson.snapshot(appointments, now)

        assertEquals("Volgende: Biologie", snapshot?.title)
        assertTrue(snapshot?.text.orEmpty().contains("C2.01"))
        assertEquals(Duration.ofMinutes(5).toMillis(), (snapshot?.whenMillis ?: 0L) - now.toEpochMilli())
        assertEquals("5 min", snapshot?.chip)
    }

    @Test
    fun ticksEveryMinuteWhileWaitingForFirstLesson() {
        val now = Instant.parse("2026-10-21T08:30:00Z")

        val snapshot = LiveLesson.snapshot(appointments, now)

        assertEquals("Volgende: Natuurkunde", snapshot?.title)
        assertEquals("30 min", snapshot?.chip)
        assertTrue(snapshot?.text.orEmpty().contains("over 30 min"))
        assertEquals(
            Instant.parse("2026-10-21T08:31:00Z").toEpochMilli(),
            snapshot?.nextAlarmMillis,
        )
    }

    @Test
    fun roundsPartialMinutesUpForAccurateCountdownChip() {
        val now = Instant.parse("2026-10-21T10:05:30Z")

        val snapshot = LiveLesson.snapshot(appointments, now)

        assertEquals("Volgende: Biologie", snapshot?.title)
        assertEquals("5 min", snapshot?.chip)
        assertEquals(
            Instant.parse("2026-10-21T10:06:00Z").toEpochMilli(),
            snapshot?.nextAlarmMillis,
        )

        val lastHalfMinute = LiveLesson.snapshot(appointments, Instant.parse("2026-10-21T10:09:30Z"))
        assertEquals("1 min", lastHalfMinute?.chip)
    }

    @Test
    fun showsUitvalChipWhenUpcomingLessonIsCancelled() {
        val cancelledBio = biology.copy(isCancelled = true)
        val snapshot = LiveLesson.snapshot(listOf(physics, cancelledBio), Instant.parse("2026-10-21T10:06:00Z"))

        assertEquals("Uitval: Biologie", snapshot?.title)
        assertEquals("Uitval", snapshot?.chip)
    }
}
