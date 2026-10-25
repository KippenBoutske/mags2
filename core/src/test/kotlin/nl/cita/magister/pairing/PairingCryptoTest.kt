package nl.cita.magister.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingCryptoTest {
    @Test
    fun roundTripSealsSession() {
        val desktop = PairingCrypto.generateKeyPair()
        val secret = PairingCrypto.randomSecret()
        val session = PairingSession(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            tenantHost = "citadel.magister.net",
            schoolName = "Citadel",
            personId = 9,
        )
        val json = kotlinx.serialization.json.Json.encodeToString(PairingSession.serializer(), session)
        val wire = PairingCrypto.seal(desktop.public, secret, json.encodeToByteArray())
        val opened = PairingCrypto.open(desktop.private, secret, wire).decodeToString()
        val back = kotlinx.serialization.json.Json.decodeFromString(PairingSession.serializer(), opened)
        assertEquals(session, back)
    }

    @Test
    fun offerRoundTripAndExpiry() {
        val offer = PairingOffer(
            ip = "192.168.1.20",
            port = 41234,
            secret = "s",
            pk = "pk",
            exp = System.currentTimeMillis() + 5_000,
        )
        val parsed = PairingOffer.parse(offer.encode())
        assertNotNull(parsed)
        assertEquals(offer.ip, parsed!!.ip)
        assertEquals(offer.port, parsed.port)
        assertFalse(parsed.isExpired())
        assertTrue(offer.copy(exp = 1L).isExpired())
        assertNull(PairingOffer.parse("https://example.com"))
    }

    @Test
    fun onlyPrivateLanIps() {
        assertTrue(LanAddresses.isPrivateLanIp("10.0.0.8"))
        assertTrue(LanAddresses.isPrivateLanIp("192.168.1.5"))
        assertTrue(LanAddresses.isPrivateLanIp("172.16.4.1"))
        assertTrue(LanAddresses.isPrivateLanIp("127.0.0.1"))
        assertFalse(LanAddresses.isPrivateLanIp("8.8.8.8"))
        assertFalse(LanAddresses.isPrivateLanIp("1.1.1.1"))
        assertFalse(LanAddresses.isPrivateLanIp("172.32.0.1"))
    }
}
