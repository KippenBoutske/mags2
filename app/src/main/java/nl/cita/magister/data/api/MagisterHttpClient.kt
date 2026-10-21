package nl.cita.magister.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import nl.cita.magister.api.MagisterEndpoints
import nl.cita.magister.auth.AuthRequestFactory
import nl.cita.magister.auth.TokenResponse
import nl.cita.magister.auth.TokenStore
import nl.cita.magister.parse.MagisterJson
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class MagisterHttpClient(
    private val tokenStore: TokenStore,
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
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
            if (session != null && original.header("Authorization") == null) {
                builder.header("Authorization", "Bearer ${session.accessToken}")
            }
            chain.proceed(builder.build())
        }
        .build()

    suspend fun exchangeCode(code: String, verifier: String): TokenResponse =
        postForm(MagisterEndpoints.TOKEN, AuthRequestFactory.tokenForm(code, verifier))

    suspend fun refresh(refreshToken: String): TokenResponse =
        postForm(MagisterEndpoints.TOKEN, AuthRequestFactory.refreshForm(refreshToken))

    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val response = executeAuthed(
            Request.Builder().url(url).get().build(),
        )
        response
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

    private suspend fun postForm(url: String, fields: Map<String, String>): TokenResponse =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder().also { builder ->
                fields.forEach { (k, v) -> builder.add(k, v) }
            }.build()
            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw MagisterApiException(response.code, text.ifBlank { response.message })
                }
                MagisterJson.instance.decodeFromString(TokenResponse.serializer(), text)
            }
        }

    private suspend fun executeAuthed(request: Request): String {
        val first = client.newCall(request).execute()
        val firstBody = first.body?.string().orEmpty()
        if (first.code != 401) {
            if (!first.isSuccessful) throw MagisterApiException(first.code, firstBody.ifBlank { first.message })
            return firstBody
        }
        refreshIfNeeded()
        val retry = client.newCall(request).execute()
        val retryBody = retry.body?.string().orEmpty()
        if (!retry.isSuccessful) throw MagisterApiException(retry.code, retryBody.ifBlank { retry.message })
        return retryBody
    }

    private suspend fun refreshIfNeeded() {
        refreshLock.withLock {
            val session = tokenStore.read() ?: throw MagisterApiException(401, "Niet ingelogd")
            val refresh = session.refreshToken ?: throw MagisterApiException(401, "Geen refresh-token")
            val tokens = refresh(refresh)
            tokenStore.updateTokens(tokens, session)
        }
    }

    companion object {
        const val USER_AGENT = "Cita/0.1 (Android; onofficiële Magister-client)"
    }
}

class MagisterApiException(val code: Int, override val message: String) : Exception("HTTP $code: $message")
