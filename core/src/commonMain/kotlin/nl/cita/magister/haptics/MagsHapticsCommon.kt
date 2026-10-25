package nl.cita.magister.haptics

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput

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
