package nl.cita.magister.haptics

import android.os.Build
import android.os.VibrationEffect
import android.view.HapticFeedbackConstants

data class PrimitiveStep(
    val id: Int,
    val scale: Float,
    val delayMs: Int = 0,
)

sealed class HapticPlay {
    data class Composition(val steps: List<PrimitiveStep>) : HapticPlay()
    data class ViewConstant(val constant: Int) : HapticPlay()
    data class Predefined(val effectId: Int) : HapticPlay()
    data object None : HapticPlay()
}

data class HapticCapability(
    val sdk: Int,
    val supportedPrimitives: Set<Int>,
) {
    fun supports(vararg ids: Int): Boolean =
        sdk >= 30 && ids.isNotEmpty() && ids.all { it in supportedPrimitives }

    fun viewAvailable(constant: Int): Boolean = sdk >= minSdkForView(constant)

    val predefinedAvailable: Boolean get() = sdk >= 29

    companion object {
        fun allPrimitives(sdk: Int = Build.VERSION.SDK_INT): Set<Int> {
            val ids = mutableSetOf(
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_THUD,
                VibrationEffect.Composition.PRIMITIVE_SPIN,
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                VibrationEffect.Composition.PRIMITIVE_SLOW_RISE,
                VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
                VibrationEffect.Composition.PRIMITIVE_TICK,
            )
            if (sdk >= 31) ids += VibrationEffect.Composition.PRIMITIVE_LOW_TICK
            return ids
        }

        fun none(sdk: Int): HapticCapability = HapticCapability(sdk, emptySet())
    }
}

object HapticResolver {
    fun gated(
        event: HapticEvent,
        appEnabled: Boolean,
        systemEnabled: Boolean,
        viewEnabled: Boolean,
        capability: HapticCapability,
        strength: HapticStrength = HapticStrength.Strong,
    ): HapticPlay {
        if (!appEnabled || !systemEnabled || !viewEnabled) return HapticPlay.None
        return resolve(event, capability, strength)
    }

    fun resolve(
        event: HapticEvent,
        capability: HapticCapability,
        strength: HapticStrength = HapticStrength.Strong,
    ): HapticPlay = resolve(planOf(event), capability, strength)

    fun resolve(
        plan: EventPlan,
        capability: HapticCapability,
        strength: HapticStrength = HapticStrength.Strong,
    ): HapticPlay {
        if (plan.preferComposition &&
            plan.primitives.isNotEmpty() &&
            capability.supports(*plan.primitives.map { it.id }.toIntArray())
        ) {
            return HapticPlay.Composition(plan.primitives.map { it.scaled(strength) })
        }
        viewChoices(plan, strength).firstOrNull { capability.viewAvailable(it) }?.let {
            return HapticPlay.ViewConstant(it)
        }
        if (capability.predefinedAvailable && plan.effect != null) {
            return HapticPlay.Predefined(plan.effect)
        }
        return HapticPlay.None
    }

