package nl.cita.magister.ui.login

import androidx.compose.runtime.Composable
import nl.cita.magister.pairing.PairingOffer

@Composable
actual fun PairingScanScreen(
    onOffer: (PairingOffer) -> Unit,
    onClose: () -> Unit,
    busy: Boolean,
    error: String?,
) = Unit
