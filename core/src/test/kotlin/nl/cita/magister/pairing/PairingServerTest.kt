package nl.cita.magister.pairing

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingServerTest {
    @Test
    fun localHttpPairingDeliversSession() {
        val latch = CountDownLatch(1)
        var received: PairingSession? = null
        val server = PairingServer { session ->
            received = session
            latch.countDown()
        }
        val lanOffer = server.start()
        val offer = lanOffer.copy(ip = "127.0.0.1", port = lanOffer.port)
        try {
            val session = PairingSession(
                accessToken = "acc",
                refreshToken = "ref",
                tenantHost = "citadel.magister.net",
                schoolName = "Citadel",
                personId = 3,
            )
            runBlocking { PairingClient.send(offer, session) }
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            assertEquals("ref", received?.refreshToken)
            assertEquals("citadel.magister.net", received?.tenantHost)
            assertEquals(3, received?.personId)
        } finally {
            server.stop()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun clientRejectsPublicIp() {
        val offer = PairingOffer(
            ip = "8.8.8.8",
            port = 9,
            secret = PairingCrypto.randomSecret(),
            pk = PairingCrypto.encodePublic(PairingCrypto.generateKeyPair().public),
            exp = System.currentTimeMillis() + 60_000,
        )
        val session = PairingSession(
            accessToken = "a",
            refreshToken = "r",
            tenantHost = "citadel.magister.net",
        )
        runBlocking { PairingClient.send(offer, session) }
    }
}
