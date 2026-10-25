package nl.cita.magister.platform

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.File
import kotlinx.coroutines.CancellationException
import nl.cita.magister.auth.SecurePrefs
import nl.cita.magister.haptics.HapticStrength
import nl.cita.magister.haptics.MagsHaptics
import nl.cita.magister.haptics.rememberAndroidMagsHaptics

object AndroidHooks {
    lateinit var appContext: Context
    var liveSync: () -> Unit = {}
    var liveCancel: () -> Unit = {}
    var liveTick: () -> Unit = {}
}

fun initAndroidPlatform(context: Context) {
    // attachBaseContext runs before applicationContext exists (Robolectric and some OEM paths).
    AndroidHooks.appContext = (context as? Application)
        ?: context.applicationContext
        ?: context
}

actual fun appDataDir(): File = AndroidHooks.appContext.filesDir

actual fun platformEncryptedStore(name: String): SettingsStore =
    SharedPreferencesSettingsStore(SecurePrefs.open(AndroidHooks.appContext, name))

actual fun platformPlainStore(name: String): SettingsStore =
    SharedPreferencesSettingsStore(
        AndroidHooks.appContext.getSharedPreferences(name, Context.MODE_PRIVATE),
    )

actual fun openSystemBrowser(url: String): Boolean {
    val context = AndroidHooks.appContext
    return try {
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
        true
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}

actual fun prefersEmbeddedLogin(): Boolean = false

actual fun platformLiveLessonSync() = AndroidHooks.liveSync()

actual fun platformLiveLessonCancel() = AndroidHooks.liveCancel()

actual fun platformLiveLessonTick() = AndroidHooks.liveTick()

actual fun platformShareText(title: String, text: String, mime: String) {
    val context = AndroidHooks.appContext
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText(title, text))
    val share = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(share, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
actual fun rememberPlatformHaptics(appEnabled: Boolean, strength: HapticStrength): MagsHaptics =
    rememberAndroidMagsHaptics(appEnabled, strength)

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled, onBack)
}

@Composable
actual fun PlatformPredictiveBack(
    enabled: Boolean,
    onBack: () -> Unit,
    onProgress: (Float) -> Unit,
) {
    PredictiveBackHandler(enabled = enabled) { progress ->
        try {
            progress.collect { event -> onProgress(event.progress) }
            onBack()
        } catch (_: CancellationException) {
            // gebaar geannuleerd
        } finally {
            onProgress(0f)
        }
    }
}

@Composable
actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
