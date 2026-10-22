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

        fun of(sdk: Int, supportedPrimitives: Set<Int>): HapticCapability =
            HapticCapability(sdk, supportedPrimitives)

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
    ): HapticPlay {
        if (!appEnabled || !systemEnabled || !viewEnabled) return HapticPlay.None
        return resolve(event, capability)
    }

    fun resolve(event: HapticEvent, capability: HapticCapability): HapticPlay =
        resolve(planOf(event), capability)

    fun resolve(plan: EventPlan, capability: HapticCapability): HapticPlay {
        if (plan.primitives.isNotEmpty() && capability.supports(*plan.primitives.map { it.id }.toIntArray())) {
            return HapticPlay.Composition(plan.primitives)
        }
        plan.viewConstants.firstOrNull { capability.viewAvailable(it) }?.let {
            return HapticPlay.ViewConstant(it)
        }
        if (capability.predefinedAvailable && plan.effect != null) {
            return HapticPlay.Predefined(plan.effect)
        }
        return HapticPlay.None
    }

    fun planOf(event: HapticEvent): EventPlan = when (event) {
        HapticEvent.TabSwitch -> EventPlan(
            primitives = listOf(PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.45f)),
            viewConstants = listOf(HapticFeedbackConstants.SEGMENT_TICK, HapticFeedbackConstants.CLOCK_TICK),
            effect = predefinedTick(),
        )
        HapticEvent.DaySnap -> EventPlan(
            primitives = listOf(PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.35f)),
            viewConstants = listOf(HapticFeedbackConstants.CLOCK_TICK, HapticFeedbackConstants.CONTEXT_CLICK),
            effect = predefinedTick(),
        )
        HapticEvent.WeekChip -> EventPlan(
            primitives = listOf(PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)),
            viewConstants = listOf(HapticFeedbackConstants.SEGMENT_TICK, HapticFeedbackConstants.CONTEXT_CLICK),
            effect = predefinedClick(),
        )
        HapticEvent.TodayJump -> EventPlan(
            primitives = listOf(PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.65f)),
            viewConstants = listOf(HapticFeedbackConstants.CONFIRM, HapticFeedbackConstants.CONTEXT_CLICK),
            effect = predefinedClick(),
        )
        HapticEvent.HomeworkThreshold -> EventPlan(
            primitives = listOf(PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.4f)),
            viewConstants = listOf(
                HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE,
                HapticFeedbackConstants.GESTURE_START,
                HapticFeedbackConstants.CLOCK_TICK,
            ),
            effect = predefinedClick(),
        )
        HapticEvent.HomeworkDone -> EventPlan(
            primitives = listOf(PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f)),
            viewConstants = listOf(
                HapticFeedbackConstants.TOGGLE_ON,
                HapticFeedbackConstants.CONFIRM,
                HapticFeedbackConstants.CONTEXT_CLICK,
            ),
            effect = predefinedClick(),
        )
        HapticEvent.HomeworkUndo -> EventPlan(
            primitives = listOf(PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.45f)),
            viewConstants = listOf(HapticFeedbackConstants.TOGGLE_OFF, HapticFeedbackConstants.CONTEXT_CLICK),
            effect = predefinedTick(),
        )
        HapticEvent.RefreshThreshold -> EventPlan(
            primitives = listOf(PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)),
            viewConstants = listOf(
                HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE,
                HapticFeedbackConstants.GESTURE_START,
                HapticFeedbackConstants.CLOCK_TICK,
            ),
            effect = predefinedTick(),
        )
        HapticEvent.RefreshDone -> EventPlan(
            primitives = listOf(PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f)),
            viewConstants = listOf(HapticFeedbackConstants.CONFIRM, HapticFeedbackConstants.CONTEXT_CLICK),
            effect = predefinedClick(),
        )
        HapticEvent.RefreshError -> EventPlan(
            primitives = listOf(PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.55f)),
            viewConstants = listOf(HapticFeedbackConstants.REJECT, HapticFeedbackConstants.CONTEXT_CLICK),
            effect = predefinedHeavy(),
        )
        HapticEvent.GradeOpen -> EventPlan(
            primitives = listOf(PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.55f)),
            viewConstants = listOf(HapticFeedbackConstants.CONTEXT_CLICK, HapticFeedbackConstants.CONFIRM),
            effect = predefinedClick(),
        )
        HapticEvent.GradeFail -> EventPlan(
            primitives = listOf(PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_THUD, 0.4f)),
            viewConstants = listOf(HapticFeedbackConstants.REJECT, HapticFeedbackConstants.CONTEXT_CLICK),
            effect = predefinedHeavy(),
        )
        HapticEvent.CalculatorStep -> EventPlan(
            primitives = listOf(PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.28f)),
            viewConstants = listOf(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK, HapticFeedbackConstants.CLOCK_TICK),
            effect = predefinedTick(),
        )
        HapticEvent.LongPress -> EventPlan(
            primitives = listOf(PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.45f)),
            viewConstants = listOf(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.CONTEXT_CLICK),
            effect = predefinedHeavy(),
        )
        HapticEvent.LoginSuccess -> EventPlan(
            primitives = listOf(
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.45f),
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f, delayMs = 40),
            ),
            viewConstants = listOf(HapticFeedbackConstants.CONFIRM, HapticFeedbackConstants.CONTEXT_CLICK),
            effect = predefinedClick(),
        )
        HapticEvent.LoginError -> EventPlan(
            primitives = listOf(PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.55f)),
            viewConstants = listOf(HapticFeedbackConstants.REJECT, HapticFeedbackConstants.CONTEXT_CLICK),
            effect = predefinedHeavy(),
        )
    }

    data class EventPlan(
        val primitives: List<PrimitiveStep>,
        val viewConstants: List<Int>,
        val effect: Int?,
    )
}

private fun minSdkForView(constant: Int): Int = when (constant) {
    HapticFeedbackConstants.LONG_PRESS -> 1
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

internal fun primitiveName(id: Int): String = when (id) {
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

internal fun viewName(constant: Int): String = when (constant) {
    HapticFeedbackConstants.CLOCK_TICK -> "CLOCK_TICK"
    HapticFeedbackConstants.CONTEXT_CLICK -> "CONTEXT_CLICK"
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
