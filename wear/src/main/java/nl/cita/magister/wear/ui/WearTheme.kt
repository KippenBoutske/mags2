package nl.cita.magister.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val WearSand = Color(0xFFD8835D)
val WearInk = Color(0xFF17191D)
val WearBg = Color(0xFF000000)
val WearSurface = Color(0xFF1C2025)
val WearSurfaceHigh = Color(0xFF252A30)
val WearOn = Color(0xFFF1F0EB)
val WearMuted = Color(0xFFC7C5BD)
val WearSage = Color(0xFF7E8B6A)
val WearSageContainer = Color(0xFF2F3326)
val WearError = Color(0xFFF0B8AA)
val WearErrorContainer = Color(0xFF5C2B25)

private val MagsWearColors = Colors(
    primary = WearSand,
    primaryVariant = Color(0xFFC47A4A),
    secondary = WearSage,
    secondaryVariant = WearSageContainer,
    error = WearError,
    onPrimary = WearInk,
    onSecondary = WearInk,
    onError = WearInk,
    background = WearBg,
    onBackground = WearOn,
    surface = WearSurface,
    onSurface = WearOn,
    onSurfaceVariant = WearMuted,
)

@Composable
fun MagsWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = MagsWearColors,
        content = content,
    )
}
