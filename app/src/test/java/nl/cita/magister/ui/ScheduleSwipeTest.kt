package nl.cita.magister.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.ui.schedule.ScheduleScreen
import nl.cita.magister.ui.schedule.buildDaySlots
import nl.cita.magister.ui.theme.CitaTheme
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
class ScheduleSwipeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun swipeMovesToNextDay() {
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    ScheduleScreen(
                        appointments = MockFixtures.appointments(),
                        contentPadding = PaddingValues(0.dp),
                        initialDate = LocalDate.of(2026, 10, 21),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Wiskunde B").assertExists()
        compose.onNodeWithTag("rooster-vandaag").assertIsDisplayed()
        compose.onNodeWithTag("rooster-pager").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText("Biologie").assertExists()
    }

    @Test
    fun tapChipJumpsAndSwipeStripChangesWeek() {
        val wednesday = LocalDate.of(2026, 10, 21)
        val thursday = LocalDate.of(2026, 10, 22)
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    ScheduleScreen(
                        appointments = MockFixtures.appointments(),
                        contentPadding = PaddingValues(0.dp),
                        initialDate = wednesday,
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("week-pick-$thursday").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Biologie").assertExists()

        compose.onNodeWithTag("rooster-weekstrip").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithTag("week-pick-${thursday.plusWeeks(1)}").assertExists()

        compose.onNodeWithTag("rooster-weekstrip").performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.onNodeWithTag("week-pick-$thursday").assertExists()
    }

    @Test
    fun thursdayShowsTussenuurAndLessonSheet() {
        val thursday = LocalDate.of(2026, 10, 22)
        val ofDay = MockFixtures.appointments().filter {
            it.startIso.startsWith("2026-10-22")
        }
        val slots = buildDaySlots(ofDay)
        assertTrue(slots.any { it is nl.cita.magister.ui.schedule.DaySlot.Gap })

        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    ScheduleScreen(
                        appointments = MockFixtures.appointments(),
                        contentPadding = PaddingValues(0.dp),
                        initialDate = thursday,
                        clockDate = thursday,
                        clockTime = java.time.LocalTime.of(11, 0),
                    )
                }
            }
        }
        compose.waitForIdle()
        assertTrue(compose.onAllNodesWithTag("tussenuur").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithTag("rooster-now-line").assertExists()
        compose.onNodeWithText("Geschiedenis").assertExists()
        compose.onNodeWithText("Geschiedenis").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("les-blad").assertExists()
        compose.onAllNodesWithText("Meers").fetchSemanticsNodes().also { nodes ->
            assertTrue(nodes.size >= 2)
        }
    }
}
