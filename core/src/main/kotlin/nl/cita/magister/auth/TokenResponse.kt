package nl.cita.magister.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("id_token") val idToken: String? = null,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_in") val expiresIn: Long = 3600,
    @SerialName("scope") val scope: String? = null,
)

data class StoredSession(
    val accessToken: String,
    val refreshToken: String?,
    val idToken: String?,
    val expiresAtEpochMs: Long,
    val tenantHost: String,
    val personId: Int?,
    val enrollmentId: Int?,
    val displayName: String?,
) {
    fun isExpired(nowMs: Long = System.currentTimeMillis(), skewMs: Long = 60_000): Boolean {
        return nowMs + skewMs >= expiresAtEpochMs
    }
}
