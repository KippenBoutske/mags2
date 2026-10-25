package nl.cita.magister.auth

import java.security.MessageDigest
import java.security.SecureRandom

data class PkcePair(
    val verifier: String,
    val challenge: String,
)

object Pkce {
    private val unreserved = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~".toCharArray()

    fun generate(random: SecureRandom = SecureRandom()): PkcePair {
        val bytes = ByteArray(64)
        random.nextBytes(bytes)
        val verifier = buildString(64) {
            bytes.forEach { append(unreserved[(it.toInt() and 0xFF) % unreserved.size]) }
        }
        return PkcePair(verifier, challenge(verifier))
    }

    fun challenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return base64Url(digest)
    }

    fun base64Url(bytes: ByteArray): String {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
