package nl.cita.magister.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import nl.cita.magister.api.CitaHttp

class PlanGoonClient {
    private val client = HttpClient(CIO) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 20_000
            requestTimeoutMillis = 20_000
            socketTimeoutMillis = 20_000
        }
    }

    suspend fun post(url: String, bearerKey: String, jsonBody: String): Result<Int> =
        runCatching {
            require(url.startsWith("https://")) { "Alleen https." }
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, CitaHttp.ACCEPT_JSON)
                header(HttpHeaders.UserAgent, CitaHttp.USER_AGENT)
                if (bearerKey.isNotBlank()) {
                    header(HttpHeaders.Authorization, CitaHttp.bearer(bearerKey))
                }
                setBody(jsonBody)
            }
            val code = response.status.value
            if (code == 200) code else throw MagisterApiException(code, url, "Niet verstuurd.")
        }
}
