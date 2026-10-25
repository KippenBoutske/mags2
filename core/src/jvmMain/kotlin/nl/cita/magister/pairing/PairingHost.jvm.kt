package nl.cita.magister.pairing

@Volatile
private var active: PairingServer? = null

actual fun startPairingHost(onSession: (PairingSession) -> Unit): PairingOffer {
    stopPairingHost()
    val server = PairingServer(onSession)
    active = server
    return server.start()
}

actual fun stopPairingHost() {
    active?.stop()
    active = null
}
