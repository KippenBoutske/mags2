package nl.cita.magister.api

import nl.cita.magister.auth.AuthRequestFactory
import nl.cita.magister.auth.Pkce
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CitaHttpHeadersTest {
    @Test
    fun everyStaticHeaderValueIsAscii() {
        assertTrue(CitaHttp.staticHeaderValues.isNotEmpty())
        CitaHttp.staticHeaderValues.forEach { value ->
            assertTrue("non-ASCII header value: $value", CitaHttp.isAscii(value))
        }
        CitaHttp.staticHeaders.forEach { (name, value) ->
            assertTrue("non-ASCII header name: $name", CitaHttp.isAscii(name))
            assertTrue("non-ASCII $name: $value", CitaHttp.isAscii(value))
        }
        assertTrue(CitaHttp.isAscii(MagisterEndpoints.SCOPE))
        assertTrue(CitaHttp.isAscii(MagisterEndpoints.CLIENT_ID))
        assertTrue(CitaHttp.isAscii(MagisterEndpoints.REDIRECT_URI))
    }

    @Test
    fun userAgentIsTheAsciiStringOkHttpMustAccept() {
        assertEquals("Mags/0.2.0 (unofficial Magister client)", CitaHttp.USER_AGENT)
        assertFalse(CitaHttp.USER_AGENT.contains("ë"))
        assertFalse(CitaHttp.USER_AGENT.contains("officiële"))
    }

    @Test
    fun okhttpAcceptsEveryHeaderTheAppSends() {
        val auth = CitaHttp.bearer("eyJhbGciOiJIUzI1NiJ9.e30.sig")
        val values = CitaHttp.staticHeaders + ("Authorization" to auth)
        val request = Request.Builder()
            .url("https://citadel.magister.net/api/account")
            .apply { values.forEach { (name, value) -> header(name, value) } }
            .get()
            .build()
        values.forEach { (name, value) ->
            assertEquals(value, request.header(name))
            assertTrue(CitaHttp.isAscii(request.header(name)!!))
        }
        Request.Builder()
            .url(MagisterEndpoints.TOKEN)
            .header("User-Agent", CitaHttp.USER_AGENT)
            .header("Accept", CitaHttp.ACCEPT_JSON)
            .post(FormBody.Builder().add("grant_type", "authorization_code").build())
            .build()
        Request.Builder()
            .url("https://example.com/webhook")
            .header("Authorization", auth)
            .header("Content-Type", CitaHttp.CONTENT_TYPE_JSON)
            .header("Accept", CitaHttp.ACCEPT_JSON)
            .post("{}".toRequestBody())
            .build()
    }

    @Test
    fun tokenAndAuthorizeValuesUsedOnLoginAreAscii() {
        val request = AuthRequestFactory.create(
            Tenant.CITADEL_HOST,
            pkce = Pkce.generate(),
            state = "state-1",
            nonce = "nonce-1",
        )
        assertTrue(CitaHttp.isAscii(request.authorizeUrl))
        assertTrue(request.authorizeUrl.startsWith(MagisterEndpoints.AUTHORIZE))
        assertTrue(request.authorizeUrl.contains("acr_values=tenant%3Acitadel.magister.net"))
        assertTrue(request.authorizeUrl.contains("redirect_uri=m6loapp%3A%2F%2Foauth2redirect%2F"))
        assertTrue(request.authorizeUrl.contains("magister.mobile"))
        assertTrue(request.authorizeUrl.contains("magister.ecs"))
        assertTrue(request.authorizeUrl.contains("offline_access"))
        assertFalse(request.authorizeUrl.contains("opp.read"))
        assertEquals(
            "openid profile offline_access magister.mobile magister.ecs",
            MagisterEndpoints.SCOPE,
        )
        AuthRequestFactory.tokenForm("code-value", request.codeVerifier).forEach { (k, v) ->
            assertTrue("$k=$v", CitaHttp.isAscii(k) && CitaHttp.isAscii(v))
        }
        AuthRequestFactory.refreshForm("refresh-token").forEach { (k, v) ->
            assertTrue("$k=$v", CitaHttp.isAscii(k) && CitaHttp.isAscii(v))
        }
        assertEquals(Tenant.CITADEL_HOST, request.tenantHost)
        assertEquals("m6loapp://oauth2redirect/", MagisterEndpoints.REDIRECT_URI)
    }

    @Test(expected = IllegalArgumentException::class)
    fun bearerRejectsNonAsciiToken() {
        CitaHttp.bearer("abcëdef")
    }
}
