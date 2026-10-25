package nl.cita.magister.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import nl.cita.magister.api.Tenant
import java.util.Base64

/** Leest claims uit een Magister id_token zonder de handtekening te verifiëren. */
object IdTokenClaims {
    private val json = Json { ignoreUnknownKeys = true }

    fun payload(idToken: String): JsonObject? {
        val parts = idToken.split('.')
        if (parts.size < 2) return null
        return runCatching {
            val padded = padBase64(parts[1])
            val raw = String(Base64.getUrlDecoder().decode(padded), Charsets.UTF_8)
            json.parseToJsonElement(raw).jsonObject
        }.getOrNull()
    }

    fun tenantHost(idToken: String): String? {
        val obj = payload(idToken) ?: return null
        val raw = obj.string("urn:magister:claims:iam:tenant")
            ?: obj.string("tenant")
            ?: return null
        return Tenant.normalize(raw).takeIf { it.isNotBlank() }
    }

    fun givenName(idToken: String): String? {
        val obj = payload(idToken) ?: return null
        return obj.string("given_name")
            ?: obj.string("nickname")
            ?: obj.string("name")?.substringBefore(' ')
    }

    fun familyName(idToken: String): String? {
        val obj = payload(idToken) ?: return null
        return obj.string("family_name")
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun padBase64(value: String): String {
        val pad = (4 - value.length % 4) % 4
        return value + "=".repeat(pad)
    }
}