    fun planOf(event: HapticEvent): EventPlan = when (event) {
        HapticEvent.TabSwitch -> EventPlan(
            primitives = listOf(tick(0.9f)),
            viewLight = HapticFeedbackConstants.KEYBOARD_TAP,
            viewNormal = HapticFeedbackConstants.CONTEXT_CLICK,
            viewStrong = HapticFeedbackConstants.CONTEXT_CLICK,
            effect = predefinedClick(),
        )
        HapticEvent.DaySnap -> EventPlan(
            primitives = listOf(lowTick(0.7f)),
            viewLight = HapticFeedbackConstants.KEYBOARD_TAP,
            viewNormal = HapticFeedbackConstants.CONTEXT_CLICK,
            viewStrong = HapticFeedbackConstants.CONTEXT_CLICK,
            effect = predefinedClick(),
            preferComposition = true,
        )
        HapticEvent.WeekChip -> EventPlan(
            primitives = listOf(tick(0.9f)),
            viewLight = HapticFeedbackConstants.KEYBOARD_TAP,
            viewNormal = HapticFeedbackConstants.CONTEXT_CLICK,
            viewStrong = HapticFeedbackConstants.CONTEXT_CLICK,
            effect = predefinedClick(),
        )
        HapticEvent.TodayJump -> EventPlan(
            primitives = listOf(click(1f)),
            viewLight = HapticFeedbackConstants.CONTEXT_CLICK,
            viewNormal = HapticFeedbackConstants.CONFIRM,
            viewStrong = HapticFeedbackConstants.CONFIRM,
            effect = predefinedClick(),
        )
        HapticEvent.HomeworkThreshold -> EventPlan(
            primitives = listOf(quickRise(0.95f)),
            viewLight = HapticFeedbackConstants.KEYBOARD_TAP,
            viewNormal = HapticFeedbackConstants.GESTURE_START,
            viewStrong = HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE,
            effect = predefinedClick(),
            preferComposition = true,
        )
        HapticEvent.HomeworkDone -> EventPlan(
            primitives = listOf(click(1f)),
            viewLight = HapticFeedbackConstants.CONTEXT_CLICK,
            viewNormal = HapticFeedbackConstants.CONFIRM,
            viewStrong = HapticFeedbackConstants.TOGGLE_ON,
            effect = predefinedClick(),
        )
        HapticEvent.HomeworkUndo -> EventPlan(
            primitives = listOf(tick(0.85f)),
            viewLight = HapticFeedbackConstants.KEYBOARD_TAP,
            viewNormal = HapticFeedbackConstants.CONTEXT_CLICK,
            viewStrong = HapticFeedbackConstants.TOGGLE_OFF,
            effect = predefinedTick(),
        )
        HapticEvent.RefreshThreshold -> EventPlan(
            primitives = listOf(tick(0.9f)),
            viewLight = HapticFeedbackConstants.KEYBOARD_TAP,
            viewNormal = HapticFeedbackConstants.GESTURE_START,
            viewStrong = HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE,
            effect = predefinedClick(),
        )
        HapticEvent.RefreshDone -> EventPlan(
            primitives = listOf(click(1f)),
            viewLight = HapticFeedbackConstants.CONTEXT_CLICK,
            viewNormal = HapticFeedbackConstants.CONFIRM,
            viewStrong = HapticFeedbackConstants.CONFIRM,
            effect = predefinedClick(),
        )
        HapticEvent.RefreshError -> EventPlan(
            primitives = listOf(quickFall(1f)),
            viewLight = HapticFeedbackConstants.CONTEXT_CLICK,
            viewNormal = HapticFeedbackConstants.REJECT,
            viewStrong = HapticFeedbackConstants.REJECT,
            effect = predefinedHeavy(),
        )
        HapticEvent.GradeOpen -> EventPlan(
            primitives = listOf(click(1f)),
            viewLight = HapticFeedbackConstants.CONTEXT_CLICK,
            viewNormal = HapticFeedbackConstants.CONTEXT_CLICK,
            viewStrong = HapticFeedbackConstants.CONFIRM,
            effect = predefinedClick(),
        )
        HapticEvent.GradeFail -> EventPlan(
            primitives = listOf(thud(0.9f)),
            viewLight = HapticFeedbackConstants.CONTEXT_CLICK,
            viewNormal = HapticFeedbackConstants.REJECT,
            viewStrong = HapticFeedbackConstants.REJECT,
            effect = predefinedHeavy(),
            preferComposition = true,
        )
        HapticEvent.CalculatorStep -> EventPlan(
            primitives = listOf(lowTick(0.7f)),
            viewLight = HapticFeedbackConstants.KEYBOARD_TAP,
            viewNormal = HapticFeedbackConstants.KEYBOARD_TAP,
            viewStrong = HapticFeedbackConstants.CONTEXT_CLICK,
            effect = predefinedTick(),
        )
        HapticEvent.LongPress -> EventPlan(
            primitives = listOf(slowRise(0.9f)),
            viewLight = HapticFeedbackConstants.LONG_PRESS,
            viewNormal = HapticFeedbackConstants.LONG_PRESS,
            viewStrong = HapticFeedbackConstants.LONG_PRESS,
            effect = predefinedHeavy(),
        )
        HapticEvent.LoginSuccess -> EventPlan(
            primitives = listOf(quickRise(0.95f), click(1f)),
            viewLight = HapticFeedbackConstants.CONTEXT_CLICK,
            viewNormal = HapticFeedbackConstants.CONFIRM,
            viewStrong = HapticFeedbackConstants.CONFIRM,
            effect = predefinedClick(),
            preferComposition = true,
        )
        HapticEvent.LoginError -> EventPlan(
            primitives = listOf(quickFall(1f)),
            viewLight = HapticFeedbackConstants.CONTEXT_CLICK,
            viewNormal = HapticFeedbackConstants.REJECT,
            viewStrong = HapticFeedbackConstants.REJECT,
            effect = predefinedHeavy(),
        )
    }

    fun viewChoices(plan: EventPlan, strength: HapticStrength): List<Int> {
        val primary = when (strength) {
            HapticStrength.Light -> plan.viewLight
            HapticStrength.Normal -> plan.viewNormal
            HapticStrength.Strong -> plan.viewStrong
        }
        return listOf(primary, plan.viewNormal, plan.viewLight, plan.viewStrong).distinct()
    }

