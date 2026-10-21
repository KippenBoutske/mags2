package nl.cita.magister.grades

import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.model.Grade
import nl.cita.magister.model.GradeColumnType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GradeCalculatorTest {
    @Test
    fun parseDutchComma() {
        assertEquals(7.2, GradeCalculator.parseDutchNumber("7,2")!!, 0.0001)
        assertEquals(10.0, GradeCalculator.parseDutchNumber("10")!!, 0.0001)
        assertNull(GradeCalculator.parseDutchNumber("O"))
        assertNull(GradeCalculator.parseDutchNumber("11"))
    }

    @Test
    fun weightedAverageUsesWeight() {
        val grades = listOf(
            grade(7.0, 1.0),
            grade(5.0, 3.0),
        )
        val avg = GradeCalculator.weightedAverage(grades)!!
        assertEquals((7.0 + 15.0) / 4.0, avg, 0.0001)
    }

    @Test
    fun ignoresNonCountingAndAverages() {
        val grades = listOf(
            grade(8.0, 1.0, counts = false),
            grade(6.0, 1.0, type = GradeColumnType.Average, counts = false),
            grade(7.0, 2.0),
        )
        assertEquals(7.0, GradeCalculator.weightedAverage(grades)!!, 0.0001)
    }

    @Test
    fun scoreNeededFormula() {
        val grades = listOf(grade(6.0, 2.0), grade(7.0, 2.0))
        val result = GradeCalculator.scoreNeeded(grades, target = 7.0, nextWeight = 2.0)
        // (7 * (4+2) - (12+14)) / 2 = (42 - 26) / 2 = 8
        assertEquals(8.0, result.needed, 0.0001)
        assertTrue(result.reachable)
        assertFalse(result.alreadyReached)
        assertEquals(6.5, result.currentAverage!!, 0.0001)
    }

    @Test
    fun scoreNeededUnreachable() {
        val grades = listOf(grade(4.0, 4.0))
        val result = GradeCalculator.scoreNeeded(grades, target = 8.0, nextWeight = 1.0)
        assertTrue(result.needed > 10.0)
        assertFalse(result.reachable)
    }

    @Test
    fun fixtureAveragesForWiskundeB() {
        val averages = GradeCalculator.averagesBySubject(MockFixtures.grades())
        val wiskunde = averages.first { it.subjectCode == "wi" }
        // (6.4*3 + 5.3*3) / 6 = 5.85
        assertEquals(5.85, wiskunde.average!!, 0.0001)
        assertEquals(2, wiskunde.gradesCounted)
    }

    @Test
    fun fixtureNederlandsUsesMagisterAverageColumn() {
        val averages = GradeCalculator.averagesBySubject(MockFixtures.grades())
        val dutch = averages.first { it.subjectCode == "ne" }
        assertEquals(7.0666, dutch.average!!, 0.01)
        assertEquals(7.1, dutch.magisterAverage!!, 0.0001)
    }

    private fun grade(
        value: Double,
        weight: Double,
        counts: Boolean = true,
        type: GradeColumnType = GradeColumnType.Grade,
    ) = Grade(
        id = value.toInt(),
        value = value.toString(),
        numericValue = value,
        isSufficient = value >= 5.5,
        counts = counts,
        weight = weight,
        subjectId = 1,
        subjectName = "Test",
        subjectCode = "te",
        columnHeading = "T",
        columnDescription = null,
        columnType = type,
        teacher = null,
        enteredAt = null,
        periodName = null,
        isPta = false,
    )
}
