package nl.cita.magister.ui.schedule

import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.InfoType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ScheduleDaysTest {
    @Test
    fun schoolDaysAreWeekdaysOnly() {
        val monday = LocalDate.of(2026, 10, 19)
        val days = schoolDays(monday, weekCount = 2, weekendLessons = emptySet())
        assertEquals(10, days.size)
        assertTrue(days.all { it.dayOfWeek.value in 1..5 })
        assertFalse(days.contains(LocalDate.of(2026, 10, 24)))
        assertEquals(LocalDate.of(2026, 10, 23), days[4])
        assertEquals(LocalDate.of(2026, 10, 26), days[5])
    }

    @Test
    fun weekendWithLessonIsIncluded() {
        val monday = LocalDate.of(2026, 10, 19)
        val saturday = LocalDate.of(2026, 10, 24)
        val days = schoolDays(monday, weekCount = 2, weekendLessons = setOf(saturday))
        assertTrue(days.contains(saturday))
        assertEquals(saturday, days[5])
        assertEquals(LocalDate.of(2026, 10, 26), days[6])
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
            weekVisibleDays(monday, setOf(saturday)).map { it.dayOfWeek })
    }

    @Test
    fun saturdayAndSundayJumpToNextMonday() {
        val saturday = LocalDate.of(2026, 10, 24)
        val sunday = LocalDate.of(2026, 10, 25)
        val monday = LocalDate.of(2026, 10, 26)
        assertEquals(monday, nextSchoolDate(saturday, emptySet()))
        assertEquals(monday, nextSchoolDate(sunday, emptySet()))
        assertEquals(saturday, nextSchoolDate(saturday, setOf(saturday)))
        assertEquals(LocalDate.of(2026, 10, 22), nextSchoolDate(LocalDate.of(2026, 10, 22), emptySet()))
    }

    @Test
    fun consecutivePeriodsWithTimeGapAreBreakNotFreePeriod() {
        val wednesday = MockFixtures.appointments().filter { it.startIso.startsWith("2026-10-21") }
        val slots = buildDaySlots(wednesday)
        val gaps = slots.filterIsInstance<DaySlot.Gap>()
        assertTrue(gaps.isNotEmpty())
        assertTrue(gaps.all { it.kind == GapKind.Break })
        assertFalse(gaps.any { it.kind == GapKind.FreePeriod })
        val periods = wednesday.map { it.periodStart() to it.periodEnd() }
        assertEquals(listOf(1 to 1, 2 to 2, 3 to 3, 4 to 4, 5 to 5, 6 to 6), periods)
    }

    @Test
    fun missingPeriodNumberIsTussenuur() {
        val thursday = MockFixtures.appointments().filter { it.startIso.startsWith("2026-10-22") }
        val slots = buildDaySlots(thursday)
        val free = slots.filterIsInstance<DaySlot.Gap>().filter { it.kind == GapKind.FreePeriod }
        assertEquals(1, free.size)
        assertEquals(2, free.single().missingFrom)
        assertEquals(2, free.single().missingTo)
        assertFalse(slots.filterIsInstance<DaySlot.Gap>().any { it.kind == GapKind.Break })
    }

    @Test
    fun timeGapWithoutPeriodSkipIsNeverTussenuur() {
        val first = lesson(1, "2026-10-21T08:20:00Z", "2026-10-21T09:10:00Z")
        val second = lesson(2, "2026-10-21T09:25:00Z", "2026-10-21T10:15:00Z")
        val gap = gapBetween(first, second)!!
        assertEquals(GapKind.Break, gap.kind)
    }

    @Test
    fun skipFrom2To4IsTussenuurForHour3() {
        val first = lesson(2, "2026-10-21T08:20:00Z", "2026-10-21T09:10:00Z")
        val second = lesson(4, "2026-10-21T10:15:00Z", "2026-10-21T11:05:00Z")
        val gap = gapBetween(first, second)!!
        assertEquals(GapKind.FreePeriod, gap.kind)
        assertEquals(3, gap.missingFrom)
        assertEquals(3, gap.missingTo)
    }

    private fun lesson(period: Int, start: String, end: String) = Appointment(
        id = period,
        startIso = start,
        endIso = end,
        periodFrom = period,
        periodTo = period,
        title = "Les $period",
        location = null,
        classroom = null,
        teachers = emptyList(),
        subjects = emptyList(),
        content = null,
        infoType = InfoType.None,
        isCancelled = false,
        isCompleted = false,
        hasAttachments = false,
    )
}
