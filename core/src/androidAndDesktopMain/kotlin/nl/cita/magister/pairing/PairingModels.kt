package nl.cita.magister.pairing

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Base64
import kotlinx.serialization.Serializable
import nl.cita.magister.parse.MagisterJson

const val PAIRING_SCHEME = "mags1:"
const val PAIRING_TTL_MS = 120_000L

@Serializable
data class PairingOffer(
    val v: Int = 1,
    val ip: String,
    val port: Int,
    val secret: String,
    val pk: String,
    val exp: Long,
) {
    fun encode(): String = PAIRING_SCHEME + MagisterJson.instance.encodeToString(serializer(), this)

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = now >= exp

    companion object {
        fun parse(raw: String): PairingOffer? {
            val text = raw.trim()
            val json = when {
                text.startsWith(PAIRING_SCHEME) -> text.removePrefix(PAIRING_SCHEME)
                text.startsWith("{") -> text
                else -> return null
            }
            return runCatching {
                MagisterJson.instance.decodeFromString(serializer(), json)
            }.getOrNull()
        }
    }
}

@Serializable
data class PairingSession(
    val accessToken: String,
    val refreshToken: String,
    val idToken: String? = null,
    val expiresAtEpochMs: Long = 0L,
    val tenantHost: String,
    val schoolName: String = "",
    val personId: Int? = null,
    val enrollmentId: Int? = null,
    val displayName: String? = null,
)

@Serializable
data class PairingWire(
    val pk: String,
    val nonce: String,
    val ct: String,
)

object LanAddresses {
    fun isPrivateLan(address: InetAddress): Boolean {
        if (address.isLoopbackAddress) return true
        if (address.isSiteLocalAddress) return true
        if (address.isLinkLocalAddress) return true
        val host = address.hostAddress ?: return false
        return isPrivateLanIp(host)
    }

    fun isPrivateLanIp(host: String): Boolean {
        val ip = host.substringBefore('%')
        if (ip == "127.0.0.1" || ip == "::1" || ip == "localhost") return true
        val parts = ip.split('.')
        if (parts.size != 4) return false
        val a = parts[0].toIntOrNull() ?: return false
        val b = parts[1].toIntOrNull() ?: return false
        return when (a) {
            10 -> true
            172 -> b in 16..31
            192 -> b == 168
            169 -> b == 254
            127 -> true
            else -> false
        }
    }

    fun firstLanIpv4(): String? {
        val nics = runCatching { NetworkInterface.getNetworkInterfaces()?.toList().orEmpty() }
            .getOrDefault(emptyList())
        for (nic in nics) {
            if (!nic.isUp || nic.isLoopback) continue
            for (addr in nic.inetAddresses) {
                if (addr is Inet4Address && isPrivateLan(addr) && !addr.isLoopbackAddress) {
                    return addr.hostAddress
                }
            }
        }
        return null
    }
}

fun b64url(bytes: ByteArray): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

fun unb64url(text: String): ByteArray = Base64.getUrlDecoder().decode(text)