    data class EventPlan(
        val primitives: List<PrimitiveStep>,
        val viewLight: Int,
        val viewNormal: Int,
        val viewStrong: Int,
        val effect: Int?,
        val preferComposition: Boolean = false,
    )
}

private fun PrimitiveStep.scaled(strength: HapticStrength): PrimitiveStep =
    copy(scale = (scale * strength.scale).coerceIn(0.15f, 1f), delayMs = 0)

private fun tick(scale: Float) = PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, scale)
private fun lowTick(scale: Float) = PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, scale)
private fun click(scale: Float) = PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, scale)
private fun thud(scale: Float) = PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_THUD, scale)
private fun quickRise(scale: Float) = PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, scale)
private fun quickFall(scale: Float) = PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, scale)
private fun slowRise(scale: Float) = PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, scale)

fun minSdkForView(constant: Int): Int = when (constant) {
    HapticFeedbackConstants.LONG_PRESS,
    HapticFeedbackConstants.KEYBOARD_TAP,
    -> 1
    HapticFeedbackConstants.CLOCK_TICK -> 21
    HapticFeedbackConstants.CONTEXT_CLICK -> 23
    HapticFeedbackConstants.CONFIRM,
    HapticFeedbackConstants.REJECT,
    HapticFeedbackConstants.GESTURE_START,
    HapticFeedbackConstants.GESTURE_END,
    -> 30
    HapticFeedbackConstants.TOGGLE_ON,
    HapticFeedbackConstants.TOGGLE_OFF,
    HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE,
    HapticFeedbackConstants.SEGMENT_TICK,
    HapticFeedbackConstants.SEGMENT_FREQUENT_TICK,
    -> 34
    else -> 1
}

fun HapticPlay.describe(): String = when (this) {
    is HapticPlay.Composition ->
        "composition " + steps.joinToString { primitiveName(it.id) + "@" + it.scale }
    is HapticPlay.ViewConstant -> "view " + viewName(constant)
    is HapticPlay.Predefined -> "predefined $effectId"
    HapticPlay.None -> "none"
}

fun primitiveName(id: Int): String = when (id) {
    VibrationEffect.Composition.PRIMITIVE_CLICK -> "CLICK"
    VibrationEffect.Composition.PRIMITIVE_TICK -> "TICK"
    VibrationEffect.Composition.PRIMITIVE_LOW_TICK -> "LOW_TICK"
    VibrationEffect.Composition.PRIMITIVE_THUD -> "THUD"
    VibrationEffect.Composition.PRIMITIVE_SPIN -> "SPIN"
    VibrationEffect.Composition.PRIMITIVE_QUICK_RISE -> "QUICK_RISE"
    VibrationEffect.Composition.PRIMITIVE_SLOW_RISE -> "SLOW_RISE"
    VibrationEffect.Composition.PRIMITIVE_QUICK_FALL -> "QUICK_FALL"
    else -> id.toString()
}

fun viewName(constant: Int): String = when (constant) {
    HapticFeedbackConstants.CLOCK_TICK -> "CLOCK_TICK"
    HapticFeedbackConstants.CONTEXT_CLICK -> "CONTEXT_CLICK"
    HapticFeedbackConstants.KEYBOARD_TAP -> "KEYBOARD_TAP"
    HapticFeedbackConstants.LONG_PRESS -> "LONG_PRESS"
    HapticFeedbackConstants.CONFIRM -> "CONFIRM"
    HapticFeedbackConstants.REJECT -> "REJECT"
    HapticFeedbackConstants.GESTURE_START -> "GESTURE_START"
    HapticFeedbackConstants.GESTURE_END -> "GESTURE_END"
    HapticFeedbackConstants.TOGGLE_ON -> "TOGGLE_ON"
    HapticFeedbackConstants.TOGGLE_OFF -> "TOGGLE_OFF"
    HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE -> "GESTURE_THRESHOLD"
    HapticFeedbackConstants.SEGMENT_TICK -> "SEGMENT_TICK"
    HapticFeedbackConstants.SEGMENT_FREQUENT_TICK -> "SEGMENT_FREQUENT_TICK"
    else -> constant.toString()
}

private fun predefinedClick(): Int = VibrationEffect.EFFECT_CLICK
private fun predefinedTick(): Int = VibrationEffect.EFFECT_TICK
private fun predefinedHeavy(): Int = VibrationEffect.EFFECT_HEAVY_CLICK
