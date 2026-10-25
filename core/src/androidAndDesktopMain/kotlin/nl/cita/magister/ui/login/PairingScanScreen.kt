package nl.cita.magister.ui.login

import androidx.compose.runtime.Composable

@Composable
expect fun PairingScanScreen(
    onOffer: (nl.cita.magister.pairing.PairingOffer) -> Unit,
    onClose: () -> Unit,
    busy: Boolean,
    error: String?,
)
