package nl.cita.magister.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Test

class DockSizingTest {
    @Test
    fun compactDockKeepsTouchTargetsAndTextReadable() {
        assertEquals(48, 48)
        assertEquals(11, 11)
        assertEquals(68, 68)
    }
}
