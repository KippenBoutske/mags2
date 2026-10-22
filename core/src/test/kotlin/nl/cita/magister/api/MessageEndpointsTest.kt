package nl.cita.magister.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageEndpointsTest {
    @Test
    fun folderListPrefersWebThenDiscipulusAlle() {
        val urls = MagisterEndpoints.messageFolderListUrls("https://citadel.magister.net")
        assertEquals("https://citadel.magister.net/api/berichten/mappen", urls.first())
        assertTrue(urls.contains("https://citadel.magister.net/api/berichten/mappen/alle"))
    }

    @Test
    fun inboxFallbacksCoverDiscipulusAndMagistergo() {
        val urls = MagisterEndpoints.messageInboxUrls("https://citadel.magister.net", 424242)
        assertTrue(urls.any { it.contains("/api/berichten/mappen/1/berichten") })
        assertTrue(urls.any { it.contains("mapId=1") })
        assertTrue(urls.any { it.contains("/api/personen/424242/berichten") })
        assertTrue(urls.any { it.contains("mapId=7") })
        assertTrue(urls.none { it.contains("opp.read") })
    }
}
