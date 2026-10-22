package nl.cita.magister.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageEndpointsTest {
    @Test
    fun folderListKeepsWebAndDiscipulusAlle() {
        val urls = MagisterEndpoints.messageFolderListUrls("https://citadel.magister.net")
        assertEquals("https://citadel.magister.net/api/berichten/mappen", urls.first())
        assertTrue(urls.contains("https://citadel.magister.net/api/berichten/mappen/alle"))
    }

    @Test
    fun inboxPrefersOfficialPostvakinThenKnownFallbacks() {
        val urls = MagisterEndpoints.messageInboxUrls("https://citadel.magister.net", 424242)
        assertEquals(
            "https://citadel.magister.net/api/berichten/postvakin/berichten?top=40&skip=0",
            urls.first(),
        )
        assertTrue(urls.any { it.contains("/api/berichten/postvakin?") })
        assertTrue(urls.any { it.contains("/api/berichten/mappen/1/berichten") })
        assertTrue(urls.any { it.contains("mapId=1") })
        assertTrue(urls.any { it.contains("/api/personen/424242/berichten") })
        assertTrue(urls.any { it.contains("mapId=7") })
        assertTrue(urls.none { it.contains("opp.read") })
        assertTrue(urls.none { it.contains("grades.read") })
        assertTrue(urls.none { it.contains("email") })
    }

    @Test
    fun gradeColumnInfoUsesExtraCijferKolomInfo() {
        assertEquals(
            "https://citadel.magister.net/api/personen/424242/aanmeldingen/2025001/cijfers/extracijferkolominfo/201",
            MagisterEndpoints.gradeColumnInfo(
                "https://citadel.magister.net",
                424242,
                2025001,
                201,
            ),
        )
    }

    @Test
    fun m6loappScopeUnchanged() {
        assertEquals(
            "openid profile offline_access magister.mobile magister.ecs",
            MagisterEndpoints.SCOPE,
        )
    }
}
