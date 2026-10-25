package nl.cita.magister.pairing

expect fun startPairingHost(onSession: (PairingSession) -> Unit): PairingOffer

expect fun stopPairingHost()
