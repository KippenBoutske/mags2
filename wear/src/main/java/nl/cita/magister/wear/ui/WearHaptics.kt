package nl.cita.magister.wear.ui

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.math.abs

enum class WearHapticEvent {
    Tick,
    Click,
    Sync,
    EdgeBump,
}

class WearHaptics(
    private val view: View,
    private val vibrator: Vibrator?,
) {
    private val hasVibrator = vibrator?.hasVibrator() == true

    fun play(event: WearHapticEvent) {
        val vib = vibrator
        if (Build.VERSION.SDK_INT >= 30 && hasVibrator && vib != null) {
            val effect = buildPrimitive(vib, event)
            if (effect != null) {
                runCatching { vib.vibrate(effect) }
                return
            }
        }
        val constant = when (event) {
            WearHapticEvent.Tick -> HapticFeedbackConstants.CLOCK_TICK
            WearHapticEvent.Click -> HapticFeedbackConstants.CONTEXT_CLICK
            WearHapticEvent.Sync -> HapticFeedbackConstants.CONFIRM
            WearHapticEvent.EdgeBump -> HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(constant)
    }

    private fun buildPrimitive(vib: Vibrator, event: WearHapticEvent): VibrationEffect? {
        if (Build.VERSION.SDK_INT < 30) return null
        return runCatching {
            val composer = VibrationEffect.startComposition()
            when (event) {
                WearHapticEvent.Tick -> {
                    val id = if (Build.VERSION.SDK_INT >= 31 &&
                        vib.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
                    ) {
                        VibrationEffect.Composition.PRIMITIVE_LOW_TICK
                    } else if (vib.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)) {
                        VibrationEffect.Composition.PRIMITIVE_TICK
                    } else {
                        return null
                    }
                    composer.addPrimitive(id, 0.55f, 0)
                }
                WearHapticEvent.Click -> {
                    if (!vib.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) return null
                    composer.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.85f, 0)
                }
                WearHapticEvent.Sync -> {
                    if (vib.areAllPrimitivesSupported(
                            VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                            VibrationEffect.Composition.PRIMITIVE_CLICK,
                        )
                    ) {
                        composer.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.7f, 0)
                        composer.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.95f, 35)
                    } else if (vib.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                        composer.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.9f, 0)
                    } else {
                        return null
                    }
                }
                WearHapticEvent.EdgeBump -> {
                    val id = if (Build.VERSION.SDK_INT >= 31 &&
                        vib.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD)
                    ) {
                        VibrationEffect.Composition.PRIMITIVE_THUD
                    } else if (vib.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL)) {
                        VibrationEffect.Composition.PRIMITIVE_QUICK_FALL
                    } else {
                        return null
                    }
                    composer.addPrimitive(id, 0.6f, 0)
                }
            }
            composer.compose()
        }.getOrNull()
    }
}

val LocalWearHaptics = staticCompositionLocalOf<WearHaptics?> { null }

@Composable
fun ProvideWearHaptics(content: @Composable () -> Unit) {
    val view = LocalView.current
    val context = LocalContext.current
    val haptics = remember(view, context) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        WearHaptics(view, vibrator)
    }
    CompositionLocalProvider(LocalWearHaptics provides haptics, content = content)
}

fun Modifier.wearScrollRatchet(
    stepDp: Float = 36f,
    minIntervalMs: Long = 30L,
): Modifier = composed {
    val haptics = LocalWearHaptics.current
    val density = LocalDensity.current
    val stepPx = with(density) { stepDp.dp.toPx() }.coerceAtLeast(16f)
    val connection = remember(haptics, stepPx, minIntervalMs) {
        object : NestedScrollConnection {
            private var acc = 0f
            private var lastMs = 0L
            private var edgeArmed = true

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val delta = abs(consumed.y)
                val now = SystemClock.uptimeMillis()
                if (delta > 0.5f) {
                    edgeArmed = true
                    acc += delta
                    if (acc >= stepPx) {
                        acc %= stepPx
                        if (now - lastMs >= minIntervalMs) {
                            lastMs = now
                            haptics?.play(WearHapticEvent.Tick)
                        }
                    }
                } else if (edgeArmed && abs(available.y) > 20f) {
                    edgeArmed = false
                    if (now - lastMs >= 90L) {
                        lastMs = now
                        haptics?.play(WearHapticEvent.EdgeBump)
                    }
                }
                return Offset.Zero
            }
        }
    }
    nestedScroll(connection)
}
