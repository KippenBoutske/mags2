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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.ui.AppClock
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
import java.time.LocalTime

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
    fun heroTimingDuringGeschiedenis() {
        val clock = AppClock.Demo
        val lesson = MockFixtures.appointments().first { it.title == "Geschiedenis" }
        val label = heroTimingLabel(true, lesson.startIso, lesson.endIso, clock.millis())
        assertEquals("tot 12:15 · nog 75 min", label)
        assertFalse(label.contains("nu", ignoreCase = true))
    }

    @Test
    fun heroTimingBeforeLesson() {
        val clock = AppClock(LocalDate.of(2026, 10, 22), LocalTime.of(10, 22))
        val lesson = MockFixtures.appointments().first { it.title == "Geschiedenis" }
        val label = heroTimingLabel(false, lesson.startIso, lesson.endIso, clock.millis())
        assertEquals("over 18 min", label)
    }

    @Test
    fun todayScreenShowsDescriptionDueChipAndAbbrev() {
        val clock = AppClock.Demo
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    TodayScreen(
                        profile = MockFixtures.profile(),
                        appointments = MockFixtures.appointments(),
                        homework = MockFixtures.homework(),
                        averages = GradeCalculator.averagesBySubject(MockFixtures.grades()),
                        grades = MockFixtures.grades(),
                        messages = MockFixtures.messages(),
                        contentPadding = PaddingValues(0.dp),
                        onOpenPage = {},
                        clockDate = clock.date,
                        clockTime = clock.time,
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("NU").assertIsDisplayed()
        compose.onNodeWithText("tot 12:15 · nog 75 min").assertIsDisplayed()
        compose.onAllNodesWithText("nu", substring = false).assertCountEquals(0)
        compose.onNodeWithText("Maak opgave 42", substring = true).assertIsDisplayed()
        compose.onNodeWithText("5,3").assertIsDisplayed()
        compose.onNodeWithText("PW2", substring = true).assertIsDisplayed()
        compose.onNodeWithText("7,6").assertIsDisplayed()
        compose.onNodeWithTag("laatste-cijfers").assertIsDisplayed()
        compose.onAllNodesWithTag("laatste-cijfer", useUnmergedTree = true).assertCountEquals(5)
        compose.onAllNodesWithTag("today-homework-row", useUnmergedTree = true).assertCountEquals(3)
        compose.onAllNodesWithTag("hw-checkbox", useUnmergedTree = true)[0].assertExists()
        val wiskunde = compose.onAllNodesWithText("Wiskunde B", substring = false)
        assertTrue(wiskunde.fetchSemanticsNodes().isNotEmpty())
        val first = MockFixtures.homework().first { !it.isDone }
        compose.onNodeWithText(dueChipLabel(first.dueIso, clock.date), substring = true).assertExists()
    }
}
