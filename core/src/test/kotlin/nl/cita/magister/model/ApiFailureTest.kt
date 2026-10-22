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
}
