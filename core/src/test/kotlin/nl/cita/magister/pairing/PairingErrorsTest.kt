package nl.cita.magister.pairing

import java.net.ConnectException
import java.net.SocketTimeoutException
import nl.cita.magister.data.api.MagisterApiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingErrorsTest {
    @Test
    fun mapsNotLoggedInAndRefresh() {
        assertEquals(
            "Niet ingelogd. Log eerst in op je telefoon.",
            pairingErrorMessage(IllegalStateException("Niet ingelogd")),
        )
        assertEquals(
            "Token vernieuwen mislukt. Log opnieuw in op je telefoon.",
            pairingErrorMessage(IllegalStateException("Geen refresh-token")),
        )
        assertEquals(
            "Token vernieuwen mislukt. Log opnieuw in op je telefoon.",
            pairingErrorMessage(MagisterApiException(400, "https://accounts.magister.net/connect/token", "invalid_grant")),
        )
    }

    @Test
    fun mapsUnreachableDesktop() {
        val msg = pairingErrorMessage(ConnectException("Connection refused"))
        assertTrue(msg.contains("wifi"))
        assertEquals(
            "Desktop is niet bereikbaar op wifi. Zet pc en telefoon op hetzelfde netwerk.",
            pairingErrorMessage(SocketTimeoutException("timed out")),
        )
    }

    @Test
    fun expiredQrAndGenericStaySpecific() {
        assertEquals(
            "Deze QR-code is verlopen. Maak een nieuwe op de pc.",
            pairingErrorMessage(IllegalArgumentException("Deze QR-code is verlopen.")),
        )
        assertEquals(
            "Desktop weigerde de koppeling (401): bad secret",
            pairingErrorMessage(IllegalStateException("Desktop weigerde de koppeling (401): bad secret")),
        )
    }

    @Test
    fun genericApiFailureIsNotShownAsPairingError() {
        val generic = "Kon niet laden."
        assertFalse(pairingErrorMessage(IllegalStateException("token missing")).contains(generic))
    }
}
