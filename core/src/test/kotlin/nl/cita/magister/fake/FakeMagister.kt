package nl.cita.magister.fake

import nl.cita.magister.fixtures.MockFixtures
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

enum class FakeScenario {
    Happy,
    MessagesFolder403,
    AccessExpired,
    RefreshExpired,
    GradesForbidden,
    AgendaForbidden,
}

/**
 * Lokale Magister-stand-in: fixtures + 401/403/refresh.
 * Geen echte school, geen nieuwe scopes.
 */
class FakeMagister(
    var scenario: FakeScenario = FakeScenario.Happy,
) : Dispatcher() {
    var accessToken: String = TOKEN_LIVE
    var refreshToken: String = TOKEN_REFRESH
    var tokenExchanges: Int = 0
    var refreshCalls: Int = 0

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.path?.substringBefore('?').orEmpty()
        val auth = request.getHeader("Authorization").orEmpty()
        val expiredAccess = scenario == FakeScenario.AccessExpired &&
            auth == "Bearer $TOKEN_EXPIRED" &&
            path != "/connect/token"

        if (expiredAccess) {
            return json(401, """{"error":"invalid_token","error_description":"expired"}""")
        }
        if (scenario == FakeScenario.RefreshExpired && path != "/connect/token" && auth.isNotBlank()) {
            return json(401, """{"error":"invalid_token"}""")
        }

        return when {
            request.method == "POST" && path == "/connect/token" -> token(request)
            path.endsWith("/challenges/tenant/search") -> json(
                200,
                """[{"Id":"1","Name":"Citadel College","Url":"https://citadel.magister.net"}]""",
            )
            path.endsWith("/api/versie") -> json(200, """{"ApiVersie":"1.0","core":"fake"}""")
            path.endsWith("/api/account") -> json(200, MockFixtures.accountJson)
            path.contains("/aanmeldingen") && !path.contains("cijfers") -> json(200, MockFixtures.enrollmentsJson)
            path.contains("/cijfers/") -> grades()
            path.contains("/afspraken") -> agenda()
            path.endsWith("/api/berichten/mappen") || path.endsWith("/api/berichten/mappen/alle") -> folders()
            path.contains("/berichten/mappen/1/berichten") || path.contains("mapId=1") ||
                path.endsWith("/api/berichten/berichten") || path.contains("/personen/") && path.endsWith("/berichten") ->
                json(200, MockFixtures.messagesJson)
            else -> json(404, """{"fouttype":"NotFound","path":"$path"}""")
        }
    }

    private fun token(request: RecordedRequest): MockResponse {
        val body = request.body.readUtf8()
        return when {
            body.contains("grant_type=refresh_token") -> {
                refreshCalls += 1
                if (scenario == FakeScenario.RefreshExpired || body.contains(TOKEN_REFRESH_DEAD)) {
                    json(400, """{"error":"invalid_grant","error_description":"refresh expired"}""")
                } else {
                    accessToken = TOKEN_LIVE
                    json(200, tokenJson(TOKEN_LIVE, TOKEN_REFRESH))
                }
            }
            body.contains("grant_type=authorization_code") -> {
                tokenExchanges += 1
                if (body.contains("code=good-code") || body.contains("code=abc")) {
                    json(200, tokenJson(TOKEN_LIVE, TOKEN_REFRESH))
                } else {
                    json(400, """{"error":"invalid_grant"}""")
                }
            }
            else -> json(400, """{"error":"unsupported_grant_type"}""")
        }
    }

    private fun grades(): MockResponse =
        if (scenario == FakeScenario.GradesForbidden) {
            json(403, """{"fouttype":"OnvoldoendePrivileges"}""")
        } else {
            json(200, MockFixtures.gradesJson)
        }

    private fun agenda(): MockResponse =
        if (scenario == FakeScenario.AgendaForbidden) {
            json(403, """{"fouttype":"OnvoldoendePrivileges"}""")
        } else {
            json(200, MockFixtures.appointmentsJson)
        }

    private fun folders(): MockResponse =
        if (scenario == FakeScenario.MessagesFolder403) {
            json(403, """{"fouttype":"OnvoldoendePrivileges"}""")
        } else {
            json(200, MockFixtures.foldersJson)
        }

    companion object {
        const val TOKEN_LIVE = "live-access"
        const val TOKEN_EXPIRED = "expired-access"
        const val TOKEN_REFRESH = "refresh-good"
        const val TOKEN_REFRESH_DEAD = "refresh-dead"

        fun start(scenario: FakeScenario = FakeScenario.Happy): Pair<MockWebServer, FakeMagister> {
            val fake = FakeMagister(scenario)
            val server = MockWebServer()
            server.dispatcher = fake
            server.start()
            return server to fake
        }

        private fun tokenJson(access: String, refresh: String): String =
            """{"access_token":"$access","refresh_token":"$refresh","token_type":"Bearer","expires_in":3600,"scope":"openid profile offline_access magister.mobile magister.ecs"}"""

        private fun json(code: Int, body: String): MockResponse =
            MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body)
    }
}
