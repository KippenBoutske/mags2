package nl.cita.magister.haptics

import androidx.compose.runtime.Composable
import nl.cita.magister.platform.rememberPlatformHaptics

@Composable
fun rememberMagsHaptics(
    appEnabled: Boolean,
    strength: HapticStrength = HapticStrength.Strong,
): MagsHaptics = rememberPlatformHaptics(appEnabled, strength)
