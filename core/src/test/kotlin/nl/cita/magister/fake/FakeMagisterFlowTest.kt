package nl.cita.magister.fake

import nl.cita.magister.api.MagisterEndpoints
import nl.cita.magister.parse.MagisterParsers
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeMagisterFlowTest {
    private lateinit var server: okhttp3.mockwebserver.MockWebServer
    private lateinit var fake: FakeMagister
    private val http = OkHttpClient()

    @Before
    fun start() {
        val started = FakeMagister.start()
        server = started.first
        fake = started.second
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun loginCallbackExchangesCodeThenAccount() {
        val tokens = postForm(
            "/connect/token",
            mapOf(
                "grant_type" to "authorization_code",
                "code" to "good-code",
                "code_verifier" to "verifier",
                "client_id" to MagisterEndpoints.CLIENT_ID,
            ),
        )
        assertTrue(tokens.contains("live-access"))
        assertEquals(1, fake.tokenExchanges)
        val account = get("/api/account", "live-access")
        val profile = MagisterParsers.parseProfile(
            json = account,
            tenantHost = "citadel.magister.net",
            schoolName = "Citadel College",
            enrollment = null,
        )
        assertEquals("Jens", profile.firstName)
        assertEquals(424242, profile.personId)
    }

    @Test
    fun scheduleAndGradesHappyPath() {
        val agenda = get("/api/personen/424242/afspraken?van=2026-10-21&tot=2026-10-22", "live-access")
        val lessons = MagisterParsers.parseAppointments(agenda)
        assertTrue(lessons.any { it.title == "Wiskunde B" })
        assertTrue(lessons.any { it.isCancelled })
        val grades = get("/api/personen/424242/aanmeldingen/1/cijfers/cijferoverzichtvooraanmelding", "live-access")
        assertTrue(MagisterParsers.parseGrades(grades).isNotEmpty())
        val homework = MagisterParsers.homeworkFromAppointments(lessons)
        assertTrue(homework.isNotEmpty())
    }

    @Test
    fun expiredAccessRefreshesThenRetries() {
        fake.scenario = FakeScenario.AccessExpired
        val first = raw("/api/account", FakeMagister.TOKEN_EXPIRED)
        assertEquals(401, first.first)
        val refresh = postForm(
            "/connect/token",
            mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to FakeMagister.TOKEN_REFRESH,
                "client_id" to MagisterEndpoints.CLIENT_ID,
            ),
        )
        assertTrue(refresh.contains("live-access"))
        assertEquals(1, fake.refreshCalls)
        val retry = raw("/api/account", FakeMagister.TOKEN_LIVE)
        assertEquals(200, retry.first)
        assertTrue(retry.second.contains("Jens"))
    }

    @Test
    fun expiredRefreshStaysUnauthorized() {
        fake.scenario = FakeScenario.RefreshExpired
        val refresh = rawForm(
            "/connect/token",
            mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to FakeMagister.TOKEN_REFRESH_DEAD,
                "client_id" to MagisterEndpoints.CLIENT_ID,
            ),
        )
        assertEquals(400, refresh.first)
        assertTrue(refresh.second.contains("invalid_grant"))
        val account = raw("/api/account", "any")
        assertEquals(401, account.first)
    }

    @Test
    fun messagesFolder403FallsBackToInbox() {
        fake.scenario = FakeScenario.MessagesFolder403
        val folders = raw("/api/berichten/mappen", "live-access")
        assertEquals(403, folders.first)
        val alle = raw("/api/berichten/mappen/alle", "live-access")
        assertEquals(403, alle.first)
        val inbox = raw("/api/berichten/mappen/1/berichten?top=40", "live-access")
        assertEquals(200, inbox.first)
        val parsed = MagisterParsers.parseMessages(inbox.second)
        assertTrue(parsed.isNotEmpty())
    }

    @Test
    fun citadelStyle403OnAlleStillServesOfficialPostvakin() {
        fake.scenario = FakeScenario.MessagesPostvakinOnly
        assertEquals(403, raw("/api/berichten/mappen/alle", "live-access").first)
        assertEquals(403, raw("/api/berichten/mappen/1/berichten?top=40", "live-access").first)
        val inbox = raw("/api/berichten/postvakin/berichten?top=40&skip=0", "live-access")
        assertEquals(200, inbox.first)
        assertTrue(MagisterParsers.parseMessages(inbox.second).isNotEmpty())
    }

    @Test
    fun messagesUnavailableStays403OnEveryInboxPath() {
        fake.scenario = FakeScenario.MessagesUnavailable
        MagisterEndpoints.messageInboxUrls("https://citadel.magister.net", 424242).forEach { url ->
            val path = url.substringAfter("https://citadel.magister.net")
            assertEquals(path, 403, raw(path, "live-access").first)
        }
        MagisterEndpoints.messageFolderListUrls("https://citadel.magister.net").forEach { url ->
            val path = url.substringAfter("https://citadel.magister.net")
            assertEquals(path, 403, raw(path, "live-access").first)
        }
    }

    @Test
    fun perSection403() {
        fake.scenario = FakeScenario.GradesForbidden
        assertEquals(403, raw("/api/personen/1/aanmeldingen/1/cijfers/overzicht", "live-access").first)
        fake.scenario = FakeScenario.AgendaForbidden
        assertEquals(403, raw("/api/personen/1/afspraken", "live-access").first)
        fake.scenario = FakeScenario.Happy
        assertEquals(200, raw("/api/personen/1/afspraken", "live-access").first)
    }

    private fun get(path: String, token: String): String {
        val result = raw(path, token)
        assertEquals("GET $path", 200, result.first)
        return result.second
    }

    private fun raw(path: String, token: String): Pair<Int, String> {
        val request = Request.Builder()
            .url(server.url(path))
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .get()
            .build()
        http.newCall(request).execute().use { response ->
            return response.code to response.body?.string().orEmpty()
        }
    }

    private fun postForm(path: String, fields: Map<String, String>): String {
        val result = rawForm(path, fields)
        assertEquals("POST $path", 200, result.first)
        return result.second
    }

    private fun rawForm(path: String, fields: Map<String, String>): Pair<Int, String> {
        val body = FormBody.Builder().also { builder ->
            fields.forEach { (k, v) -> builder.add(k, v) }
        }.build()
        val request = Request.Builder()
            .url(server.url(path))
            .post(body)
            .build()
        http.newCall(request).execute().use { response ->
            return response.code to response.body?.string().orEmpty()
        }
    }
}
