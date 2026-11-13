package nl.cita.magister.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class CitaThemeTest {
    @Test
    fun brandSurfacesUseNightInkAndSoftPaperAcrossThemes() {
        assertEquals(Color(0xFF14171B), WarmDarkScheme.background)
        assertEquals(Color(0xFFD8835D), WarmDarkScheme.primary)
        assertEquals(Color(0xFFF3EFE7), WarmLightScheme.background)
        assertEquals(Color(0xFF9E432B), WarmLightScheme.primary)
    }
}
