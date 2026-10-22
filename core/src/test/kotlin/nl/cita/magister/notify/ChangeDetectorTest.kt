package nl.cita.magister.notify

import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.grades.GradeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangeDetectorTest {
    @Test
    fun firstSnapshotDoesNotPingSchedule() {
        val lessons = MockFixtures.appointments()
        val keys = lessons.map { ChangeDetector.appointmentKey(it) }.toSet()
        assertTrue(ChangeDetector.scheduleDiff(emptySet(), lessons).isEmpty())
        assertTrue(ChangeDetector.scheduleDiff(keys, lessons).isEmpty())
    }

    @Test
    fun newGradeIsReported() {
        val grades = MockFixtures.grades()
        val seen = grades.map { it.id }.toSet() - grades.first().id
        val pings = ChangeDetector.newGrades(seen, grades)
        assertEquals(1, pings.size)
        assertEquals(grades.first().subjectName, pings.single().subject)
    }

    @Test
    fun cancelledLessonPings() {
        val lessons = MockFixtures.appointments()
        val seen = lessons.map { ChangeDetector.appointmentKey(it) }.toSet()
        val cancelled = lessons.first().copy(isCancelled = true, isChanged = true)
        val next = listOf(cancelled) + lessons.drop(1)
        val pings = ChangeDetector.scheduleDiff(seen, next)
        assertTrue(pings.any { it.kind == SchedulePing.Kind.Cancelled })
    }

    @Test
    fun averagesStayStableForFixtures() {
        val averages = GradeCalculator.averagesBySubject(MockFixtures.grades())
        assertTrue(averages.any { it.subjectCode == "gs" })
    }
}
