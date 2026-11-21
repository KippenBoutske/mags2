package nl.cita.magister.wear

import nl.cita.magister.model.Appointment
import nl.cita.magister.model.InfoType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WearSyncPayloadTest {
    private val physics = Appointment(
        id = 1,
        startIso = "2026-10-21T09:00:00Z",
        endIso = "2026-10-21T10:00:00Z",
        periodFrom = 2,
        periodTo = 2,
        title = "Natuurkunde",
        location = "Griftdijk",
        classroom = "B1.08",
        teachers = listOf("JNS"),
        subjects = listOf("Natuurkunde"),
        content = null,
        infoType = InfoType.None,
        isCancelled = false,
        isChanged = false,
        isCompleted = false,
        hasAttachments = false,
    )

    private val biology = physics.copy(
        id = 2,
        startIso = "2026-10-21T10:00:00Z",
        endIso = "2026-10-21T10:50:00Z",
        periodFrom = 3,
        periodTo = 3,
        title = "Biologie",
        subjects = listOf("Biologie"),
        classroom = "C2.01",
    )

    @Test
    fun payloadRoundTripsJson() {
        val payload = WearSyncPayload(
            updatedAtMillis = 123456789L,
            studentName = "Jens",
            schoolName = "Citadel College",
            appointments = listOf(physics, biology),
        )
        val decoded = WearSyncPayload.fromJson(payload.toJson())
        assertNotNull(decoded)
        assertEquals("Jens", decoded?.studentName)
        assertEquals(2, decoded?.appointments?.size)
    }

    @Test
    fun switchesToUpcomingLessonFiveMinutesBeforeStart() {
        val during = WearScheduleHelper.currentOrNextLesson(
            listOf(physics, biology),
            Instant.parse("2026-10-21T09:30:30Z"),
        )
        assertNotNull(during)
        assertTrue(during!!.isCurrent)
        assertEquals("Natuurkunde", during.lesson.title)
        assertEquals(30, during.remainingMinutes)
        assertEquals("nog 30 min", WearScheduleHelper.countdownLabel(during))

        val soon = WearScheduleHelper.currentOrNextLesson(
            listOf(physics, biology),
            Instant.parse("2026-10-21T09:55:00Z"),
        )
        assertNotNull(soon)
        assertFalse(soon!!.isCurrent)
        assertEquals("Biologie", soon.lesson.title)
        assertEquals(5, soon.remainingMinutes)
        assertEquals("over 5 min", WearScheduleHelper.countdownLabel(soon))
    }
}
