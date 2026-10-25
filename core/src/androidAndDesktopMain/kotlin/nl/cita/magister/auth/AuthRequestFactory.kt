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

    /**
     * Magister IdentityServer advertises query/fragment/form_post. Hybrid
     * (`id_token code`) defaults to form_post, which POSTs `code`/`state` to
     * `m6loapp://` with no query string. Desktop asks for query so a GET
     * `?code=` can be caught by the WebView or a protocol handler.
     * Android keeps the factory URL unchanged.
     */
    fun withResponseMode(authorizeUrl: String, mode: String): String {
        if (authorizeUrl.contains("response_mode=")) return authorizeUrl
        val sep = if (authorizeUrl.contains('?')) '&' else '?'
        return "$authorizeUrl${sep}response_mode=${enc(mode)}"
    }

    fun parseOAuthParams(source: String): Map<String, String> {
        val trimmed = source.trim()
        if (trimmed.isEmpty()) return emptyMap()
        val query = when {
            trimmed.contains('#') -> trimmed.replace('#', '?').substringAfter('?', "")
            trimmed.contains('?') -> trimmed.substringAfter('?', "")
            else -> trimmed
        }
        if (query.isEmpty() || '=' !in query) return emptyMap()
        return query.split('&').mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq <= 0) return@mapNotNull null
            val key = part.substring(0, eq)
            val value = part.substring(eq + 1).replace('+', ' ')
            key to java.net.URLDecoder.decode(value, StandardCharsets.UTF_8)
        }.toMap()
    }

    fun redirectFromFormPost(action: String, formBody: String): String? {
        val params = parseOAuthParams(formBody).ifEmpty { parseOAuthParams(action) }
        if (params["code"].isNullOrBlank() && params["error"].isNullOrBlank()) return null
        val actionOk = action.isBlank() ||
            action.startsWith(MagisterEndpoints.REDIRECT_URI.trimEnd('/')) ||
            action.startsWith("m6loapp:")
        if (!actionOk) return null
        val query = params.entries.joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }
        return "${MagisterEndpoints.REDIRECT_URI}?$query"
    }

    fun extractCode(redirectUrl: String): String? {
        val normalized = redirectUrl.replace('#', '?')
        val fromRedirect = normalized.startsWith(MagisterEndpoints.REDIRECT_URI.trimEnd('/'))
        val fromFormBody = !normalized.contains("://") && "code=" in normalized
        if (!fromRedirect && !fromFormBody) return null
        return parseOAuthParams(normalized)["code"]
    }

    fun extractError(redirectUrl: String): String? {
        val params = parseOAuthParams(redirectUrl)
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
