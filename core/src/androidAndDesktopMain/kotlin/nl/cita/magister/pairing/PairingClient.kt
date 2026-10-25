package nl.cita.magister.pairing

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import nl.cita.magister.parse.MagisterJson

object PairingClient {
    suspend fun send(offer: PairingOffer, session: PairingSession) {
        require(!offer.isExpired()) { "Deze QR-code is verlopen." }
        require(LanAddresses.isPrivateLanIp(offer.ip)) { "Alleen een privé-LAN-adres is toegestaan." }
        val desktopPublic = PairingCrypto.decodePublic(offer.pk)
        val plaintext = MagisterJson.instance.encodeToString(PairingSession.serializer(), session)
            .encodeToByteArray()
        val wire = PairingCrypto.seal(desktopPublic, offer.secret, plaintext)
        val json = MagisterJson.instance.encodeToString(PairingWire.serializer(), wire)
        val client = HttpClient(CIO) {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = 8_000
                requestTimeoutMillis = 12_000
            }
        }
        try {
            val response = client.post("http://${offer.ip}:${offer.port}/pair") {
                header("X-Mags-Pair", offer.secret)
                contentType(ContentType.Application.Json)
                setBody(json)
            }
            if (!response.status.isSuccess()) {
                val body = response.bodyAsText().take(200)
                error("Desktop weigerde de koppeling (${response.status.value}): $body")
            }
        } finally {
            client.close()
        }
    }
}
