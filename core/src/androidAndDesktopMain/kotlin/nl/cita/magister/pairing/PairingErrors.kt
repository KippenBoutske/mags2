package nl.cita.magister.pairing

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import nl.cita.magister.data.api.MagisterApiException

fun pairingErrorMessage(error: Throwable): String {
    val msg = error.message.orEmpty()
    return when {
        looksLikeNotLoggedIn(error) ->
            "Niet ingelogd. Log eerst in op je telefoon."
        looksLikeRefreshFailure(error) ->
            "Token vernieuwen mislukt. Log opnieuw in op je telefoon."
        msg.contains("verlopen", ignoreCase = true) && msg.contains("QR", ignoreCase = true) ->
            "Deze QR-code is verlopen. Maak een nieuwe op de pc."
        msg.contains("privé-LAN", ignoreCase = true) || msg.contains("private LAN", ignoreCase = true) ->
            "Alleen een privé-LAN-adres is toegestaan."
        looksLikeUnreachable(error) ->
            "Desktop is niet bereikbaar op wifi. Zet pc en telefoon op hetzelfde netwerk."
        msg.contains("weigerde", ignoreCase = true) ->
            msg
        msg.isNotBlank() -> msg
        else -> "Koppelen mislukt."
    }
}

private fun looksLikeNotLoggedIn(error: Throwable): Boolean {
    val msg = error.message.orEmpty()
    return msg.contains("Niet ingelogd", ignoreCase = true) ||
        (error is MagisterApiException && error.code == 401 && error.path.contains("session"))
}

private fun looksLikeRefreshFailure(error: Throwable): Boolean {
    if (error is MagisterApiException &&
        (error.path.contains("token", ignoreCase = true) || error.code == 400 || error.code == 401)
    ) {
        val body = error.body
        if (body.contains("refresh", ignoreCase = true) ||
            body.contains("invalid_grant", ignoreCase = true) ||
            error.path.contains("token", ignoreCase = true)
        ) {
            return true
        }
    }
    val msg = error.message.orEmpty()
    return msg.contains("refresh-token", ignoreCase = true) ||
        msg.contains("Geen refresh-token", ignoreCase = true) ||
        (msg.contains("refresh", ignoreCase = true) && msg.contains("token", ignoreCase = true))
}

private fun looksLikeUnreachable(error: Throwable): Boolean {
    var current: Throwable? = error
    while (current != null) {
        when (current) {
            is ConnectException,
            is SocketTimeoutException,
            is UnknownHostException,
            is UnresolvedAddressException,
            -> return true
        }
        val name = current::class.simpleName.orEmpty()
        if (name.contains("Timeout", ignoreCase = true) && name.contains("Connect", ignoreCase = true)) {
            return true
        }
        val msg = current.message.orEmpty()
        if (msg.contains("Failed to connect", ignoreCase = true) ||
            msg.contains("Connection refused", ignoreCase = true) ||
            msg.contains("No route to host", ignoreCase = true) ||
            msg.contains("unreachable", ignoreCase = true) ||
            msg.contains("timed out", ignoreCase = true)
        ) {
            return true
        }
        current = current.cause
    }
    return false
}
