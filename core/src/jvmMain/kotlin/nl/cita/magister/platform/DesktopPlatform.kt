package nl.cita.magister.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import nl.cita.magister.haptics.HapticStrength
import nl.cita.magister.haptics.MagsHaptics

actual fun appDataDir(): File {
    val override = System.getenv("MAGS_DATA_DIR")
    if (!override.isNullOrBlank()) return File(override).apply { mkdirs() }
    val appdata = System.getenv("APPDATA")
    val home = System.getProperty("user.home")
    val dir = when {
        !appdata.isNullOrBlank() -> File(appdata, "Mags")
        else -> File(home, ".mags")
    }
    dir.mkdirs()
    return dir
}

actual fun platformEncryptedStore(name: String): SettingsStore {
    val dir = appDataDir()
    val key = FileSettingsStore.loadOrCreateKey(File(dir, "keystore.key"))
    return FileSettingsStore(File(dir, "$name.enc.json"), key)
}

actual fun platformPlainStore(name: String): SettingsStore =
    FileSettingsStore(File(appDataDir(), "$name.json"))

actual fun openSystemBrowser(url: String): Boolean {
    return runCatching {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
            true
        } else {
            false
        }
    }.getOrDefault(false)
}

actual fun prefersEmbeddedLogin(): Boolean = true

actual fun platformLiveLessonSync() = Unit

actual fun platformLiveLessonCancel() = Unit

actual fun platformLiveLessonTick() = Unit

actual fun platformShareText(title: String, text: String, mime: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(text), null)
}

@Composable
actual fun rememberPlatformHaptics(appEnabled: Boolean, strength: HapticStrength): MagsHaptics =
    MagsHaptics.NoOp

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop overlays expose their own sluit-knop; Escape is handled per window if needed.
    LaunchedEffect(enabled) { /* no-op: keep signature aligned with Android */ }
}

@Composable
actual fun PlatformPredictiveBack(
    enabled: Boolean,
    onBack: () -> Unit,
    onProgress: (Float) -> Unit,
) {
    PlatformBackHandler(enabled, onBack)
}

@Composable
actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? = null
