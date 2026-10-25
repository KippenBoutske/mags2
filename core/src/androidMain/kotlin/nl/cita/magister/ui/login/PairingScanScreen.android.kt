package nl.cita.magister.ui.login

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import nl.cita.magister.pairing.LanAddresses
import nl.cita.magister.pairing.PairingOffer
import nl.cita.magister.platform.AndroidHooks

private const val CAPTURE_ACTIVITY = "nl.cita.magister.pairing.PairingCaptureActivity"

@Composable
actual fun PairingScanScreen(
    onOffer: (PairingOffer) -> Unit,
    onClose: () -> Unit,
    busy: Boolean,
    error: String?,
) {
    val context = LocalContext.current
    var pasted by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<PairingOffer?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }

    fun acceptRaw(raw: String) {
        val offer = PairingOffer.parse(raw)
        when {
            offer == null -> scanError = "Geen Mags-koppelcode."
            offer.isExpired() -> scanError = "Deze QR-code is verlopen."
            !LanAddresses.isPrivateLanIp(offer.ip) ->
                scanError = "Alleen een privé-LAN-adres is toegestaan."
            else -> {
                scanError = null
                pending = offer
            }
        }
    }

    DisposableEffect(Unit) {
        AndroidHooks.pairingScanResult = { raw -> acceptRaw(raw) }
        onDispose { AndroidHooks.pairingScanResult = null }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Sluiten")
        }
        Text("Koppel desktop", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Scan de QR-code op je pc, of plak de koppelcode. " +
                "Telefoon en pc moeten op hetzelfde wifi zitten.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = {
                runCatching { context.startPairingCapture() }
                    .onFailure { scanError = it.message ?: "Camera kon niet starten." }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
        ) {
            Text("Camera openen")
        }
        OutlinedTextField(
            value = pasted,
            onValueChange = { pasted = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Koppelcode") },
            minLines = 3,
        )
        TextButton(
            onClick = { acceptRaw(pasted) },
            enabled = pasted.isNotBlank() && !busy,
        ) {
            Text("Code gebruiken")
        }
        if (busy) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            Text("Sessie wordt gestuurd…")
        }
        val shown = error ?: scanError
        if (shown != null) {
            Text(shown, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }

    val offer = pending
    if (offer != null && !busy) {
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Desktop koppelen?") },
            text = {
                Text(
                    "Sessie naar ${offer.ip}:${offer.port} sturen. " +
                        "Als Magister refresh-tokens roteert, kan één van de twee apparaten " +
                        "later opnieuw moeten inloggen.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val chosen = offer
                        pending = null
                        onOffer(chosen)
                    },
                ) { Text("Koppelen") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Annuleren") }
            },
        )
    }
}

private fun Context.startPairingCapture() {
    val intent = Intent().setClassName(packageName, CAPTURE_ACTIVITY)
    val activity = findActivity()
    if (activity != null) {
        activity.startActivity(intent)
    } else {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
