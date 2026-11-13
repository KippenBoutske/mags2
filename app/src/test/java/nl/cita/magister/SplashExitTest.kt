package nl.cita.magister

import android.view.View
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SplashExitTest {
    @Test
    fun splashExitToleratesAndroidReturningNoIconView() {
        assertNull(splashIconOrNull { null })
    }

    @Test
    fun splashExitToleratesAndroidThrowingWhenIconViewIsMissing() {
        assertNull(splashIconOrNull { throw NullPointerException("No splash icon view") })
    }

    @Test
    fun splashExitPreservesAnAvailableIconView() {
        val icon = View(RuntimeEnvironment.getApplication())
        assertSame(icon, splashIconOrNull { icon })
    }
}

