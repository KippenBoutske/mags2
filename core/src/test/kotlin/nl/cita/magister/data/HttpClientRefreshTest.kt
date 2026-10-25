package nl.cita.magister.data

import kotlinx.coroutines.runBlocking
import nl.cita.magister.auth.StoredSession
import nl.cita.magister.auth.TokenStore
import nl.cita.magister.data.api.MagisterApiException
import nl.cita.magister.data.api.MagisterHttpClient
import nl.cita.magister.platform.InMemorySettingsStore
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HttpClientRefreshTest {
    private lateinit var server: MockWebServer
    private lateinit var tokens: TokenStore
    private var refreshHits = 0
    private var accountHits = 0

    @Before
    fun start() {
        tokens = TokenStore(InMemorySettingsStore())
        tokens.write(
            StoredSession(
                accessToken = "expired-access",
                refreshToken = "refresh-good",
                idToken = null,
                expiresAtEpochMs = System.currentTimeMillis() + 3_600_000L,
                tenantHost = "citadel.magister.net",
                personId = 424242,
                enrollmentId = 1,
                displayName = "Jens",
            ),
        )
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                if (request.method == "POST" && path.contains("/connect/token")) {
                    refreshHits += 1
                    return MockResponse()
                        .setResponseCode(200)
                        .setBody("""{"access_token":"live-access","refresh_token":"refresh-good","expires_in":3600}""")
                }
                if (path.contains("/api/account")) {
                    accountHits += 1
                    val auth = request.getHeader("Authorization").orEmpty()
                    return if (auth.contains("expired-access")) {
                        MockResponse().setResponseCode(401).setBody("""{"error":"expired"}""")
                    } else {
                        MockResponse().setResponseCode(200).setBody("""{"Persoon":{"Id":424242,"Roepnaam":"Jens"}}""")
                    }
                }
                return MockResponse().setResponseCode(404)
            }
        }
        server.start()
    }

    @After
    fun stop() {
        server.shutdown()
        tokens.clear()
    }

    private fun client(): MagisterHttpClient = MagisterHttpClient(
        tokenStore = tokens,
        rewriteUrl = { url ->
            val noScheme = url.substringAfter("://")
            val pathAndQuery = noScheme.substringAfter('/', missingDelimiterValue = "")
            "http://${server.hostName}:${server.port}/$pathAndQuery"
        },
    )

    @Test
    fun executeAuthedRefreshesOn401AndRetries() = runBlocking {
        val http = client()
        val body = http.get("https://citadel.magister.net/api/account?noCache=0")
        assertTrue(body.contains("Jens"))
        assertEquals(2, accountHits)
        assertEquals(1, refreshHits)
        assertEquals("live-access", tokens.read()?.accessToken)
        http.close()
    }

    @Test
    fun executeAuthedRefreshesWhenTokenIsAlmostExpired() = runBlocking {
        tokens.write(tokens.read()!!.copy(expiresAtEpochMs = System.currentTimeMillis() + 10_000L))
        val http = client()
        val body = http.get("https://citadel.magister.net/api/account?noCache=0")
        assertTrue(body.contains("Jens"))
        assertEquals(1, refreshHits)
        assertEquals(1, accountHits)
        assertEquals("live-access", tokens.read()?.accessToken)
        http.close()
    }

    @Test
    fun executeAuthedSurfaces403() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                MockResponse().setResponseCode(403).setBody("""{"fouttype":"OnvoldoendePrivileges"}""")
        }
        val http = client()
        try {
            http.get("https://citadel.magister.net/api/berichten/mappen/alle")
            error("expected 403")
        } catch (error: MagisterApiException) {
            assertEquals(403, error.code)
            assertTrue(error.path.contains("berichten"))
            assertEquals("Magister weigert deze data.", error.friendly())
        } finally {
            http.close()
        }
    }
}
