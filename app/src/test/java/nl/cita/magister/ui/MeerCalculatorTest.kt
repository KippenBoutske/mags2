package nl.cita.magister.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.ui.grades.GradesScreen
import nl.cita.magister.ui.settings.SettingsScreen
import nl.cita.magister.ui.theme.CitaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp")
class MeerCalculatorTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun profileLineSkipsBlanks() {
        assertEquals("5VWO-A · VWO", profileLine("5VWO-A", "VWO", ""))
        assertEquals("5VWO-A · VWO", profileLine("5VWO-A", "VWO", null, "  "))
        assertEquals("5VWO-A · VWO · Citadel College", profileLine("5VWO-A", "VWO", "Citadel College"))
        assertFalse(profileLine("5VWO-A", "VWO", "").endsWith("·"))
    }

    @Test
    fun meerHubIsFullWidthAndDoesNotDumpLists() {
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    SettingsScreen(
                        profile = MockFixtures.profile(),
                        tenantHost = "citadel.magister.net",
                        schoolName = "Citadel College",
                        webhookUrl = "",
                        hasWebhookKey = false,
                        demoMode = true,
                        schools = emptyList(),
                        searching = false,
                        lastError = null,
                        contentPadding = PaddingValues(0.dp),
                        onQuery = {},
                        onSwitchSchool = { _, _, _ -> },
                        onSaveWebhook = { _, _ -> },
                        onExport = {},
                        onShareJson = {},
                        onLogout = {},
                        absences = MockFixtures.absences(),
                        studyGuides = MockFixtures.studyGuides(),
                        assignments = MockFixtures.assignments(),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("meer-profiel-regel").assertTextEquals("5VWO-A · VWO · Citadel College")
        compose.onNodeWithText("Afwezigheid").assertIsDisplayed()
        compose.onNodeWithText("geoorloofd", substring = true).assertDoesNotExist()
        val profile = compose.onNodeWithTag("meer-profiel").getUnclippedBoundsInRoot()
        val groups = compose.onAllNodesWithTag("meer-groep").fetchSemanticsNodes()
        assertTrue(groups.size >= 2)
        val first = compose.onAllNodesWithTag("meer-groep")[0].getUnclippedBoundsInRoot()
        assertTrue(abs((profile.right - profile.left - (first.right - first.left)).value) < 1f)
        compose.onNodeWithTag("meer-rij-afwezigheid").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("meer-pagina").assertExists()
        compose.onNodeWithText("Afwezigheid").assertExists()
    }

    @Test
    fun calculatorUsesStepperChipsAndCaption() {
        val grades = MockFixtures.grades()
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    GradesScreen(
                        profile = MockFixtures.profile(),
                        averages = GradeCalculator.averagesBySubject(grades),
                        grades = grades,
                        refreshing = false,
                        exporting = false,
                        contentPadding = PaddingValues(0.dp),
                        onExport = {},
                        initialSubjectId = 3,
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Doel 7,0").assertIsDisplayed()
        compose.onNodeWithText("Weging ×3").assertExists()
        compose.onNodeWithTag("cijfers-weging-3").assertExists()
        compose.onNodeWithText("nodig voor je volgende").assertExists()
        compose.onNodeWithTag("cijfers-nodig").assertExists()
    }
}
