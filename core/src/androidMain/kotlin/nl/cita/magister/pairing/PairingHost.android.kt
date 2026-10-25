package nl.cita.magister.pairing

actual fun startPairingHost(onSession: (PairingSession) -> Unit): PairingOffer {
    error("Desktop-koppeling hosten kan alleen op de pc.")
}

actual fun stopPairingHost() = Unit
