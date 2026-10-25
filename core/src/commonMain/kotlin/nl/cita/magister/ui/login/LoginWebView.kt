package nl.cita.magister.ui.login

import androidx.compose.runtime.Composable

@Composable
expect fun LoginWebView(
    url: String,
    onRedirect: (String) -> Unit,
    onClose: () -> Unit,
)
