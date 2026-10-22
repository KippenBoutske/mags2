package nl.cita.magister.export

import nl.cita.magister.fixtures.MockFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GradeExportTest {
    @Test
    fun payloadUsesRequiredFields() {
        val payload = GradeExport.build(
            profile = MockFixtures.profile(),
            grades = MockFixtures.grades(),
            exportedAt = Instant.parse("2026-10-21T22:00:00Z"),
        )
        assertEquals("magister-app", payload.source)
        assertEquals("2026-10-21T22:00:00Z", payload.exportedAt)
        assertEquals("Jens de Vries", payload.student.name)
        assertEquals("5VWO-A", payload.student.className)
        assertEquals("citadel.magister.net", payload.student.tenant)
        assertTrue(payload.subjects.any { it.code == "wi" && it.weighted })
        val wiskunde = payload.subjects.first { it.code == "wi" }
        assertEquals(5.85, wiskunde.average!!, 0.0001)
        assertTrue(payload.attention.any { it.code == "wi" })
        assertTrue(payload.attention.first { it.code == "wi" }.reason.contains("onder de 6,0"))
    }

    @Test
    fun trendingDownIsFlagged() {
        val subject = ExportSubject(
            code = "na",
            name = "Natuurkunde",
            average = 6.4,
            weighted = true,
            grades = listOf(
                ExportGrade(7.8, 2.0, "PW1", "2025-09-01"),
                ExportGrade(6.2, 2.0, "PW2", "2025-10-01"),
                ExportGrade(5.1, 2.0, "PW3", "2025-11-01"),
            ),
        )
        val attention = GradeExport.attention(listOf(subject))
        assertEquals(1, attention.size)
        assertTrue(attention.first().reason.contains("lager"))
    }
}
