package nl.cita.magister.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import nl.cita.magister.api.CitaHttp
import nl.cita.magister.api.MagisterEndpoints
import nl.cita.magister.auth.AuthRequestFactory
import nl.cita.magister.auth.TokenResponse
import nl.cita.magister.auth.TokenStore
import nl.cita.magister.dto.TenantSearchHit
import nl.cita.magister.parse.MagisterJson
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class MagisterHttpClient(
    private val tokenStore: TokenStore,
    extraInterceptors: List<Interceptor> = emptyList(),
) {
    private val refreshLock = Mutex()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            },
        )
        .addInterceptor { chain ->
            val original = chain.request()
            val session = tokenStore.read()
            val builder = original.newBuilder()
                .header("Accept", CitaHttp.ACCEPT_JSON)
                .header("User-Agent", CitaHttp.USER_AGENT)
            val path = original.url.encodedPath
            val skipAuth = original.url.host == "accounts.magister.net" &&
                (path.contains("token") || path.contains("challenges"))
            val isPublicProbe = path.contains("/api/versie")
            if (session != null && original.header("Authorization") == null && !skipAuth && !isPublicProbe) {
                builder.header("Authorization", CitaHttp.bearer(session.accessToken))
            }
            if (original.url.host.endsWith("magister.net") && original.url.host != "accounts.magister.net") {
                builder.header("X-API-Client-ID", CitaHttp.API_CLIENT_ID)
            }
            chain.proceed(builder.build())
        }
        .apply { extraInterceptors.forEach(::addInterceptor) }
        .build()

    suspend fun exchangeCode(code: String, verifier: String): TokenResponse =
        postForm(MagisterEndpoints.TOKEN, AuthRequestFactory.tokenForm(code, verifier))

    suspend fun refresh(refreshToken: String): TokenResponse =
        postForm(MagisterEndpoints.TOKEN, AuthRequestFactory.refreshForm(refreshToken))

    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        executeAuthed(Request.Builder().url(url).get().build())
    }

    suspend fun putJson(url: String, json: String): String = withContext(Dispatchers.IO) {
        executeAuthed(
            Request.Builder()
                .url(url)
                .put(json.toRequestBody(jsonMedia))
                .build(),
        )
    }

    suspend fun patchJson(url: String, json: String): String = withContext(Dispatchers.IO) {
        executeAuthed(
            Request.Builder()
                .url(url)
                .patch(json.toRequestBody(jsonMedia))
                .build(),
        )
    }

    suspend fun postJson(url: String, json: String): String = withContext(Dispatchers.IO) {
        executeAuthed(
            Request.Builder()
                .url(url)
                .post(json.toRequestBody(jsonMedia))
                .build(),
        )
    }

    suspend fun searchTenants(query: String): List<TenantSearchHit> = withContext(Dispatchers.IO) {
        val url = MagisterEndpoints.tenantSearch(query)
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw MagisterApiException(response.code, url, text.ifBlank { response.message })
            }
            MagisterJson.instance.decodeFromString(ListSerializer(TenantSearchHit.serializer()), text)
        }
    }

    suspend fun tenantAlive(host: String): Boolean = withContext(Dispatchers.IO) {
        val url = MagisterEndpoints.version(host)
        val probe = client.newBuilder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .build()
        runCatching {
            probe.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                response.isSuccessful && response.body?.string().orEmpty().contains("ApiVersie")
            }
        }.getOrDefault(false)
    }

    private suspend fun postForm(url: String, fields: Map<String, String>): TokenResponse =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder().also { builder ->
                fields.forEach { (k, v) -> builder.add(k, v) }
            }.build()
            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("Accept", CitaHttp.ACCEPT_JSON)
                .header("User-Agent", CitaHttp.USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw MagisterApiException(response.code, url, text.ifBlank { response.message })
                }
                MagisterJson.instance.decodeFromString(TokenResponse.serializer(), text)
            }
        }

    private suspend fun executeAuthed(request: Request): String {
        val first = client.newCall(request).execute()
        val firstBody = first.body?.string().orEmpty()
        if (first.code != 401) {
            if (!first.isSuccessful) {
                throw MagisterApiException(first.code, request.url.toString(), firstBody.ifBlank { first.message })
            }
            return firstBody
        }
        refreshIfNeeded()
        val retry = client.newCall(request).execute()
        val retryBody = retry.body?.string().orEmpty()
        if (!retry.isSuccessful) {
            throw MagisterApiException(retry.code, request.url.toString(), retryBody.ifBlank { retry.message })
        }
        return retryBody
    }

    private suspend fun refreshIfNeeded() {
        refreshLock.withLock {
            val session = tokenStore.read() ?: throw MagisterApiException(401, "session", "Niet ingelogd")
            val refresh = session.refreshToken ?: throw MagisterApiException(401, "session", "Geen refresh-token")
            val tokens = refresh(refresh)
            tokenStore.updateTokens(tokens, session)
        }
    }
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
