package nl.cita.magister.ui.today

import nl.cita.magister.model.Appointment
import nl.cita.magister.model.InfoType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class TodayNextLessonTest {
    @Test
    fun switchesToNextLessonAtFiveMinuteBoundary() {
        val current = lesson(id = 1, subject = "Natuurkunde", start = "2026-10-22T08:30:00Z", end = "2026-10-22T09:30:00Z")
        val next = lesson(id = 2, subject = "Biologie", start = "2026-10-22T09:30:00Z", end = "2026-10-22T10:15:00Z")

        assertFalse(showUpcomingLesson(current, next, LocalTime.of(11, 24)))
        assertTrue(showUpcomingLesson(current, next, LocalTime.of(11, 25)))
    }

    private fun lesson(id: Int, subject: String, start: String, end: String) = Appointment(
        id = id,
        startIso = start,
        endIso = end,
        periodFrom = id,
        periodTo = id,
        title = subject,
        location = "Griftdijk",
        classroom = "B1.08",
        teachers = emptyList(),
        subjects = listOf(subject),
        content = null,
        infoType = InfoType.None,
        isCancelled = false,
        isChanged = false,
        isCompleted = false,
        hasAttachments = false,
    )
}
