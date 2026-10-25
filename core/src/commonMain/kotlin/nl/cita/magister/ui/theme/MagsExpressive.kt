package nl.cita.magister.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun MagsExpressiveTheme(
    colorScheme: ColorScheme,
    typography: Typography,
    content: @Composable () -> Unit,
)

@Composable
expect fun MagsLoadingIndicator(modifier: Modifier = Modifier)
