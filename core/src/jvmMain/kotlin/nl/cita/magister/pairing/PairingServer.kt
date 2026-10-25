package nl.cita.magister.pairing

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.security.KeyPair
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import nl.cita.magister.parse.MagisterJson
import nl.cita.magister.platform.MagsLog

class PairingServer(
    private val onSession: (PairingSession) -> Unit,
) {
    private var server: HttpServer? = null
    private var keys: KeyPair? = null
    private var secret: String? = null
    private val consumed = AtomicBoolean(false)
    var offer: PairingOffer? = null
        private set

    fun start(): PairingOffer {
        stop()
        consumed.set(false)
        val ip = LanAddresses.firstLanIpv4() ?: "127.0.0.1"
        val pair = PairingCrypto.generateKeyPair()
        val oneTime = PairingCrypto.randomSecret()
        keys = pair
        secret = oneTime
        val http = HttpServer.create(InetSocketAddress("0.0.0.0", 0), 0)
        http.createContext("/pair", ::handlePair)
        http.createContext("/", ::handleRoot)
        http.executor = Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "mags-pair").apply { isDaemon = true }
        }
        http.start()
        server = http
        val port = http.address.port
        val created = PairingOffer(
            ip = ip,
            port = port,
            secret = oneTime,
            pk = PairingCrypto.encodePublic(pair.public),
            exp = System.currentTimeMillis() + PAIRING_TTL_MS,
        )
        offer = created
        MagsLog.info("pairing listen $ip:$port exp=${created.exp}")
        return created
    }

    fun stop() {
        runCatching { server?.stop(0) }
        server = null
        keys = null
        secret = null
        offer = null
    }

    private fun handleRoot(exchange: com.sun.net.httpserver.HttpExchange) {
        val remote = exchange.remoteAddress.address
        if (remote == null || !LanAddresses.isPrivateLan(remote)) {
            exchange.sendResponseHeaders(403, -1)
            exchange.close()
            return
        }
        val bytes = "mags-pair".toByteArray()
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun handlePair(exchange: com.sun.net.httpserver.HttpExchange) {
        try {
            if (exchange.requestMethod != "POST") {
                exchange.sendResponseHeaders(405, -1)
                return
            }
            val remote = exchange.remoteAddress.address
            if (remote == null || !LanAddresses.isPrivateLan(remote)) {
                MagsLog.info("pairing reject non-LAN ${remote?.hostAddress}")
                exchange.sendResponseHeaders(403, -1)
                return
            }
            val current = offer
            val privateKey = keys?.private
            val expected = secret
            if (current == null || privateKey == null || expected == null || current.isExpired()) {
                exchange.sendResponseHeaders(410, -1)
                return
            }
            val headerSecret = exchange.requestHeaders.getFirst("X-Mags-Pair").orEmpty()
            if (!constantTimeEquals(headerSecret, expected)) {
                exchange.sendResponseHeaders(401, -1)
                return
            }
            if (!consumed.compareAndSet(false, true)) {
                exchange.sendResponseHeaders(409, -1)
                return
            }
            val body = exchange.requestBody.readBytes().decodeToString()
            val wire = MagisterJson.instance.decodeFromString(PairingWire.serializer(), body)
            val plain = PairingCrypto.open(privateKey, expected, wire)
            val session = MagisterJson.instance.decodeFromString(PairingSession.serializer(), plain.decodeToString())
            if (session.refreshToken.isBlank() || session.tenantHost.isBlank()) {
                consumed.set(false)
                exchange.sendResponseHeaders(400, -1)
                return
            }
            MagsLog.info("pairing session accepted tenant=${session.tenantHost}")
            exchange.sendResponseHeaders(204, -1)
            onSession(session)
        } catch (error: Exception) {
            consumed.set(false)
            MagsLog.error("pairing handler failed", error)
            runCatching { exchange.sendResponseHeaders(400, -1) }
        } finally {
            exchange.close()
        }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        val left = a.toByteArray()
        val right = b.toByteArray()
        if (left.size != right.size) return false
        var diff = 0
        for (i in left.indices) diff = diff or (left[i].toInt() xor right[i].toInt())
        return diff == 0
    }
}
