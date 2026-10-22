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
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import nl.cita.magister.BuildConfig

interface MagsHaptics {
    fun play(event: HapticEvent, ignoreAppToggle: Boolean = false)
    fun applyStrength(strength: HapticStrength) = Unit

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
) : MagsHaptics {
    @Volatile var appEnabled: Boolean = true
    @Volatile var strength: HapticStrength = HapticStrength.Strong
        private set
    @Volatile private var systemOn: Boolean = true

    private val hasVibrator = vibrator?.hasVibrator() == true
    private val compositions: Array<VibrationEffect?> =
        arrayOfNulls(HapticEvent.entries.size * HapticStrength.entries.size)
    private val viewConstants: IntArray =
        IntArray(HapticEvent.entries.size * HapticStrength.entries.size)
    private val predefined: Array<VibrationEffect?> = arrayOfNulls(HapticEvent.entries.size)

    init {
        refreshSystem()
        HapticEvent.entries.forEach { event ->
            val plan = HapticResolver.planOf(event)
            HapticStrength.entries.forEach { level ->
                val slot = slot(event, level)
                val resolved = HapticResolver.resolve(plan, capability, level)
                when (resolved) {
                    is HapticPlay.Composition -> compositions[slot] = buildComposition(resolved)
                    is HapticPlay.ViewConstant -> viewConstants[slot] = resolved.constant
                    is HapticPlay.Predefined -> Unit
                    HapticPlay.None -> Unit
                }
                if (viewConstants[slot] == 0) {
                    HapticResolver.viewChoices(plan, level)
                        .firstOrNull { capability.viewAvailable(it) }
                        ?.let { viewConstants[slot] = it }
                }
            }
            if (capability.predefinedAvailable && plan.effect != null && Build.VERSION.SDK_INT >= 29) {
                predefined[event.ordinal] = VibrationEffect.createPredefined(plan.effect)
            }
        }
    }

    override fun applyStrength(strength: HapticStrength) {
        this.strength = strength
    }

    fun refreshSystem() {
        systemOn = Settings.System.getInt(contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
    }

    override fun play(event: HapticEvent, ignoreAppToggle: Boolean) {
        if ((!appEnabled && !ignoreAppToggle) || !systemOn || !view.isHapticFeedbackEnabled) return
        val level = strength
        val slot = slot(event, level)
        val composed = compositions[slot]
        if (composed != null && hasVibrator) {
            vibrator?.vibrate(composed)
            if (debug) Log.d(TAG, "play $event → composition")
            return
        }
        val constant = viewConstants[slot]
        if (constant != 0) {
            view.performHapticFeedback(constant)
            if (debug) Log.d(TAG, "play $event → view $constant")
            return
        }
        val pre = predefined[event.ordinal]
        if (pre != null && hasVibrator) {
            vibrator?.vibrate(pre)
            if (debug) Log.d(TAG, "play $event → predefined")
        }
    }

    private fun buildComposition(play: HapticPlay.Composition): VibrationEffect? {
        if (Build.VERSION.SDK_INT < 30 || !hasVibrator) return null
        val composer = VibrationEffect.startComposition()
        play.steps.forEach { step ->
            composer.addPrimitive(step.id, step.scale, 0)
        }
        return composer.compose()
    }

    companion object {
        const val TAG = MagsHaptics.TAG

        private fun slot(event: HapticEvent, strength: HapticStrength): Int =
            event.ordinal * HapticStrength.entries.size + strength.ordinal
    }
}

@Composable
fun rememberMagsHaptics(appEnabled: Boolean, strength: HapticStrength = HapticStrength.Strong): MagsHaptics {
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
    SideEffect {
        player.appEnabled = appEnabled
        player.applyStrength(strength)
        player.refreshSystem()
    }
    return player
}

fun Modifier.hapticOnDown(event: HapticEvent, enabled: Boolean = true): Modifier {
    if (!enabled) return this
    return composed {
        val haptics = LocalMagsHaptics.current
        pointerInput(event) {
            awaitPointerEventScope {
                while (true) {
                    val pointer = awaitPointerEvent(PointerEventPass.Initial)
                    if (pointer.changes.any { it.changedToDown() }) {
                        haptics.play(event)
                    }
                }
            }
        }
    }
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
