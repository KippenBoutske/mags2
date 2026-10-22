package nl.cita.magister.ui.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.grades.GradeCalculator
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp")
class TodayCopyTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun homeworkPrefersDescriptionOverRepeatedSubject() {
        val item = MockFixtures.homework().first { it.subject == "Wiskunde B" }
        assertEquals("Wiskunde B", item.title)
        assertEquals("Wiskunde B", item.subject)
        val body = homeworkBody(item)
        assertTrue(body.contains("opgave 42"))
        assertFalse(body == item.subject)
    }

    @Test
    fun gradeChipUsesWholeAbbreviation() {
        assertEquals("GS", subjectChipLabel("Geschiedenis", "gs"))
        assertEquals("WI", subjectChipLabel("Wiskunde B", "wi"))
        assertEquals("Geschiedenis", subjectChipLabel("Geschiedenis", "?"))
    }

    @Test
    fun lessonChipShowsTimeAndSubject() {
        val lesson = MockFixtures.appointments().first { it.title == "Wiskunde B" }
        val label = lessonChipLabel(lesson)
        assertTrue(label.contains("Wiskunde B"))
        assertFalse(label == "1")
        assertTrue(label.contains(":"))
    }

    @Test
    fun todayScreenShowsDescriptionDueChipAndAbbrev() {
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    TodayScreen(
                        profile = MockFixtures.profile(),
                        appointments = MockFixtures.appointments(),
                        homework = MockFixtures.homework(),
                        averages = GradeCalculator.averagesBySubject(MockFixtures.grades()),
                        messages = MockFixtures.messages(),
                        contentPadding = PaddingValues(0.dp),
                        onOpenPage = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Maak opgave 42", substring = true).assertIsDisplayed()
        compose.onNodeWithText("GS").assertIsDisplayed()
        compose.onAllNodesWithTag("today-homework-row", useUnmergedTree = true).assertCountEquals(3)
        compose.onAllNodesWithTag("hw-checkbox", useUnmergedTree = true)[0].assertExists()
        val wiskunde = compose.onAllNodesWithText("Wiskunde B", substring = false)
        assertTrue(wiskunde.fetchSemanticsNodes().size <= 2)
        val today = LocalDate.now(java.time.ZoneId.of("Europe/Amsterdam"))
        val first = MockFixtures.homework().first { !it.isDone }
        compose.onNodeWithText(dueChipLabel(first.dueIso, today), substring = true).assertExists()
    }
}
