package nl.cita.magister.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlinx.coroutines.delay
import nl.cita.magister.pairing.PAIRING_TTL_MS
import nl.cita.magister.pairing.PairingOffer
import nl.cita.magister.pairing.PairingQrImage

@Composable
fun PairingHostScreen(
    offer: PairingOffer,
    onClose: () -> Unit,
) {
    var remainingMs by remember(offer.exp) { mutableLongStateOf(max(0L, offer.exp - System.currentTimeMillis())) }
    LaunchedEffect(offer.exp) {
        while (remainingMs > 0L) {
            delay(250)
            remainingMs = max(0L, offer.exp - System.currentTimeMillis())
        }
    }
    val seconds = (remainingMs / 1000L).toInt()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Koppel met je telefoon") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Sluiten")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Scan deze code in de Android-app: Meer → Koppel desktop. " +
                    "Telefoon en pc moeten op hetzelfde wifi-netwerk zitten.",
                style = MaterialTheme.typography.bodyMedium,
            )
            PairingQrImage(offer.encode(), Modifier.fillMaxWidth(0.72f))
            Text(
                if (seconds > 0) "Geldig nog $seconds s · ${offer.ip}:${offer.port}"
                else "Verlopen. Start opnieuw.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "De QR verloopt na ${PAIRING_TTL_MS / 1000} seconden of na één gebruik.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onClose) { Text("Annuleren") }
        }
    }
}
