package nl.cita.magister.haptics

import android.os.VibrationEffect
import android.view.HapticFeedbackConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HapticResolverTest {
    private val pixel = HapticCapability(37, HapticCapability.allPrimitives(37))
    private val api34 = HapticCapability.none(34)
    private val api30 = HapticCapability.none(30)
    private val api26 = HapticCapability.none(26)

    @Test
    fun pixelUsesCompositionPrimitives() {
        assertComposition(HapticEvent.TabSwitch, "TICK" to 0.45f)
        assertComposition(HapticEvent.DaySnap, "LOW_TICK" to 0.35f)
        assertComposition(HapticEvent.WeekChip, "TICK" to 0.5f)
        assertComposition(HapticEvent.TodayJump, "CLICK" to 0.65f)
        assertComposition(HapticEvent.HomeworkThreshold, "QUICK_RISE" to 0.4f)
        assertComposition(HapticEvent.HomeworkDone, "CLICK" to 0.6f)
        assertComposition(HapticEvent.HomeworkUndo, "TICK" to 0.45f)
        assertComposition(HapticEvent.RefreshThreshold, "TICK" to 0.5f)
        assertComposition(HapticEvent.RefreshDone, "CLICK" to 0.6f)
        assertComposition(HapticEvent.RefreshError, "QUICK_FALL" to 0.55f)
        assertComposition(HapticEvent.GradeOpen, "CLICK" to 0.55f)
        assertComposition(HapticEvent.GradeFail, "THUD" to 0.4f)
        assertComposition(HapticEvent.CalculatorStep, "LOW_TICK" to 0.28f)
        assertComposition(HapticEvent.LongPress, "SLOW_RISE" to 0.45f)
        assertComposition(HapticEvent.LoginError, "QUICK_FALL" to 0.55f)
    }

    @Test
    fun loginSuccessIsRiseThenClick() {
        val play = HapticResolver.resolve(HapticEvent.LoginSuccess, pixel)
        val steps = (play as HapticPlay.Composition).steps
        assertEquals(2, steps.size)
        assertEquals(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, steps[0].id)
        assertEquals(0.45f, steps[0].scale)
        assertEquals(VibrationEffect.Composition.PRIMITIVE_CLICK, steps[1].id)
        assertEquals(0.7f, steps[1].scale)
        assertEquals(40, steps[1].delayMs)
    }

    @Test
    fun api34FallsBackToViewConstants() {
        assertView(HapticEvent.TabSwitch, api34, HapticFeedbackConstants.SEGMENT_TICK)
        assertView(HapticEvent.DaySnap, api34, HapticFeedbackConstants.CLOCK_TICK)
        assertView(HapticEvent.WeekChip, api34, HapticFeedbackConstants.SEGMENT_TICK)
        assertView(HapticEvent.TodayJump, api34, HapticFeedbackConstants.CONFIRM)
        assertView(HapticEvent.HomeworkThreshold, api34, HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE)
        assertView(HapticEvent.HomeworkDone, api34, HapticFeedbackConstants.TOGGLE_ON)
        assertView(HapticEvent.HomeworkUndo, api34, HapticFeedbackConstants.TOGGLE_OFF)
        assertView(HapticEvent.RefreshThreshold, api34, HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE)
        assertView(HapticEvent.RefreshDone, api34, HapticFeedbackConstants.CONFIRM)
        assertView(HapticEvent.RefreshError, api34, HapticFeedbackConstants.REJECT)
        assertView(HapticEvent.GradeOpen, api34, HapticFeedbackConstants.CONTEXT_CLICK)
        assertView(HapticEvent.GradeFail, api34, HapticFeedbackConstants.REJECT)
        assertView(HapticEvent.CalculatorStep, api34, HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
        assertView(HapticEvent.LongPress, api34, HapticFeedbackConstants.LONG_PRESS)
        assertView(HapticEvent.LoginSuccess, api34, HapticFeedbackConstants.CONFIRM)
        assertView(HapticEvent.LoginError, api34, HapticFeedbackConstants.REJECT)
    }

    @Test
    fun api30SkipsApi34ViewConstants() {
        assertView(HapticEvent.TabSwitch, api30, HapticFeedbackConstants.CLOCK_TICK)
        assertView(HapticEvent.HomeworkDone, api30, HapticFeedbackConstants.CONFIRM)
        assertView(HapticEvent.CalculatorStep, api30, HapticFeedbackConstants.CLOCK_TICK)
        assertView(HapticEvent.HomeworkThreshold, api30, HapticFeedbackConstants.GESTURE_START)
    }

    @Test
    fun api26UsesOldestViewFallback() {
        assertView(HapticEvent.TabSwitch, api26, HapticFeedbackConstants.CLOCK_TICK)
        assertView(HapticEvent.DaySnap, api26, HapticFeedbackConstants.CLOCK_TICK)
        assertView(HapticEvent.WeekChip, api26, HapticFeedbackConstants.CONTEXT_CLICK)
        assertView(HapticEvent.TodayJump, api26, HapticFeedbackConstants.CONTEXT_CLICK)
        assertView(HapticEvent.HomeworkThreshold, api26, HapticFeedbackConstants.CLOCK_TICK)
        assertView(HapticEvent.HomeworkDone, api26, HapticFeedbackConstants.CONTEXT_CLICK)
        assertView(HapticEvent.HomeworkUndo, api26, HapticFeedbackConstants.CONTEXT_CLICK)
        assertView(HapticEvent.RefreshError, api26, HapticFeedbackConstants.CONTEXT_CLICK)
        assertView(HapticEvent.LongPress, api26, HapticFeedbackConstants.LONG_PRESS)
        assertView(HapticEvent.LoginSuccess, api26, HapticFeedbackConstants.CONTEXT_CLICK)
        assertView(HapticEvent.CalculatorStep, api26, HapticFeedbackConstants.CLOCK_TICK)
    }

    @Test
    fun predefinedWhenNoViewConstantMatches() {
        val plan = HapticResolver.EventPlan(
            primitives = emptyList(),
            viewConstants = emptyList(),
            effect = VibrationEffect.EFFECT_CLICK,
        )
        val play = HapticResolver.resolve(plan, HapticCapability.none(29))
        assertEquals(HapticPlay.Predefined(VibrationEffect.EFFECT_CLICK), play)
        assertEquals(HapticPlay.None, HapticResolver.resolve(plan, HapticCapability.none(26)))
    }

    @Test
    fun gatedHonoursToggles() {
        assertEquals(
            HapticPlay.None,
            HapticResolver.gated(HapticEvent.TabSwitch, appEnabled = false, systemEnabled = true, viewEnabled = true, capability = pixel),
        )
        assertEquals(
            HapticPlay.None,
            HapticResolver.gated(HapticEvent.TabSwitch, appEnabled = true, systemEnabled = false, viewEnabled = true, capability = pixel),
        )
        assertEquals(
            HapticPlay.None,
            HapticResolver.gated(HapticEvent.TabSwitch, appEnabled = true, systemEnabled = true, viewEnabled = false, capability = pixel),
        )
        val on = HapticResolver.gated(HapticEvent.TabSwitch, true, true, true, pixel)
        assertTrue(on is HapticPlay.Composition)
    }

    @Test
    fun gradeFailOnlyWhenInsufficient() {
        assertEquals(HapticEvent.GradeOpen, hapticForGrade(true))
        assertEquals(HapticEvent.GradeFail, hapticForGrade(false))
    }

    @Test
    fun describeIsStableForLogs() {
        val play = HapticResolver.resolve(HapticEvent.TabSwitch, pixel)
        assertEquals("composition TICK@0.45", play.describe())
        val view = HapticResolver.resolve(HapticEvent.TabSwitch, api34)
        assertEquals("view SEGMENT_TICK", view.describe())
    }

    private fun assertComposition(event: HapticEvent, vararg steps: Pair<String, Float>) {
        val play = HapticResolver.resolve(event, pixel) as HapticPlay.Composition
        assertEquals(event.name, steps.size, play.steps.size)
        play.steps.forEachIndexed { index, step ->
            assertEquals(event.name, steps[index].first, primitiveName(step.id))
            assertEquals(event.name, steps[index].second, step.scale)
        }
    }

    private fun assertView(event: HapticEvent, capability: HapticCapability, constant: Int) {
        val play = HapticResolver.resolve(event, capability)
        assertEquals(event.name, HapticPlay.ViewConstant(constant), play)
    }
}
