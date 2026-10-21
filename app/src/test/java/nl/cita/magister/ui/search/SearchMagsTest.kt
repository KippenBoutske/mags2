package nl.cita.magister.ui.search

import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.ui.nav.MagsPage
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMagsTest {
    @Test
    fun findsAcrossDomains() {
        val hits = searchMags(
            query = "Nederlands",
            grades = MockFixtures.grades(),
            appointments = MockFixtures.appointments(),
            homework = MockFixtures.homework(),
            messages = MockFixtures.messages(),
        )
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.any { it.page == MagsPage.Cijfers && it.title.contains("Nederlands") })
    }

    @Test
    fun ignoresShortQuery() {
        val hits = searchMags(
            query = "e",
            grades = MockFixtures.grades(),
            appointments = MockFixtures.appointments(),
            homework = MockFixtures.homework(),
            messages = MockFixtures.messages(),
        )
        assertTrue(hits.isEmpty())
    }
}
