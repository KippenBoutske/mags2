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
        assertEquals(7.1, dutch.average!!, 0.0001)
        assertEquals(7.1, dutch.magisterAverage!!, 0.0001)
        assertEquals("7,1", dutch.averageLabel)
        assertEquals(2, dutch.gradesCounted)
    }

    @Test
    fun citadelScreenshotDoesNotUseUnweightedMean() {
        val parsed = MockFixtures.citadelGrades(withExtra = false)
        val averages = GradeCalculator.averagesBySubject(parsed)
        val bio = averages.first { it.subjectCode == "bi" }
        assertEquals(4.8, bio.average!!, 0.0001)
        assertEquals("4,8", bio.averageLabel)
        assertEquals(1, bio.gradesCounted)
        val countedBio = GradeCalculator.countedGrades(parsed.filter { it.subjectCode == "bi" })
        assertEquals(listOf("Klassenwerk"), countedBio.map { it.columnHeading })
        assertTrue((bio.average ?: 0.0) != 6.2)
    }

    @Test
    fun citadelAnwAndCkvDropDuplicateSeAndLetterGrades() {
        val grades = MockFixtures.citadelGrades()
        val averages = GradeCalculator.averagesBySubject(grades)
        val anw = averages.first { it.subjectCode == "anw" }
        assertEquals(6.9, anw.average!!, 0.0001)
        assertEquals("6,9", anw.averageLabel)
        assertEquals(0, anw.gradesCounted)
        val ckv = averages.first { it.subjectCode == "ckv" }
        assertEquals(6.4, ckv.average!!, 0.0001)
        assertEquals(0, ckv.gradesCounted)
        val economy = averages.first { it.subjectCode == "ec" }
        assertNull(economy.average)
        assertEquals("–", economy.averageLabel)
    }

    @Test
    fun citadelExtraColumnInfoSuppliesWeightsAndRetake() {
        val grades = MockFixtures.citadelGrades()
        val bio = GradeCalculator.countedGrades(grades.filter { it.subjectCode == "bi" }).single()
        assertEquals(2.0, bio.weight!!, 0.0001)
        assertEquals("×2", GradeCalculator.formatWeight(bio.weight))
        val wi = GradeCalculator.countedGrades(grades.filter { it.subjectCode == "wi" })
        assertEquals(setOf(402, 411), wi.map { it.columnId }.toSet())
        assertTrue(wi.all { it.weight == 3.0 })
        val summary = GradeCalculator.averagesBySubject(grades).first { it.subjectCode == "wi" }
        assertEquals(6.3, summary.average!!, 0.0001)
        assertEquals("6,3", summary.averageLabel)
        assertEquals(2, summary.gradesCounted)
    }

    @Test
    fun skipsZeroWeightAndNonNumeric() {
        val grades = listOf(
            grade(8.0, 0.0),
            grade(7.0, 1.0, raw = "V"),
            grade(6.0, 2.0),
        )
        assertEquals(6.0, GradeCalculator.weightedAverage(grades)!!, 0.0001)
        assertEquals(1, GradeCalculator.countedGrades(grades).size)
    }

    @Test
    fun herkansingReplacesOriginalColumn() {
        val original = grade(4.0, 3.0, id = 1, columnId = 401)
        val retake = grade(7.5, 3.0, id = 2, columnId = 411, replacesColumnId = 401)
        val later = grade(5.0, 3.0, id = 3, columnId = 402)
        val counted = GradeCalculator.countedGrades(listOf(original, retake, later))
        assertEquals(setOf(2, 3), counted.map { it.id }.toSet())
        assertEquals(6.25, GradeCalculator.weightedAverage(listOf(original, retake, later))!!, 0.0001)
    }

    @Test
    fun latestEarnedGradesAreIndividualNewestFirst() {
        val latest = GradeCalculator.latestEarnedGrades(MockFixtures.grades())
        assertEquals(listOf("Wiskunde B", "Engels", "Nederlands", "Biologie", "Geschiedenis"), latest.map { it.subjectName })
        assertEquals(listOf("5,3", "7,6", "6,8", "8,4", "7,0"), latest.map { it.value })
        assertEquals(listOf("PW2", "WR1", "LT1", "PW1", "PO1"), latest.map { it.columnHeading })
        assertTrue(latest.all { it.columnType == GradeColumnType.Grade })
    }

    @Test
    fun latestEarnedGradesDropsGemSeAndEndEvenIfNewer() {
        val gem = grade(7.1, 0.0, type = GradeColumnType.Average, id = 19, heading = "GEM")
            .copy(enteredAt = "2025-10-11T12:00:00.0000000Z", value = "7,1", subjectName = "Nederlands")
        val se = grade(4.8, 0.0, type = GradeColumnType.Average, id = 202, heading = "SE cijfer")
            .copy(enteredAt = "2025-10-09T09:00:00.0000000Z", value = "4,8", subjectName = "biologie")
        val namedGem = grade(6.0, 1.0, type = GradeColumnType.Grade, id = 99, heading = "Voortschrijdend gemiddelde")
            .copy(enteredAt = "2025-10-12T08:00:00.0000000Z", value = "6,0")
        val pw2 = grade(5.3, 3.0, id = 32, heading = "PW2")
            .copy(enteredAt = "2025-10-10T10:00:00.0000000Z", value = "5,3")
        val writing = grade(7.6, 2.0, id = 22, heading = "Writing")
            .copy(enteredAt = "2025-10-08T10:00:00.0000000Z", value = "7,6")
        val latest = GradeCalculator.latestEarnedGrades(listOf(gem, se, namedGem, pw2, writing))
        assertEquals(listOf("5,3", "7,6"), latest.map { it.value })
        assertEquals(listOf("PW2", "Writing"), latest.map { it.columnHeading })
    }

    @Test
    fun formatAverageKeepsOneOrTwoDecimals() {
        assertEquals("6,9", GradeCalculator.formatAverage(6.9, "6,9"))
        assertEquals("5,85", GradeCalculator.formatAverage(5.85))
        assertEquals("–", GradeCalculator.formatAverage(null))
        assertNull(GradeCalculator.formatWeight(null))
        assertNull(GradeCalculator.formatWeight(0.0))
    }

    private fun grade(
        value: Double,
        weight: Double,
        counts: Boolean = true,
        type: GradeColumnType = GradeColumnType.Grade,
        id: Int = value.toInt(),
        columnId: Int? = null,
        replacesColumnId: Int? = null,
        heading: String = "T",
        raw: String? = null,
    ) = Grade(
        id = id,
        value = raw ?: value.toString(),
        numericValue = if (raw != null) GradeCalculator.parseDutchNumber(raw) else value,
        isSufficient = value >= 5.5,
        counts = counts,
        weight = weight,
        subjectId = 1,
        subjectName = "Test",
        subjectCode = "te",
        columnHeading = heading,
        columnDescription = null,
        columnType = type,
        teacher = null,
        enteredAt = null,
        periodName = null,
        isPta = false,
        columnId = columnId,
        replacesColumnId = replacesColumnId,
    )
}
