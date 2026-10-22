package nl.cita.magister.data

import androidx.test.core.app.ApplicationProvider
import nl.cita.magister.auth.StoredSession
import nl.cita.magister.auth.TokenStore
import nl.cita.magister.data.api.MagisterApiException
import nl.cita.magister.data.api.MagisterHttpClient
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HttpClientRefreshTest {
    private lateinit var server: MockWebServer
    private lateinit var tokens: TokenStore
    private var refreshHits = 0
    private var accountHits = 0

    @Before
    fun start() {
        tokens = TokenStore(ApplicationProvider.getApplicationContext())
        tokens.write(
            StoredSession(
                accessToken = "expired-access",
                refreshToken = "refresh-good",
                idToken = null,
                expiresAtEpochMs = 1L,
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

    @Test
    fun executeAuthedRefreshesOn401AndRetries() = runBlocking {
        val rewrite = Interceptor { chain ->
            val original = chain.request()
            val mapped: HttpUrl = original.url.newBuilder()
                .scheme("http")
                .host(server.hostName)
                .port(server.port)
                .build()
            chain.proceed(original.newBuilder().url(mapped).build())
        }
        val http = MagisterHttpClient(tokens, listOf(rewrite))
        val body = http.get("https://citadel.magister.net/api/account?noCache=0")
        assertTrue(body.contains("Jens"))
        assertEquals(2, accountHits)
        assertEquals(1, refreshHits)
        assertEquals("live-access", tokens.read()?.accessToken)
    }

    @Test
    fun executeAuthedSurfaces403() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                MockResponse().setResponseCode(403).setBody("""{"fouttype":"OnvoldoendePrivileges"}""")
        }
        val rewrite = Interceptor { chain ->
            val mapped = chain.request().url.newBuilder()
                .scheme("http").host(server.hostName).port(server.port).build()
            chain.proceed(chain.request().newBuilder().url(mapped).build())
        }
        val http = MagisterHttpClient(tokens, listOf(rewrite))
        try {
            http.get("https://citadel.magister.net/api/berichten/mappen/alle")
            error("expected 403")
        } catch (error: MagisterApiException) {
            assertEquals(403, error.code)
            assertTrue(error.path.contains("berichten"))
            assertEquals("Magister weigert deze data.", error.friendly())
        }
    }
}
