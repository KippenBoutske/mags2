package nl.cita.magister.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.cita.magister.api.CitaHttp
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class PlanGoonClient {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            // Geen header-logging: de bearer-key mag nooit in logs.
            chain.proceed(chain.request())
        }
        .build()

    suspend fun post(url: String, bearerKey: String, jsonBody: String): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(url.startsWith("https://")) { "Alleen https." }
                val builder = Request.Builder()
                    .url(url)
                    .header("Content-Type", CitaHttp.CONTENT_TYPE_JSON)
                    .header("Accept", CitaHttp.ACCEPT_JSON)
                    .header("User-Agent", CitaHttp.USER_AGENT)
                    .post(jsonBody.toRequestBody(jsonMedia))
                if (bearerKey.isNotBlank()) {
                    builder.header("Authorization", CitaHttp.bearer(bearerKey))
                }
                val request = builder.build()
                client.newCall(request).execute().use { response ->
                    val code = response.code
                    if (code == 200) {
                        code
                    } else {
                        throw MagisterApiException(code, url, "Niet verstuurd.")
                    }
                }
            }
        }
}
