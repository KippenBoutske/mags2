package nl.cita.magister.agenda

import nl.cita.magister.fixtures.MockFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class NextLessonTest {
    @Test
    fun picksFirstUpcoming() {
        val now = Instant.parse("2026-10-21T07:00:00Z")
        val next = NextLesson.pick(MockFixtures.appointments(), now)
        assertEquals("Wiskunde B", next?.title)
    }

    @Test
    fun skipsCancelledAndPast() {
        val now = Instant.parse("2026-10-21T12:00:00Z")
        val next = NextLesson.pick(MockFixtures.appointments(), now)
        assertEquals("Biologie", next?.title)
    }

    @Test
    fun emptyWhenDayIsOver() {
        val now = Instant.parse("2026-10-23T07:00:00Z")
        assertNull(NextLesson.pick(MockFixtures.appointments(), now))
    }
}
