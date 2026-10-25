package nl.cita.magister.auth

import nl.cita.magister.api.MagisterEndpoints
import nl.cita.magister.api.Tenant
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

data class AuthRequest(
    val authorizeUrl: String,
    val codeVerifier: String,
    val state: String,
    val nonce: String,
    val tenantHost: String,
)

object AuthRequestFactory {
    fun create(
        tenantHost: String,
        pkce: PkcePair = Pkce.generate(),
        state: String = UUID.randomUUID().toString(),
        nonce: String = UUID.randomUUID().toString(),
        tenantId: String? = null,
    ): AuthRequest {
        val host = Tenant.normalize(tenantHost)
        val acr = when {
            host.isNotBlank() -> "tenant:$host"
            !tenantId.isNullOrBlank() -> "tenant:$tenantId"
            else -> null
        }
        val params = linkedMapOf(
            "client_id" to MagisterEndpoints.CLIENT_ID,
            "redirect_uri" to MagisterEndpoints.REDIRECT_URI,
            "response_type" to MagisterEndpoints.RESPONSE_TYPE,
            "scope" to MagisterEndpoints.SCOPE,
            "state" to state,
            "nonce" to nonce,
            "code_challenge" to pkce.challenge,
            "code_challenge_method" to MagisterEndpoints.CODE_CHALLENGE_METHOD,
            "prompt" to "select_account",
        )
        if (acr != null) params["acr_values"] = acr
        val query = params.entries.joinToString("&") { (k, v) ->
            "${enc(k)}=${enc(v)}"
        }
        return AuthRequest(
            authorizeUrl = "${MagisterEndpoints.AUTHORIZE}?$query",
            codeVerifier = pkce.verifier,
            state = state,
            nonce = nonce,
            tenantHost = host,
        )
    }

    fun extractCode(redirectUrl: String): String? {
        val normalized = redirectUrl.replace('#', '?')
        if (!normalized.startsWith(MagisterEndpoints.REDIRECT_URI.trimEnd('/'))) return null
        val query = normalized.substringAfter('?', "")
        return query.split('&').mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq <= 0) return@mapNotNull null
            val key = part.substring(0, eq)
            val value = part.substring(eq + 1)
            key to java.net.URLDecoder.decode(value, StandardCharsets.UTF_8)
        }.toMap()["code"]
    }

    fun extractError(redirectUrl: String): String? {
        val normalized = redirectUrl.replace('#', '?')
        val query = normalized.substringAfter('?', "")
        val params = query.split('&').mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq <= 0) return@mapNotNull null
            part.substring(0, eq) to java.net.URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8)
        }.toMap()
        val error = params["error"] ?: return null
        val description = params["error_description"]
        return if (description.isNullOrBlank()) error else "$error: $description"
    }

    fun tokenForm(code: String, codeVerifier: String): Map<String, String> = mapOf(
        "grant_type" to "authorization_code",
        "code" to code,
        "redirect_uri" to MagisterEndpoints.REDIRECT_URI,
        "client_id" to MagisterEndpoints.CLIENT_ID,
        "code_verifier" to codeVerifier,
    )

    fun refreshForm(refreshToken: String): Map<String, String> = mapOf(
        "grant_type" to "refresh_token",
        "refresh_token" to refreshToken,
        "client_id" to MagisterEndpoints.CLIENT_ID,
    )

    private fun enc(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
}
