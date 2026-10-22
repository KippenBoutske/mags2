package nl.cita.magister.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiFailureTest {
    @Test
    fun messages403IsFriendlyWithoutPath() {
        val error = ApiFailure("/api/berichten/mappen/alle", 403, """{"fouttype":"OnvoldoendePrivileges"}""")
        assertEquals("Berichten zijn voor dit account niet beschikbaar.", error.friendly())
        assertFalse(error.friendly().contains("HTTP"))
        assertFalse(error.friendly().contains("/api/"))
        assertTrue(error.summary().contains("/api/berichten/mappen/alle"))
        assertTrue(error.summary().contains("403"))
    }

    @Test
    fun gradesAndAgenda403StayPerSection() {
        assertEquals(
            "Cijfers zijn nu niet beschikbaar.",
            ApiFailure("/api/personen/1/cijfers", 403, "").friendly(),
        )
        assertEquals(
            "Rooster is nu niet beschikbaar.",
            ApiFailure("/api/personen/1/afspraken", 403, "").friendly(),
        )
        assertEquals(
            "Rooster is nu niet beschikbaar.",
            ApiFailure("/afspraken", 403, "").friendly(),
        )
    }

    @Test
    fun sectionErrorsSplitWarnings() {
        val sections = SectionErrors.from(
            listOf(
                ApiFailure("/cijfers", 403, ""),
                ApiFailure("/afspraken", 403, ""),
                ApiFailure("/berichten", 403, ""),
            ),
        )
        assertEquals(403, sections.grades?.code)
        assertEquals(403, sections.agenda?.code)
        assertEquals(403, sections.messages?.code)
    }

    @Test
    fun extras403StayPerSection() {
        assertEquals(
            "Afwezigheid is nu niet beschikbaar.",
            ApiFailure("/api/personen/1/absenties", 403, "").friendly(),
        )
        assertEquals(
            "Studiewijzers zijn nu niet beschikbaar.",
            ApiFailure("/api/leerlingen/1/studiewijzers", 403, "").friendly(),
        )
        val sections = SectionErrors.from(
            listOf(
                ApiFailure("/absenties", 403, ""),
                ApiFailure("/studiewijzers", 403, ""),
                ApiFailure("/opdrachten", 403, ""),
            ),
        )
        assertEquals(403, sections.absences?.code)
        assertEquals(403, sections.studyGuides?.code)
        assertEquals(403, sections.assignments?.code)
    }
}
