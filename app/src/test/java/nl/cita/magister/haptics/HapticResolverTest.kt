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

    @Test
    fun pixelCompositionsOnlyWhereTheyAddValue() {
        val snap = HapticResolver.resolve(HapticEvent.DaySnap, pixel) as HapticPlay.Composition
        assertEquals("LOW_TICK", primitiveName(snap.steps.single().id))
        assertEquals(0.7f, snap.steps.single().scale, 0.001f)

        val login = HapticResolver.resolve(HapticEvent.LoginSuccess, pixel) as HapticPlay.Composition
        assertEquals(2, login.steps.size)
        assertEquals(0, login.steps[1].delayMs)

        assertTrue(HapticResolver.resolve(HapticEvent.TabSwitch, pixel) is HapticPlay.ViewConstant)
        assertTrue(HapticResolver.resolve(HapticEvent.GradeOpen, pixel) is HapticPlay.ViewConstant)
    }

    @Test
    fun strongTapsUseConfirmContextClick() {
        assertEquals(
            HapticPlay.ViewConstant(HapticFeedbackConstants.CONTEXT_CLICK),
            HapticResolver.resolve(HapticEvent.TabSwitch, api34, HapticStrength.Strong),
        )
        assertEquals(
            HapticPlay.ViewConstant(HapticFeedbackConstants.CONFIRM),
            HapticResolver.resolve(HapticEvent.TodayJump, api34, HapticStrength.Strong),
        )
        assertEquals(
            HapticPlay.ViewConstant(HapticFeedbackConstants.REJECT),
            HapticResolver.resolve(HapticEvent.LoginError, api34, HapticStrength.Strong),
        )
    }

    @Test
    fun gatedHonoursToggles() {
        assertEquals(
            HapticPlay.None,
            HapticResolver.gated(HapticEvent.TabSwitch, false, true, true, pixel),
        )
    }

    @Test
    fun loginSuccessHasNoDelay() {
        val plan = HapticResolver.planOf(HapticEvent.LoginSuccess)
        assertTrue(plan.preferComposition)
        assertEquals(0, plan.primitives.last().delayMs)
        assertEquals(VibrationEffect.Composition.PRIMITIVE_CLICK, plan.primitives.last().id)
    }
}
