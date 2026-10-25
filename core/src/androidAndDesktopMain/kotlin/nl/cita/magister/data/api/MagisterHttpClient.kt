package nl.cita.magister.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import nl.cita.magister.api.CitaHttp
import nl.cita.magister.api.MagisterEndpoints
import nl.cita.magister.auth.AuthRequestFactory
import nl.cita.magister.auth.TokenResponse
import nl.cita.magister.auth.TokenStore
import nl.cita.magister.dto.TenantSearchHit
import nl.cita.magister.parse.MagisterJson

class MagisterHttpClient(
    private val tokenStore: TokenStore,
    private val rewriteUrl: (String) -> String = { it },
    engine: HttpClientEngine? = null,
) {
    private val refreshLock = Mutex()

    val http: HttpClient = HttpClient(engine ?: CIO.create()) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 25_000
            requestTimeoutMillis = 25_000
            socketTimeoutMillis = 25_000
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    suspend fun exchangeCode(code: String, verifier: String): TokenResponse =
        postForm(MagisterEndpoints.TOKEN, AuthRequestFactory.tokenForm(code, verifier))

    suspend fun refresh(refreshToken: String): TokenResponse =
        postForm(MagisterEndpoints.TOKEN, AuthRequestFactory.refreshForm(refreshToken))

    suspend fun get(url: String): String = executeAuthed(HttpMethod.Get, url)

    suspend fun putJson(url: String, json: String): String =
        executeAuthed(HttpMethod.Put, url, json)

    suspend fun patchJson(url: String, json: String): String =
        executeAuthed(HttpMethod.Patch, url, json)

    suspend fun postJson(url: String, json: String): String =
        executeAuthed(HttpMethod.Post, url, json)

    suspend fun searchTenants(query: String): List<TenantSearchHit> {
        val url = MagisterEndpoints.tenantSearch(query)
        val text = execute(HttpMethod.Get, url, authed = false)
        return MagisterJson.instance.decodeFromString(ListSerializer(TenantSearchHit.serializer()), text)
    }

    suspend fun tenantAlive(host: String): Boolean {
        val url = MagisterEndpoints.version(host)
        return runCatching {
            val client = HttpClient(CIO) {
                expectSuccess = false
                install(HttpTimeout) {
                    connectTimeoutMillis = 4_000
                    requestTimeoutMillis = 4_000
                    socketTimeoutMillis = 4_000
                }
            }
            try {
                val response = client.request(rewriteUrl(url)) {
                    method = HttpMethod.Get
                    header(HttpHeaders.Accept, CitaHttp.ACCEPT_JSON)
                    header(HttpHeaders.UserAgent, CitaHttp.USER_AGENT)
                }
                response.status.value in 200..299 && response.bodyAsText().contains("ApiVersie")
            } finally {
                client.close()
            }
        }.getOrDefault(false)
    }

    private suspend fun postForm(url: String, fields: Map<String, String>): TokenResponse {
        val text = execute(
            method = HttpMethod.Post,
            url = url,
            form = fields,
            authed = false,
        )
        return MagisterJson.instance.decodeFromString(TokenResponse.serializer(), text)
    }

    private suspend fun executeAuthed(
        method: HttpMethod,
        url: String,
        json: String? = null,
    ): String {
        if (tokenStore.expiringSoon()) {
            runCatching { refreshIfNeeded() }
        }
        val first = raw(method, url, json = json, authed = true)
        if (first.status != 401) {
            if (first.status !in 200..299) {
                throw MagisterApiException(first.status, url, first.body.ifBlank { first.message })
            }
            return first.body
        }
        refreshIfNeeded()
        val retry = raw(method, url, json = json, authed = true)
        if (retry.status !in 200..299) {
            throw MagisterApiException(retry.status, url, retry.body.ifBlank { retry.message })
        }
        return retry.body
    }

    private suspend fun execute(
        method: HttpMethod,
        url: String,
        json: String? = null,
        form: Map<String, String>? = null,
        authed: Boolean,
    ): String {
        val result = raw(method, url, json = json, form = form, authed = authed)
        if (result.status !in 200..299) {
            throw MagisterApiException(result.status, url, result.body.ifBlank { result.message })
        }
        return result.body
    }

    private suspend fun raw(
        method: HttpMethod,
        url: String,
        json: String? = null,
        form: Map<String, String>? = null,
        authed: Boolean,
    ): RawResponse {
        val mapped = rewriteUrl(url)
        val parsed = URLBuilder().takeFrom(url)
        val host = parsed.host
        val path = "/" + url.substringAfter("://").substringAfter('/', missingDelimiterValue = "")
        val skipAuth = host == "accounts.magister.net" &&
            (path.contains("token") || path.contains("challenges"))
        val isPublicProbe = path.contains("/api/versie")
        val session = tokenStore.read()
        val response = http.request(mapped) {
            this.method = method
            header(HttpHeaders.Accept, CitaHttp.ACCEPT_JSON)
            header(HttpHeaders.UserAgent, CitaHttp.USER_AGENT)
            if (authed && session != null && !skipAuth && !isPublicProbe) {
                header(HttpHeaders.Authorization, CitaHttp.bearer(session.accessToken))
            }
            if (host.endsWith("magister.net") && host != "accounts.magister.net") {
                header("X-API-Client-ID", CitaHttp.API_CLIENT_ID)
            }
            when {
                form != null -> {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                form.forEach { (k, v) -> append(k, v) }
                            },
                        ),
                    )
                }
                json != null -> {
                    contentType(ContentType.Application.Json)
                    setBody(json)
                }
            }
        }
        return RawResponse(
            status = response.status.value,
            body = response.bodyAsText(),
            message = response.status.description,
        )
    }

    private suspend fun refreshIfNeeded() {
        refreshLock.withLock {
            val session = tokenStore.read() ?: throw MagisterApiException(401, "session", "Niet ingelogd")
            val refresh = session.refreshToken ?: throw MagisterApiException(401, "session", "Geen refresh-token")
            val tokens = refresh(refresh)
            tokenStore.updateTokens(tokens, session)
        }
    }

    fun close() = http.close()

    private data class RawResponse(
        val status: Int,
        val body: String,
        val message: String,
    )
}

class MagisterApiException(
    val code: Int,
    val url: String,
    val body: String,
) : Exception("${shortPath(url)} HTTP $code: ${body.take(240)}") {
    val path: String get() = shortPath(url)

    fun friendly(): String = when (code) {
        401 -> "Sessie verlopen. Log opnieuw in."
        403 -> "Magister weigert deze data."
        404 -> "Niet gevonden."
        else -> "Kon niet laden."
    }

    fun summary(): String = "$path HTTP $code"

    companion object {
        fun shortPath(url: String): String {
            val noScheme = url.substringAfter("://", url)
            val path = noScheme.substringAfter('/', missingDelimiterValue = noScheme)
            return "/${path.substringBefore('?')}"
        }
    }
}
