package nl.cita.magister.haptics

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import nl.cita.magister.BuildConfig

interface MagsHaptics {
    fun play(event: HapticEvent, ignoreAppToggle: Boolean = false)

    companion object {
        const val TAG = "MagsHaptics"
        val NoOp = object : MagsHaptics {
            override fun play(event: HapticEvent, ignoreAppToggle: Boolean) = Unit
        }
    }
}

val LocalMagsHaptics = staticCompositionLocalOf { MagsHaptics.NoOp }

class AndroidMagsHaptics(
    private val view: View,
    private val vibrator: Vibrator?,
    private val contentResolver: ContentResolver,
    private val capability: HapticCapability,
    private val debug: Boolean,
    private val log: (String) -> Unit = { Log.d(TAG, it) },
) : MagsHaptics {
    @Volatile var appEnabled: Boolean = true

    override fun play(event: HapticEvent, ignoreAppToggle: Boolean) {
        val resolved = HapticResolver.gated(
            event = event,
            appEnabled = appEnabled || ignoreAppToggle,
            systemEnabled = systemHapticsEnabled(),
            viewEnabled = view.isHapticFeedbackEnabled,
            capability = capability,
        )
        if (debug) {
            log("play $event → ${resolved.describe()}")
        }
        when (resolved) {
            is HapticPlay.Composition -> playComposition(resolved)
            is HapticPlay.ViewConstant -> view.performHapticFeedback(resolved.constant)
            is HapticPlay.Predefined -> playPredefined(resolved.effectId)
            HapticPlay.None -> Unit
        }
    }

    private fun systemHapticsEnabled(): Boolean =
        Settings.System.getInt(contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0

    private fun playComposition(play: HapticPlay.Composition) {
        if (Build.VERSION.SDK_INT < 30 || vibrator == null || !vibrator.hasVibrator()) return
        val composer = VibrationEffect.startComposition()
        play.steps.forEach { step ->
            composer.addPrimitive(step.id, step.scale, step.delayMs)
        }
        vibrator.vibrate(composer.compose())
    }

    private fun playPredefined(effectId: Int) {
        if (Build.VERSION.SDK_INT < 29 || vibrator == null || !vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createPredefined(effectId))
    }

    companion object {
        const val TAG = MagsHaptics.TAG
    }
}

@Composable
fun rememberMagsHaptics(appEnabled: Boolean): MagsHaptics {
    val view = LocalView.current
    val context = LocalContext.current
    val player = remember(view, context) {
        val vibrator = vibratorOf(context)
        AndroidMagsHaptics(
            view = view,
            vibrator = vibrator,
            contentResolver = context.contentResolver,
            capability = HapticCapability(
                sdk = Build.VERSION.SDK_INT,
                supportedPrimitives = probePrimitives(vibrator, Build.VERSION.SDK_INT),
            ),
            debug = BuildConfig.DEBUG,
        )
    }
    SideEffect { player.appEnabled = appEnabled }
    return player
}

internal fun vibratorOf(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }

internal fun probePrimitives(vibrator: Vibrator?, sdk: Int): Set<Int> {
    if (vibrator == null || sdk < 31 || !vibrator.hasVibrator()) return emptySet()
    val candidates = HapticCapability.allPrimitives(sdk).toIntArray()
    if (candidates.isEmpty()) return emptySet()
    return try {
        if (vibrator.areAllPrimitivesSupported(*candidates)) {
            candidates.toSet()
        } else {
            candidates.filter { vibrator.areAllPrimitivesSupported(it) }.toSet()
        }
    } catch (_: Throwable) {
        emptySet()
    }
}
