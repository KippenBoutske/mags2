package nl.cita.magister.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import nl.cita.magister.haptics.HapticStrength
import nl.cita.magister.haptics.MagsHaptics

/**
 * Platform hooks. Implementations live in androidMain / jvmMain.
 * Shared screens and view models call these instead of Android or desktop APIs.
 */
expect fun openSystemBrowser(url: String): Boolean

/** Desktop cannot complete Custom-Tab + `m6loapp://` without a protocol handler. */
expect fun prefersEmbeddedLogin(): Boolean

expect fun platformLiveLessonSync()

expect fun platformLiveLessonCancel()

expect fun platformLiveLessonTick()

expect fun platformShareText(title: String, text: String, mime: String)

@Composable
expect fun rememberPlatformHaptics(appEnabled: Boolean, strength: HapticStrength): MagsHaptics

@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

@Composable
expect fun PlatformPredictiveBack(
    enabled: Boolean,
    onBack: () -> Unit,
    onProgress: (Float) -> Unit,
)

@Composable
expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?
