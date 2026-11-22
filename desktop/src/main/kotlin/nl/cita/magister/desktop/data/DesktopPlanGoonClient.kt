package nl.cita.magister.desktop.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.cita.magister.api.CitaHttp
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class DesktopPlanGoonClient {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
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
                client.newCall(builder.build()).execute().use { response ->
                    val code = response.code
                    if (code in 200..299) {
                        code
                    } else {
                        throw DesktopApiException(code, url, "Niet verstuurd.")
                    }
                }
            }
        }
}
